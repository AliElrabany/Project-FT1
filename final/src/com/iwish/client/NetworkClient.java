package com.iwish.client;

import com.iwish.shared.Message;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


public class NetworkClient {

    private String host;
    private int port;

    public NetworkClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public Message send(Message request) throws Exception {
        Socket socket = new Socket(host, port);
        socket.setSoTimeout(8000);

        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        out.writeObject(request);
        out.flush();

        Message response = (Message) in.readObject();

        in.close();
        out.close();
        socket.close();

        return response;
    }
}
