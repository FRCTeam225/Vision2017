package org.techfire225.firevision2017;


import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

/*
 * MjpgServer
 *
 * Original version by Team 254, The Cheesy Poofs
 * Original source at https://github.com/Team254/FRC-2016-Public/blob/master/vision_app/app/src/main/java/com/team254/cheezdroid/MjpgServer.java
 *
 * Modified to support sending output from our OpenCV pipeline
 */
public class MjpgServer {

    PublishImage publisher;

    public static final String K_BOUNDARY = "boundary";
    private static MjpgServer sInst = null;

    public static final String TAG = "MJPG";

    public static MjpgServer getInstance() {
        if (sInst == null) {
            sInst = new MjpgServer();
        }
        return sInst;
    }

    private ArrayList<Connection> mConnections = new ArrayList<>();
    private Object mLock = new Object();

    private class Connection {

        private Socket mSocket;

        public Connection(Socket s) {
            mSocket = s;
        }

        public boolean isAlive() {
            return !mSocket.isClosed() && mSocket.isConnected();
        }

        public void start() {
            try {
                Log.i(TAG, "Starting a connection!");
                OutputStream stream = mSocket.getOutputStream();
                stream.write(("HTTP/1.0 200 OK\r\n" +
                        "Server: cheezyvision\r\n" +
                        "Cache-Control: no-cache\r\n" +
                        "Pragma: no-cache\r\n" +
                        "Connection: close\r\n" +
                        "Content-Type: multipart/x-mixed-replace;boundary=--" + K_BOUNDARY + "\r\n").getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void writeImageUpdate(byte[] buffer) {
            if (!isAlive()) {
                return;
            }
            OutputStream stream = null;
            try {
                stream = mSocket.getOutputStream();
                stream.write(("\r\n--" + K_BOUNDARY + "\r\n").getBytes());
                stream.write(("Content-type: image/jpeg\r\n" +
                        "Content-Length: " + buffer.length + "\r\n" +
                        "\r\n").getBytes());
                stream.write(buffer);
                stream.flush();
            } catch (IOException e) {
                // There is a broken pipe exception being thrown here I cannot figure out.
            }
        }

    }


    private ServerSocket mServerSocket;
    private boolean mRunning;
    private Thread mRunThread;
    private Thread mPubThread;
    private Long mLastUpdate = 0L;

    private MjpgServer() {
        try {
            mServerSocket = new ServerSocket(5800);
            mRunning = true;
            mRunThread = new Thread(runner);
            mRunThread.start();

            mPubThread = new Thread(publisher);
            mPubThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void update(byte[] bytes) {
        new SendUpdateTask().execute(bytes);
    }

    private class SendUpdateTask extends AsyncTask<byte[], Void, Void> {

        @Override
        protected Void doInBackground(byte[]... params) {
            update(params[0], true);
            return null;
        }
    }

    private void update(byte[] bytes, boolean updateTimer) {
        if (updateTimer) {
            mLastUpdate = System.currentTimeMillis();
        }
        synchronized (mLock) {
            ArrayList<Integer> badIndices = new ArrayList<>(mConnections.size());
            for (int i = 0; i < mConnections.size(); i++) {
                Connection c = mConnections.get(i);
                if (c == null || !c.isAlive()) {
                    badIndices.add(i);
                } else {
                    c.writeImageUpdate(bytes);
                }
            }

            for (int i : badIndices) {
                mConnections.remove(i);
            }
        }
    }

    public void publish(long matPtr) {
        if ( publisher == null ) {
            publisher = new PublishImage(matPtr);
            publisher.start();
        }
        else if ( publisher.isAlive() )
            Native.releaseImage(matPtr);
        else {
            publisher = new PublishImage(matPtr);
            publisher.start();
        }
    }


    public class PublishImage extends Thread {
        long ptr;
        public PublishImage(long ptr) { this.ptr = ptr; }


        public void run() {
            update(Native.processJpg(ptr));
        }
    };

    Runnable runner = new Runnable() {

        @Override
        public void run() {
            while (mRunning) {
                try {
                    Log.i(TAG, "Waiting for connections");
                    Socket s = mServerSocket.accept();
                    Log.i("MjpgServer", "Got a socket: " + s);
                    Connection c = new Connection(s);
                    synchronized (mLock) {
                        mConnections.add(c);
                    }
                    c.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };
}
