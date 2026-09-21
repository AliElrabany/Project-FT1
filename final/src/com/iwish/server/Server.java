package com.iwish.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;


public class Server {

    public static final int DEFAULT_PORT = 5050;

    private ServerSocket serverSocket;
    private Database database;
    private volatile boolean running;

    public void start(int port, String dbName) throws Exception {
        System.out.println("[Server] Connecting to MySQL database: " + dbName);
        database = new Database(dbName);

        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("[Server] i-Wish server started on port " + port);
        System.out.println("[Server] Waiting for clients...");

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Server] Client connected: " + clientSocket.getInetAddress());
                Thread thread = new Thread(new ClientHandler(clientSocket, database));
                thread.start();
            } catch (Exception e) {
                if (running) {
                    System.out.println("[Server] Accept error: " + e.getMessage());
                }
            }
        }
    }
    public boolean isRunning() {
        return running;
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception e) {
            System.out.println("[Server] Error while stopping: " + e.getMessage());
        }
        System.out.println("[Server] Stopped.");
    }

    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        String dbName = "iwish";

        if (args.length >= 1) {
            port = Integer.parseInt(args[0]);
        }
        if (args.length >= 2) {
            dbName = args[1];
        }

        final Server server = new Server();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                server.stop();
            }
        });

        server.start(port, dbName);
    }
}
