package cpen221.mp3;

import com.google.gson.Gson;
import cpen221.mp3.server.ServerRequest;
import cpen221.mp3.server.ServerResponse;
import cpen221.mp3.server.WikiMediatorServer;
import cpen221.mp3.wikimediator.WikiMediator;
import org.fastily.jwiki.core.Wiki;
import org.junit.jupiter.api.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Task4Tests {

    private static final int PORT = 9999;
    private static final int NUM_REQUESTS = 4;
    private static final int CAPACITY = 1;
    private static final int STALENESS_INTERVAL = 1;
    private static final String HOSTNAME = "127.0.0.1";
    private static final String FILEPATH = "local/request_data";

    private static int requestCounter;

    private static WikiMediator mediator;

    private static WikiMediatorClient client;
    private static WikiMediatorClient client2;
    private static WikiMediatorClient client3;

    private static final Thread mediatorStarter = new Thread(() -> {
        System.out.println("Initializing mediator");
        mediator = new WikiMediator(CAPACITY, STALENESS_INTERVAL);
    });

    private static final Thread serverStarter = new Thread(() -> {
        System.out.println("Starting server");
        WikiMediatorServer server = new WikiMediatorServer(PORT, NUM_REQUESTS, mediator);
        server.serve();
    });

    private static final Thread clientStarter1 = new Thread(() -> client = new WikiMediatorClient(HOSTNAME, PORT));

    private static final Thread clientStarter2 = new Thread(() -> client2 = new WikiMediatorClient(HOSTNAME, PORT));

    private static final Thread clientStarter3 = new Thread(() -> client3 = new WikiMediatorClient(HOSTNAME, PORT));

    public static void clearCache() {
        try {
            PrintWriter writer = new PrintWriter(FILEPATH);
            writer.print("");
            writer.close();
        } catch (FileNotFoundException e) {
            System.out.println(e.getLocalizedMessage());
        }
    }

    @BeforeAll
    public static void init() {
        try {
            clearCache();
            mediatorStarter.start();
            mediatorStarter.join();

            serverStarter.start();
            // Give server some time to start, so client(s) can connect.
            Thread.sleep(500);

            clientStarter1.start();
            clientStarter2.start();
            clientStarter3.start();

            clientStarter1.join();
            clientStarter2.join();
            clientStarter3.join();
        } catch (InterruptedException ie) {
            System.out.println("interrupted while initializing Task 4 tests.");
        }
    }

    @Test
    @Order(1)
    public void pleaseWork() {
        Gson gson = new Gson();
        boolean exception = false;

        String request = "{'id':'1','type':'search','query':'Barack Obama','limit':'1'}";

        ServerResponse expectedResponse = new ServerResponse();
        expectedResponse.setId("1");
        expectedResponse.setResponses(List.of("Barack Obama"));
        expectedResponse.setStatus("success");
        expectedResponse.setResponse(null);

        String expected = gson.toJson(expectedResponse, ServerResponse.class);

        client.sendRequest(request);
        try {
            String reply = client.getReply();
            assertEquals(expected, reply);

        } catch (IOException ioe) {
            System.out.println(ioe.getLocalizedMessage());
            exception = true;
        }
        assertFalse(exception);
        requestCounter ++;
    }

    @Test
    @Order(2)
    public void testTimeout() {
        Gson gson = new Gson();
        Wiki wiki = new Wiki.Builder().withDomain("en.wikipedia.org").build();
        boolean exception = false;

        ServerRequest request = new ServerRequest();
        request.setId("2");
        request.setType("getPage");
        request.setPageTitle("Canada Goose");
        request.setTimeout("5");

        ServerResponse expectedResponse = new ServerResponse();
        expectedResponse.setId("2");
        expectedResponse.setStatus("success");
        expectedResponse.setResponse(wiki.getPageText("Canada Goose"));

        client.sendRequest(gson.toJson(request, ServerRequest.class));

        try {
            String reply = client.getReply();
            assertEquals(gson.toJson(expectedResponse, ServerResponse.class), reply);

        } catch (IOException ioe) {
            exception = true;
            System.out.println(ioe.getLocalizedMessage());
        }
        assertFalse(exception);

        requestCounter++;
    }

    @Test
    @Order(3)
    public void testTimedOut() {
        Gson gson = new Gson();
        boolean exception = false;

        ServerRequest request = new ServerRequest();
        request.setId("two");
        request.setType("search");
        request.setQuery("Canada Goose");
        request.setLimit("20");
        request.setTimeout("0");

        ServerResponse expected = new ServerResponse();
        expected.setId("two");
        expected.setStatus("failed");
        expected.setResponse("Operation timed out");

        client.sendRequest(gson.toJson(request, ServerRequest.class));

        try {
            String reply = client.getReply();
            assertEquals(gson.toJson(expected, ServerResponse.class), reply);
        } catch (IOException ioe) {
            exception = true;
            System.out.println(ioe.getLocalizedMessage());
        }
        assertFalse(exception);

        requestCounter++;
    }

    @Test
    @Order(4)
    public void test3ServerSmashers() {
        boolean interrupted = false;
        Wiki wiki = new Wiki.Builder().withDomain("en.wikipedia.org").build();

        ServerRequest request1 = new ServerRequest();
        request1.setId("smasher A");
        request1.setType("getPage");
        request1.setPageTitle("Godzilla");

        ServerResponse expected1 = new ServerResponse();
        expected1.setId("smasher A");
        expected1.setStatus("success");
        expected1.setResponse(wiki.getPageText("Godzilla"));

        ServerRequest request2 = new ServerRequest();
        request2.setId("smasher B");
        request2.setType("getPage");
        request2.setPageTitle("Communications Satellite");

        ServerResponse expected2 = new ServerResponse();
        expected2.setId("smasher B");
        expected2.setStatus("success");
        expected2.setResponse(wiki.getPageText("Communications Satellite"));

        ServerRequest request3 = new ServerRequest();
        request3.setId("smasher C");
        request3.setType("getPage");
        request3.setPageTitle("Sapphire");

        ServerResponse expected3 = new ServerResponse();
        expected3.setId("smasher C");
        expected3.setStatus("success");
        expected3.setResponse(wiki.getPageText("Sapphire"));

        ServerSmasher a = new ServerSmasher(request1, expected1, client, 100, 10);
        ServerSmasher b = new ServerSmasher(request2, expected2, client2, 100, 10);
        ServerSmasher c = new ServerSmasher(request3, expected3, client3, 100, 10);

        Thread smasherA = new Thread(a);
        Thread smasherB = new Thread(b);
        Thread smasherC = new Thread(c);

        smasherB.start();
        smasherA.start();
        smasherC.start();
        try {
            smasherA.join();
            smasherB.join();
            smasherC.join();

            assertFalse(a.failed);
            assertFalse(b.failed);
            assertFalse(c.failed);
        } catch (InterruptedException e) {
            interrupted = true;
            System.out.println(e.getLocalizedMessage());
        }
        assertFalse(interrupted);

        requestCounter += 300;
    }

    @Test
    @Order(5)
    public void testZeitgeist() {
        Gson gson = new Gson();
        Wiki wiki = new Wiki.Builder().withDomain("en.wikipedia.org").build();
        boolean exception = false;

        // Don't really care about what is returned from this request, because it is non-deterministic
        ServerRequest request1 = new ServerRequest();
        request1.setId("A");
        request1.setType("search");
        request1.setQuery("Sapphire");
        request1.setLimit("5");

        ServerResponse expected1 = new ServerResponse();
        expected1.setId("A");
        expected1.setStatus("success");
        expected1.setResponses(wiki.search("Sapphire", 5));

        ServerRequest zeitgeist = new ServerRequest();
        zeitgeist.setId("B");
        zeitgeist.setType("zeitgeist");
        zeitgeist.setLimit("1");

        ServerResponse expected = new ServerResponse();
        expected.setId("B");
        expected.setStatus("success");
        expected.setResponses(List.of("Sapphire"));

        ServerSmasher a = new ServerSmasher(request1, expected1, client3, 10, 1);
        Thread smasherA = new Thread(a);

        smasherA.start();
        try {
            smasherA.join();
            client2.sendRequest(gson.toJson(zeitgeist, ServerRequest.class));
            String reply = client2.getReply();

            assertEquals(gson.toJson(expected, ServerResponse.class), reply);
        } catch (InterruptedException | IOException e) {
            exception = true;
            System.out.println(e.getLocalizedMessage());
        }
        assertFalse(exception);

        requestCounter += 11;
    }

    @Test
    @Order(6)
    public void testTrending() {
        Wiki wiki = new Wiki.Builder().withDomain("en.wikipedia.org").build();
        boolean exception = false;

        ServerRequest request = new ServerRequest();
        request.setId("5");
        request.setType("trending");
        request.setTimeLimitInSeconds("3600");
        request.setMaxItems("1");

        ServerResponse expected = new ServerResponse();
        expected.setId("5");
        expected.setStatus("success");
        expected.setResponses(List.of("Sapphire"));

        ServerRequest request1 = new ServerRequest();
        request1.setId("6");
        request1.setType("search");
        request1.setQuery("Sapphire");
        request1.setLimit("20");

        ServerResponse expected1 = new ServerResponse();
        expected1.setId("6");
        expected1.setStatus("success");
        expected1.setResponses(wiki.search("Sapphire", 20));

        ServerSmasher a = new ServerSmasher(request1, expected1, client3, 10, 0);
        ServerSmasher b = new ServerSmasher(request, expected, client, 10, 0);

        Thread smasherA = new Thread(a);
        Thread smasherB = new Thread(b);

        smasherA.start();
        try {
            smasherA.join();
            smasherB.start();
            smasherB.join();
            assertFalse(b.failed);
        } catch (InterruptedException e) {
            exception = true;
            System.out.println(e.getLocalizedMessage());
        }
        assertFalse(exception);

        requestCounter += 20;
    }

    @Test
    @Order(7)
    public void testShortestPath() {
        Gson gson = new Gson();
        boolean exception = false;

        ServerRequest request = new ServerRequest();
        request.setId("path");
        request.setType("shortestPath");
        request.setPageTitle1("Anatidae"); // path from pageTitle
        request.setPageTitle2("253 Mathilde"); // to pageTitleB
        request.setTimeout("60"); // In seconds

        ServerResponse expected = new ServerResponse();
        expected.setId("path");
        expected.setStatus("success");
        expected.setResponses(List.of("Anatidae", "Extinction", "Asteroid impact avoidance", "253 Mathilde"));

        client.sendRequest(gson.toJson(request, ServerRequest.class));
        try {
            String reply = client.getReply();
            assertEquals(gson.toJson(expected, ServerResponse.class), reply);
        } catch (IOException e) {
            exception = true;
            System.out.println(e.getLocalizedMessage());
        }
        assertFalse(exception);
        requestCounter++;
    }

    // NOTE: RUNNING THIS TEST BY ITSELF WILL CAUSE IT TO FAIL
    @Test
    @Order(8)
    public void testPeakLoad() {
        Gson gson = new Gson();
        boolean exception = false;

        ServerRequest request = new ServerRequest();
        request.setId("load");
        request.setType("windowedPeakLoad");
        // In the past hour
        request.setTimeWindowInSeconds("3600");

        ServerResponse expected = new ServerResponse();
        expected.setId("load");
        expected.setStatus("success");
        expected.setResponse(String.valueOf(requestCounter + 1));

        ServerRequest defaultLoad = new ServerRequest();
        defaultLoad.setId("default load");
        defaultLoad.setType("windowedPeakLoad");


        try {
            client3.sendRequest(gson.toJson(request, ServerRequest.class));
            String reply = client3.getReply();
            assertEquals(gson.toJson(expected, ServerResponse.class), reply);

            client2.sendRequest(gson.toJson(defaultLoad, ServerRequest.class));
            reply = client2.getReply();

            System.out.println("Peak load in 30 seconds: " + reply);
        } catch (IOException e) {
            exception = true;
            System.out.println(e.getLocalizedMessage());
        }
        assertFalse(exception);
        requestCounter += 2;
    }


    @Test
    @Order(9)
    public void ExceptionsTest() {
        WikiMediator newWikiMed = new WikiMediator(10, 100);
        WikiMediatorServer newServer = new WikiMediatorServer(-1, 0, newWikiMed);

        Gson gson = new Gson();
        boolean exception = false;

        ServerRequest request = new ServerRequest();
        request.setId("anError");
        request.setType("12345error");
        request.setPageTitle1("Anatidae"); // path from pageTitle
        request.setPageTitle2("253 Mathilde"); // to pageTitleB
        request.setTimeout("1"); // In seconds

        ServerResponse expected = new ServerResponse();
        expected.setId("anError");
        expected.setStatus("failed");
        expected.setResponse("Malformed request");

        client.sendRequest(gson.toJson(request, ServerRequest.class));
        try {
            String reply = client.getReply();
            assertEquals(gson.toJson(expected, ServerResponse.class), reply);
        } catch (IOException e) {
            exception = true;
            System.out.println(e.getLocalizedMessage());
        }
        assertFalse(exception);
    }

    @Test
    @Order(10)
    public void testShortestPathTimeout() {
        Gson gson = new Gson();
        boolean exception = false;

        ServerRequest request = new ServerRequest();
        request.setId("path");
        request.setType("shortestPath");
        request.setPageTitle1("Anatidae"); // path from pageTitle
        request.setPageTitle2("253 Mathilde"); // to pageTitleB
        request.setTimeout("1"); // In seconds

        ServerResponse expected = new ServerResponse();
        expected.setId("path");
        expected.setStatus("failed");
        expected.setResponse("Operation timed out");

        client.sendRequest(gson.toJson(request, ServerRequest.class));
        try {
            String reply = client.getReply();
            assertEquals(gson.toJson(expected, ServerResponse.class), reply);
        } catch (IOException e) {
            exception = true;
            System.out.println(e.getLocalizedMessage());
        }
        assertFalse(exception);
        requestCounter++;
    }

    // Don't want the server to shut down while a test is executing.
    @AfterAll
    public static void serverShutdown() {
        Gson gson = new Gson();
        boolean exception = false;

        ServerRequest request = new ServerRequest();
        request.setId("The Minecraft Admin");
        request.setType("stop");

        ServerResponse expectedResponse = new ServerResponse();
        expectedResponse.setId("The Minecraft Admin");
        expectedResponse.setResponse("bye");

        client.sendRequest(gson.toJson(request, ServerRequest.class));
        try {
            String reply = client.getReply();
            assertEquals(gson.toJson(expectedResponse, ServerResponse.class), reply);
            client.close();
            client2.close();
            client3.close();
            serverStarter.join();

        } catch (IOException | InterruptedException ioe) {
            System.out.println("Server couldn't shut down.");
            System.out.println(ioe.getLocalizedMessage());
            exception = true;
        }

        assertFalse(exception);

        clearCache();
    }
}
