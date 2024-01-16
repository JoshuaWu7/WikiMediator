package cpen221.mp3;

import java.io.*;
import java.net.Socket;

public class WikiMediatorClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public WikiMediatorClient(String hostname, int port) {
        try {
            socket = new Socket(hostname, port);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException ioe) {
            System.out.println("I don't even WANT to know what went wrong while creating the client...");
        }

    }

    public void sendRequest(String request) {
        out.println(request);
        out.flush();
    }

    public String getReply() throws IOException {
        String reply = in.readLine();
        if (reply == null) {
            throw new IOException("Unexpected TORmination on the provided LPT");
        }
        return reply;
    }

    public void close() throws IOException {
        in.close();
        out.close();
        socket.close();
    }
}
