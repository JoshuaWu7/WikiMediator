package cpen221.mp3.wikimediator;

import cpen221.mp3.fsftbuffer.FSFTBuffer;
import org.fastily.jwiki.core.Wiki;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

public class WikiMediator {

    private static final long MILLIS_PER_SEC = 1000;
    private static final int DEFAULT_LOAD_WINDOW = 30;
    private static final int LOCK_INDEX = 0;
    private static final String FILEPATH = "local/request_data";

    private final Wiki wiki;
    private final FSFTBuffer<WikiPage> wikiBuffer;
    private final List<Request> requests;

    /* Thread Safety Argument */
    // WikiMediator stores requests in a local list named requests, and caches pages in
    // a local FSFTBuffer, which should already allow concurrent method calls.
    // The first element in requests acts as a lock for the whole WikiMediator.
    // Every method first acquires this lock, then adds a request to requests,
    // writes this request to a file, then determines the portion of requests
    // that it needs to iterate over.
    // Since the method determines how much of the shared list will be iterated over when
    // it has the lock, and since the list can only be modified by adding to it,
    // the method will always be iterating over an unchanging section of that list.
    // This ensures that concurrent operations happen without affecting each other.

    /* Representation Invariants */
    // wiki is non-null
    // wikiBuffer is non-null
    // requests is a non-null list

    /* Abstraction Functions */
    // a WikiMediator is a FSFT buffer that holds wiki pages and a list of request objects

    /**
     * Constructor for the WikiMediator
     *
     * @param capacity          a positive integer
     * @param stalenessInterval a positive integer
     */
    public WikiMediator(int capacity, int stalenessInterval) {
        this.wiki = new Wiki.Builder().withDomain("en.wikipedia.org").build();
        this.wikiBuffer = new FSFTBuffer<>(capacity, stalenessInterval);

        // Requests in the list will be immutable, therefore no need to put locks on them,
        // except for the first.
        this.requests = new ArrayList<>();

        // Always have something to use as a lock at index 0.
        // Before anything is added to the requests list, this lock must be acquired first.
        requests.add(new Request(RequestType.LOCK));

        // Note: only one newline at end of file. Any other blank lines will break this.
        try {
            BufferedReader reader = new BufferedReader(new FileReader(FILEPATH));
            for (String fileLine = reader.readLine(); fileLine != null;
                 fileLine = reader.readLine()) {
                Request lineRequest = lineParser(fileLine.split(" "));
                requests.add(lineRequest);
            }
            reader.close();
        } catch (IOException ioe) {
            System.out.println("Could not find request_data");
        }
    }

    /**
     * Given a query, return up to {@code limit} page titles that match the query string (per Wikipedia's search service).
     * Adds string count to queryFrequency hashmap
     *
     * @param query a non-null string that contains the query that we want to search for
     * @param limit a positive integer representing the maximum amount of page titles that match the query string
     * @return a list of page titles that contains the searched string query
     */
    public List<String> search(String query, int limit) {
        synchronized (requests.get(LOCK_INDEX)) {
            Request request = new Request(RequestType.SEARCH, query);
            requests.add(request);
            writeToFile(request);
        }
        return wiki.search(query, limit);
    }

    /**
     * Given a {@code pageTitle}, return the text associated with the Wikipedia page that matches {@code pageTitle}.
     * Adds string count to queryFrequency hashmap.
     *
     * @param pageTitle a non-null String
     * @return The text of the page, or an empty string if the page is non-existent/something went wrong.
     */
    public String getPage(String pageTitle) {
        synchronized (requests.get(LOCK_INDEX)) {
            Request request = new Request(RequestType.GET_PAGE, pageTitle);
            requests.add(request);
            writeToFile(request);
        }

        String pageText;
        // Concurrent puts and gets should already be threadsafe.
        try {
            // Using pageTitle as ID.
            pageText = (wikiBuffer.get(pageTitle)).getPageText();
        } catch (NoSuchElementException noPage) {
            pageText = wiki.getPageText(pageTitle);
            wikiBuffer.put(new WikiPage(pageTitle, pageText));
        }
        return pageText;
    }

