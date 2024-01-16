package cpen221.mp3.server;

import com.google.gson.Gson;
import cpen221.mp3.wikimediator.WikiMediator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class WikiMediatorServer {

    private static final int WAIT_DURATION_SECONDS = 5;

    private ServerSocket serverSocket;
    private WikiMediator mediator;
    private ExecutorService requestHandler;

    private boolean running;

    /* Representation Invariants */
    // serverSocket is never null
    // mediator is never null
    // requestHandler is never null and will have a fixed pool of given size

    /* Abstraction Function */
    // represents a server that that listens to requests made from

    /**
     * Start a server at a given port number, with the ability to process
     * upto n requests concurrently.
     *
     * @param port         the port number to bind the server to, 9000 <= {@code port} <= 9999
     * @param n            the number of concurrent requests the server can handle, 0 < {@code n} <= 32
     * @param wikiMediator the WikiMediator instance to use for the server, {@code wikiMediator} is not {@code null}
     */
    public WikiMediatorServer(int port, int n, WikiMediator wikiMediator) {
        try {
            this.serverSocket = new ServerSocket(port);
            this.mediator = wikiMediator;
            this.requestHandler = Executors.newFixedThreadPool(n);
            running = true;
        } catch (Exception ioe) {
            System.out.println(ioe.getLocalizedMessage());
        }
    }

    /**
     * Runs the server, listens for connections and handles them.
     */
    public void serve() {
        while (running) {
            //                final Socket socket = serverSocket.accept();
            final ExecutorService socketAcceptor = Executors.newSingleThreadExecutor();

            SocketChecker socketChecker = new SocketChecker();
            final Future<?> future = socketAcceptor.submit(socketChecker);
            socketAcceptor.shutdown();

            try {
                future.get(WAIT_DURATION_SECONDS, TimeUnit.SECONDS);
                Thread handler = new Thread(() -> {
                    // Generates one thread per client
                    try {
                        try {
                            handle(socketChecker.socket);
                        } finally {
                            socketChecker.socket.close();
                        }
                    } catch (IOException ioe) {
                        throw new RuntimeException(ioe.getLocalizedMessage());
                    }
                });
                handler.start();

            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e.getLocalizedMessage());

            } catch (TimeoutException e) {
                socketAcceptor.shutdownNow();
            }
        }
        System.out.println("Shutting down");
        requestHandler.shutdown();
    }

    /**
     * Handles the socket by creating multiple threads and creates a ServerResponse
     *
     * @param socket is non-null
     * @throws IOException if socket fails to be handled
     */
    private void handle(Socket socket) throws IOException {
        System.err.println("Client connected");

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

        for (String line = in.readLine(); line != null; line = in.readLine()) {
            Gson gson = new Gson();
            ServerRequest request = gson.fromJson(line, ServerRequest.class);

            // Requests are all handled by threads in the server's thread pool (of size n).
            final Runnable serverResponseGenerator = new Thread(() -> {
                ServerResponse response = generateResponse(request);
                String output = gson.toJson(response);

                out.println(output);
                out.flush();

                if (Objects.equals(request.type, "stop")) {
                    running = false;
                }
            });
            requestHandler.execute(serverResponseGenerator);
        }
        out.close();
        in.close();
    }

    /**
     * Generates a formatted ServerResponse from the ServerRequest.
     * Takes into account whether the request timeout is null or not.
     *
     * @param request a non-null ServerRequest
     * @return a non-null ServerResponse
     */
    private ServerResponse generateResponse(ServerRequest request) {
        String status;

        final ExecutorService responseRetrieval = Executors.newSingleThreadExecutor();
        CountDownLatch latch = new CountDownLatch(1);

        MediatorClient mediatorClient = new MediatorClient(mediator, request, latch);

        if (request.timeout == null) {
            responseRetrieval.submit(mediatorClient);
            responseRetrieval.shutdown();
            try {
                latch.await();
                status = "success";
            } catch (InterruptedException ie) {
                status = "failed";
                mediatorClient.response = ie.getLocalizedMessage();
            }

        } else {
            final Future<?> future = responseRetrieval.submit(mediatorClient);
            responseRetrieval.shutdown();
            try {
                future.get(Integer.parseInt(request.timeout), TimeUnit.SECONDS);
                status = "success";
            } catch (InterruptedException ie) {
                status = "failed";
                mediatorClient.response = "Interrupted Exception was thrown";

            } catch (ExecutionException ee) {
                status = "failed";
                mediatorClient.response = ee.getLocalizedMessage();

            } catch (TimeoutException te) {
                status = "failed";
                mediatorClient.response = "Operation timed out";
            }
            if (!responseRetrieval.isTerminated()) {
                responseRetrieval.shutdownNow();
            }
        }
        return formatResponse(request, status, mediatorClient.response, mediatorClient.responses);
    }

    /**
     * A helper function in generating formatted responses
     *
     * @param request   a non-null ServerRequest
     * @param status    a non-null status String
     * @param response  a non-null status String
     * @param responses a non-null list of strings representing responses
     * @return a non-null ServerResponse
     */
    private ServerResponse formatResponse(ServerRequest request, String status,
                                          String response, List<String> responses) {
        ServerResponse serverResponse = new ServerResponse();
        serverResponse.setId(request.id);
        serverResponse.setResponse(response);
        serverResponse.setResponses(responses);


        if (Objects.equals(request.type, "stop")) {
            serverResponse.setStatus(null);
        } else if (Objects.equals(request.type, "shortestPath")) {
            if (responses == null) {
                serverResponse.setStatus("failed");
            } else {
                serverResponse.setStatus(status);
            }
        } else {
            serverResponse.setStatus(status);
            if (Objects.equals(response, "Malformed request")) {
                serverResponse.setStatus("failed");
            }
        }

        return serverResponse;
    }

    private class SocketChecker implements Runnable {
        public Socket socket;

        public void run() {
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                System.out.println(e.getLocalizedMessage());
            }
        }
    }

    private static class MediatorClient implements Runnable {
        public String response;
        public List<String> responses;

        private final WikiMediator mediator;
        private final ServerRequest request;
        private final CountDownLatch latch;

        /* Representation Invariants */
        // response: a string that is non-null
        // responses: a list of string that is non-null
        // mediator is non-null
        // request is non-null
        // latch is non-null

        /* Abstraction Function */
        // represents a mediator client that run as a thread to responds to a ServerRequest
        // depending on it's corresponding fields.

        /**
         * Constructor for the MediatorClient
         *
         * @param mediator a non-null WikiMediator
         * @param request  a non-null ServerRequest
         * @param latch    a non-null CountDownLatch
         */
        public MediatorClient(WikiMediator mediator, ServerRequest request, CountDownLatch latch) {
            this.mediator = mediator;
            this.request = request;
            this.latch = latch;
        }

        /**
         * Run method for a MediatorClient thread
         */
        public void run() {
            if (Objects.equals(request.type, "search")) {
                responses = new ArrayList<>(
                    mediator.search(request.query, Integer.parseInt(request.limit)));

            } else if (Objects.equals(request.type, "getPage")) {
                response = mediator.getPage(request.pageTitle);

            } else if (Objects.equals(request.type, "zeitgeist")) {
                responses = new ArrayList<>(mediator.zeitgeist(Integer.parseInt(request.limit)));

            } else if (Objects.equals(request.type, "trending")) {
                responses =
                    new ArrayList<>(mediator.trending(Integer.parseInt(request.timeLimitInSeconds),
                        Integer.parseInt(request.maxItems)));

            } else if (Objects.equals(request.type, "windowedPeakLoad")) {
                if (request.timeWindowInSeconds != null) {
                    response = String.valueOf(
                        mediator.windowedPeakLoad(Integer.parseInt(request.timeWindowInSeconds)));
                } else {
                    response = String.valueOf(mediator.windowedPeakLoad());
                }
            } else if (Objects.equals(request.type, "shortestPath")) {
                try {
                    responses = new ArrayList<>(
                        mediator.shortestPath(request.pageTitle1, request.pageTitle2,
                            Integer.parseInt(request.timeout)));
                } catch (TimeoutException e) {
                    response = e.getLocalizedMessage();
                }
            } else if (Objects.equals(request.type, "stop")) {
                response = "bye";
            } else {
                response = "Malformed request";
            }
            latch.countDown();
        }
    }

}
