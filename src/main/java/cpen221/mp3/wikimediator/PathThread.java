package cpen221.mp3.wikimediator;

import org.fastily.jwiki.core.Wiki;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PathThread extends Thread {

    private final Wiki wiki;
    private final String pageTitle1;
    private final String pageTitle2;
    private final long beginTime;
    private final long timeout;
    private final List<String> backwardsTitles;
    List<Integer> depth;
    int currentDepth;
    public ArrayList<List<String>> output = new ArrayList<>();

    /* Representation Invariant */
    // wiki is non-null
    // pageTitle1 is a non-null string
    // pageTitle2 is a non-null string
    // beginTime is a positive, long data type
    // timeout is a positive, long data type
    // backwardsTitle is a non-null list
    // depth is a mutable list of integers, containing exactly two integers
    // current depth is a positive, non-null integer
    // output is a non-null arraylist containing non-null lists.

    /* Abstraction Function */
    // Timeout represents time in milliseconds.
    // First element of depth represents the current searching depth which the program is searching
    // Second element is zero if the program has not timed out yet,
    // otherwise one if it has timed out
    // Output is a mutated list which will contain a list of the
    // possible forward paths between pageTitle1 and pageTitle2

    /**
     * Constructor for the pathThread object
     *
     * @param wiki            is non-null
     * @param pageTitle1      a non-null string
     * @param pageTitle2      a non-null string
     * @param beginTime       a positive, long data type
     * @param timeout         a positive, long data type
     * @param backwardsTitles a non-null list
     * @param depth           a mutable list of integers with exactly two values
     * @param currentDepth    positive, non-null integer
     */
    PathThread(Wiki wiki, String pageTitle1, String pageTitle2, long beginTime, long timeout,
               List<String> backwardsTitles, List<Integer> depth, int currentDepth) {
        if (beginTime + timeout < System.currentTimeMillis()) {
            synchronized (depth) {
                depth.set(1, 1);
            }
        }
        this.wiki = wiki;
        this.pageTitle1 = pageTitle1;
        this.pageTitle2 = pageTitle2;
        this.beginTime = beginTime;
        this.timeout = timeout;
        this.backwardsTitles = backwardsTitles;
        this.depth = depth;
        this.currentDepth = currentDepth;
    }

    /**
     * Starts the PathThread to run
     */
    public void run() {
        if (depth.get(1) == 0 && (depth.get(0) == 0 || currentDepth <= depth.get(0))) {
            List<String> forwardTitles = wiki.getLinksOnPage(pageTitle1);

            ArrayList<PathThread> threads = new ArrayList<>();

            for (String page : forwardTitles) {
                if (depth.get(1) == 1) {
                    break;
                }

                if (backwardsTitles.contains(page)) {
                    output.add(Arrays.asList(pageTitle1, page, pageTitle2));
                    break;
                } else {
                    PathThread branchThread = new PathThread(wiki, page, pageTitle2,
                        beginTime, timeout, backwardsTitles, depth, currentDepth + 1);
                    branchThread.start();
                    threads.add(branchThread);
                }
            }
            if (depth.get(1) == 0) {
                for (PathThread thread : threads) {
                    if (depth.get(1) == 1) {
                        break;
                    }
                    try {
                        thread.join();
                        for (List<String> possibility : thread.output) {
                            List<String> newPossibility = new ArrayList<>();
                            newPossibility.add(pageTitle1);
                            newPossibility.addAll(possibility);
                            output.add(newPossibility);
                        }
                    } catch (InterruptedException e) {

                    }
                }
            }

        }

    }

}