    /**
     * Return the most common {@code String}s used in {@code search} and {@code getPage} requests,
     * with items being sorted in non-increasing (decreasing) count order.
     * When many requests have been made, return only `limit` items.
     *
     * @param limit the maximum number of common {@code String}s to return.
     * @return a list of Strings, 0 < list.size() <= {@code limit}, representing the most common {@code String}s
     * used in {@code search} and {@code getPage} requests.
     */
    public List<String> zeitgeist(int limit) {
        int numRequests;
        synchronized (requests.get(LOCK_INDEX)) {
            Request request = new Request(RequestType.ZEITGEIST);
            requests.add(request);
            writeToFile(request);
            // Keep track of the current list size, so that if anything is added to the end while zeitgeist is
            // iterating it doesn't screw up.
            // Don't include the request itself, since it's not of type SEARCH or GET_PAGE
            numRequests = requests.size() - 1;
        }

        // Make a hashmap in zeitgeist, so no worries about concurrent threads taking from the same hashmap.
        HashMap<String, Integer> queryFrequencies = new HashMap<>();
        // index 0 is the lock.
        for (int reqIndex = 1; reqIndex < numRequests; reqIndex++) {

            // Only concerned with search or getPage:
            Request currentReq = requests.get(reqIndex);
            if (currentReq.getType() == RequestType.SEARCH ||
                currentReq.getType() == RequestType.GET_PAGE) {

                // If String is not in the hashMap already, add it. Else, increase its frequency.
                String query = currentReq.getQuery();
                if (!queryFrequencies.containsKey(query)) {
                    queryFrequencies.put(query, 1);
                } else {
                    queryFrequencies.put(query, queryFrequencies.get(query) + 1);
                }
            }
        }
        return getCommonStrings(queryFrequencies, limit);
    }

    /**
     * returns the most frequent requests made in the last timeLimitInSeconds seconds.
     * This method should report at most maxItems of the most frequent requests.
     *
     * @param timeLimitInSeconds the time limit (in seconds) of interest
     * @param maxItems           a positive integer
     * @return list of strings representing the list of most frequent requests within a time limit window
     */
    public List<String> trending(int timeLimitInSeconds, int maxItems) {
        int startIndex;
        long currentTime;
        synchronized (requests.get(LOCK_INDEX)) {
            // Put the following in the lock, so currentTime is always larger than anything else currently in requests.
            currentTime = System.currentTimeMillis();
            Request request = new Request(RequestType.TRENDING);
            requests.add(request);
            writeToFile(request);

            // Doesn't care about itself, since its own request type is of type TRENDING.
            startIndex = requests.size() - 2;
        }

        // Make a hashmap for trending, start iterating over the requests from the startIndex
        HashMap<String, Integer> queryFrequencies = new HashMap<>();
        long window = timeLimitInSeconds * MILLIS_PER_SEC;

        for (int reqIndex = startIndex; reqIndex > 0; reqIndex--) {

            // Only concerned with search or getPage:
            Request currentReq = requests.get(reqIndex);
            if (currentReq.getType() == RequestType.SEARCH ||
                currentReq.getType() == RequestType.GET_PAGE) {

                // Additional condition that requests must be within the time window:
                if (currentReq.getRequestTime() >= currentTime - window) {

                    // If String is not in the hashMap already, add it. Else, increase its frequency.
                    String query = currentReq.getQuery();
                    if (!queryFrequencies.containsKey(query)) {
                        queryFrequencies.put(query, 1);
                    } else {
                        queryFrequencies.put(query, queryFrequencies.get(query) + 1);
                    }
                }
            }
        }
        return getCommonStrings(queryFrequencies, maxItems);
    }

    /**
     * Gives the maximum number of requests seen in any time window of a given length
     * The request count is to include all requests made using the public API of `WikiMediator`, and
     * therefore counts all **five** methods listed as **basic page requests**.
     * (There is one more request that appears later, `shortestPath`, and that should also be included
     * if you do implement that method.)
     *
     * @param timeWindowInSeconds the time window in which to count requests
     * @return an integer representing the maximum number of requests seen in any time window of length
     * {@code timeWindowInSeconds}.
     */
    public int windowedPeakLoad(int timeWindowInSeconds) {
        int endIndex;
        synchronized (requests.get(LOCK_INDEX)) {
            Request request = new Request(RequestType.PEAK_LOAD);
            requests.add(request);
            writeToFile(request);
            endIndex = requests.size() - 1;
        }
        long window = timeWindowInSeconds * MILLIS_PER_SEC;
        int startIndex = 1;
        int peakLoad = 0;

        while (startIndex <= endIndex) {
            int count = 0;
            for (int reqIndex = startIndex;
                 reqIndex <= endIndex &&
                     window >= requests.get(reqIndex).getRequestTime() -
                         requests.get(startIndex).getRequestTime();
                 reqIndex++) {
                count++;
            }
            if (count > peakLoad) {
                peakLoad = count;
            }
            startIndex++;
        }
        return peakLoad;
    }

    /**
     * Returns the peak load of a default window of wikimediator requests
     *
     * @return integer peak load that is positive or zero
     */
    public int windowedPeakLoad() {
        return windowedPeakLoad(DEFAULT_LOAD_WINDOW);
    }

    private void writeToFile(Request request) {
        try {
            FileWriter requestWriter = new FileWriter(FILEPATH, true);
            StringBuilder lineBuilder = new StringBuilder();

            lineBuilder.append(request.getType().toString());
            lineBuilder.append(" ");
            lineBuilder.append(request.getQuery());
            if (!request.getQuery().isBlank()) {
                lineBuilder.append(" ");
            }
            lineBuilder.append(request.getRequestTime());

            requestWriter.write(lineBuilder.toString());
            requestWriter.write("\n");
            requestWriter.close();
        } catch (IOException ioe) {
            System.out.println("Couldn't find request_data");
        }
    }

