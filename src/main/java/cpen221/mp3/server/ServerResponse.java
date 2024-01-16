package cpen221.mp3.server;

import java.util.List;

public class ServerResponse {
    String id;
    String status;
    String response;
    List<String> responses;

    /* Representation Invariant */
    // All fields except id can possibly be null.

    /* Abstraction Function */
    // A representation of a serverResponse sent from a WikiMediatorServer containing fields
    // corresponding to different ServerResponse parameters.

    public ServerResponse() {
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public void setResponses(List<String> responses) {
        this.responses = responses;
    }
}
