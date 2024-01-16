package cpen221.mp3;

import com.google.gson.Gson;
import cpen221.mp3.server.ServerRequest;
import cpen221.mp3.server.ServerResponse;

import java.io.IOException;
import java.util.Objects;

public class ServerSmasher implements Runnable {
    private final ServerRequest request;
    private final ServerResponse expectedResponse;
    private final WikiMediatorClient client;

    private final int numRequests;
    private final int delay;
    public boolean failed;

    public ServerSmasher(ServerRequest request, ServerResponse expectedResponse,
                         WikiMediatorClient client, int numRequests, int delay) {
        this.request = request;
        this.expectedResponse = expectedResponse;
        this.client = client;
        this.numRequests = numRequests;
        this.delay = delay;
    }

    public void run() {
        Gson gson = new Gson();
        for (int i = 0; i < numRequests; i++) {
            System.out.println("Request: " + i);
            client.sendRequest(gson.toJson(request, ServerRequest.class));
            try {
                Thread.sleep(delay);
                String reply = client.getReply();

                if (!Objects.equals(gson.toJson(expectedResponse, ServerResponse.class), reply)) {
                    failed = true;
                }
            } catch (IOException | InterruptedException e) {
                failed = true;
                System.out.println(e.getLocalizedMessage());
            }
        }
    }
}