    /**
     * Helper function that parses the lines file line string array into a Request object.
     *
     * @param fileLine a non-null String array
     * @return Request
     */
    private Request lineParser(String[] fileLine) {
        RequestType type;
        String query;
        long requestTime;

        // The element at position 0 in the list should be the request type
        type = RequestType.valueOf(fileLine[0]);
        requestTime = Long.parseLong(fileLine[fileLine.length - 1]);

        if (fileLine.length == 2) {
            return new Request(type, requestTime);
        }

        query = fileLine[1];

        if (fileLine.length > 3) {
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append(query);
            for (int index = 2; index < fileLine.length - 1; index++) {
                queryBuilder.append(" ");
                queryBuilder.append(fileLine[index]);
            }
            query = queryBuilder.toString();
        }
        return new Request(type, query, requestTime);
    }

    /**
     * A private helper method to extract the most frequent strings from a hashMap generated by trending or
     * zeitgeist
     *
     * @param queryFrequencies a hashmap generated by zeitgeist or trending
     * @param limit            the maximum number of Strings to search for in {@code} queryFrequencies
     * @return A list of Strings, where each String represents a query String associated with a call to
     * search or getPage. The list is sorted in non-increasing order, by the frequency each String in the list
     * appears in {@code} queryFrequencies.
     */
    private List<String> getCommonStrings(HashMap<String, Integer> queryFrequencies, int limit) {
        List<String> commonStrings = new ArrayList<>();
        int reqSize = queryFrequencies.size();

        if (queryFrequencies.size() == 0) {
            return commonStrings;
        }

        for (int count = 0; count < limit && commonStrings.size() < reqSize; count++) {
            int highestFrequency = 0;
            String highestRequest = "";

            for (String query : queryFrequencies.keySet()) {
                // Found query with greater frequency, and that query is not already in the list:
                if (queryFrequencies.get(query) > highestFrequency &&
                    !commonStrings.contains(query)) {
                    highestFrequency = queryFrequencies.get(query);
                    highestRequest = query;
                }
            }
            commonStrings.add(highestRequest);
        }
        return commonStrings;
    }

    /**
     * Returns the forward path of page titles from pageTitle1 to pageTitle2. If there is no connection, or the connection times out, throw TimeoutException.
     *
     * @param pageTitle1 a non-null string
     * @param pageTitle2 a non-null string
     * @param timeout    a positive integer
     * @return List<String> containing the forward path of page titles.
     * @throws TimeoutException
     */
    public List<String> shortestPath(String pageTitle1, String pageTitle2, int timeout) throws
        TimeoutException {
        synchronized (requests.get(LOCK_INDEX)) {
            requests.add(new Request(RequestType.PATH));
            writeToFile(new Request(RequestType.PATH));
        }

        long currentTime = System.currentTimeMillis();

        List<String> forwardPageTitles1 = wiki.getLinksOnPage(pageTitle1);

        for (String page : forwardPageTitles1) {
            if (page.equals(pageTitle2)) {
                return Arrays.asList(pageTitle1, pageTitle2);
            }
        }

        List<String> backwardsPageTitles2 = wiki.whatLinksHere(pageTitle2);

        if (currentTime + timeout * MILLIS_PER_SEC < System.currentTimeMillis()) {
            throw new TimeoutException("Operation timed out");
        }

        ArrayList<List<String>> paths = new ArrayList<>();
        List<Integer> depth;

        int currentSearchDepth = 1;
        boolean found = false;
        while (!found) {
            depth = Arrays.asList(currentSearchDepth, 0);
            PathThread mainThread = new PathThread(wiki, pageTitle1, pageTitle2,
                currentTime, timeout * MILLIS_PER_SEC, backwardsPageTitles2, depth, 1);
            mainThread.run();
            if (depth.get(1) == 1) {
                throw new TimeoutException("Operation timed out");
            }
            paths = mainThread.output;
            if (paths.size() != 0) {
                found = true;
            }
            currentSearchDepth += 1;
        }


        ArrayList<List<String>> shortestPaths = new ArrayList<>(paths);
        int pathSize = shortestPaths.get(0).size();


        for (int i = 0; i < pathSize && shortestPaths.size() != 1; i++) {
            String smallest = shortestPaths.get(0).get(i);
            ArrayList<List<String>> smallestPaths = new ArrayList<>();
            for (List<String> path : shortestPaths) {
                if (path.get(i).compareTo(smallest) < 0) {
                    smallestPaths = new ArrayList<>(List.of(path));
                } else if (path.get(i).compareTo(smallest) == 0) {
                    smallestPaths.add(path);
                }
            }
            shortestPaths = smallestPaths;
        }


        return shortestPaths.get(0);
    }
}
