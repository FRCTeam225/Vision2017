package org.techfire225.firevision2017;


import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class VisionServer extends Thread {
    ServerSocket socket;
    ArrayList<VisionClient> clients = new ArrayList<>();

    boolean isConnected = false;
    boolean lastState = false;

    MainActivity activity;
    public VisionServer(MainActivity activity, int port) throws IOException {
        this.activity = activity;
        socket = new ServerSocket(port);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                VisionServer.this.activity.changeConnectionState(false);
            }
        });

        start();
    }

    public void publish(final VisionMessage pkt) {
        new Thread() {
            public void run() {
                synchronized (clients) {
                    ArrayList<VisionClient> failedClients = new ArrayList<>();
                    for (VisionClient client : clients) {
                        try {
                            client.sendToClient(pkt);
                        } catch (IOException e) {
                            failedClients.add(client);
                        }
                    }
                    clients.removeAll(failedClients);

                    isConnected = clients.size() != 0;

                    if ( isConnected != lastState ) {
                        // Update the header to show that a client connected
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                activity.changeConnectionState(isConnected);
                            }
                        });
                    }

                    lastState = isConnected;
                }
            }
        }.start();
    }

    public void run() {
        while ( socket.isBound() ) {
            try {
                Socket client = socket.accept();
                clients.add(new VisionClient(client));
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public class VisionClient {
        Socket client;
        ObjectOutputStream sender;
        InputStream reader;
        Object lock = new Object();
        public VisionClient(Socket client) throws IOException {
            this.client = client;
            sender = new ObjectOutputStream(client.getOutputStream());
            reader = client.getInputStream();
        }

        public void sendToClient(VisionMessage msg) throws IOException {
            if ( !client.isConnected() )
                throw new IOException("Socket closed");
            synchronized (lock) {
                sender.writeObject(msg);
            }
        }

        public void close() throws IOException {
            client.close();
        }
    }
}