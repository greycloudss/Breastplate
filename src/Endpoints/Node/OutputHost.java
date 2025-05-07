package Endpoints.Node;

import Endpoints.Host;
import Endpoints.Local.ConnectionManager;
import helpers.Pair;
import helpers.SSLUtil;

import javax.net.ssl.SSLSocket;
import java.io.InputStream;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class OutputHost extends Host {
    private volatile Socket socket;
    private final Thread connectThread;
    ConnectionManager connectionManager;

    private volatile ArrayList<String> fileRecvH = new ArrayList<>();

    private volatile ArrayList<String> missingLocal = new ArrayList<>();
    private volatile ArrayList<String> missingRemote = new ArrayList<>();
    private volatile ArrayList<Pair<String, String>> mismatches = new ArrayList<>();
    private final List<Pair<String,String>> recvPairs = new ArrayList<>();

    public void setConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public void findMismatchedHashes() {
        missingLocal.clear();
        missingRemote.clear();
        mismatches.clear();

        List<String> localHashes = connectionManager.getAllHashes();

        for (String h : fileRecvH)
            if (!localHashes.contains(h))
                missingLocal.add(h);

        for (String h : localHashes)
            if (!fileRecvH.contains(h))
                missingRemote.add(h);

        int k = Math.max(missingLocal.size(), missingRemote.size());
        for (int i = 0; i < k; i++) {
            String r = i < missingLocal .size() ? missingLocal .get(i) : "";
            String l = i < missingRemote.size() ? missingRemote.get(i) : "";
            mismatches.add(new Pair<>(r, l));
        }
    }

    public ArrayList<String> getMissingLocal() {
        return missingLocal;
    }

    public ArrayList<String> getMissingRemote() {
        return missingRemote;
    }

    public ArrayList<Pair<String, String>> getMismatches() {
        return mismatches;
    }

    public String getPathForHash(String hash) {
        for (Pair<String,String> p : recvPairs)
            if (p.Val().equals(hash))
                return p.Key();
        return null;
    }

    void parseMessage(String msg) {
        recvPairs.clear();
        fileRecvH.clear();

        String[] lines = msg.split("\n");
        for (String l : lines) {
            l = l.trim();
            if (l.isEmpty()) continue;
            int bar = l.lastIndexOf('|');
            if (bar < 0) continue;

            String rel  = l.substring(0, bar);
            String hash = l.substring(bar + 1);

            recvPairs.add(new Pair<>(rel, hash));
            fileRecvH.add(hash);
        }
    }

    private void threadConnection() {
        new Thread(() -> {
            try (InputStream in = socket.getInputStream()) {
                byte[] buf = new byte[4096];
                int len;
                StringBuilder buffer = new StringBuilder();

                while ((len = in.read(buf)) != -1) {
                    buffer.append(new String(buf, 0, len, StandardCharsets.UTF_8));

                    int idx;
                    while ((idx = buffer.indexOf("\n")) != -1) {
                        String line = buffer.substring(0, idx).trim();
                        buffer.delete(0, idx + 1);

                        if (line.contains(";;;")) {
                            fileRecvH.clear();
                            line = line.substring(line.lastIndexOf(";;;") + 3);
                        }

                        System.out.println("[RECEIVED] from " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + " : " + line);

                        line = line.trim();
                        parseMessage(line);

                        if (!line.isEmpty() && line.matches("[0-9a-fA-F]{64}")) {
                            fileRecvH.add(line);
                        }
                    }
                }
            } catch (IOException ignored) { }
        }).start();
    }



    private void connectAndConfigure(Inet4Address host, int port) {
        try {
            this.socket = (SSLSocket) SSLUtil.clientFactory().createSocket(host, port);
            ((SSLSocket)socket).startHandshake();
            socket.setSoTimeout(10000);
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(true);
            socket.setReuseAddress(true);
            threadConnection();
        } catch (IOException e) {
            System.out.println("[ERROR] could not connect to " + host + ":" + port);
        }
    }

    public OutputHost(Inet4Address host, int port) {
        super(host, port);
        connectThread = new Thread(() -> connectAndConfigure(getHost(), getPort()));
        connectThread.start();
    }

    void configureSocket(Socket socket) {
        try {
            socket.setSoTimeout(10000);
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(true);
        } catch (Exception e) {
            System.out.println("[ERROR] {oHost config} failed to config socket" + this.getHost() + ":" + this.getPort());
        }
    }

    public OutputHost(Socket socket) {
        super(socket);
        configureSocket(socket);
        this.socket = socket;
        threadConnection();
        connectThread = null;
    }


    public OutputHost(String ep) {
        super(ep);
        connectThread = new Thread(() -> connectAndConfigure(getHost(), getPort()));
        connectThread.start();
    }

    public Socket getSocket() {
        return socket;
    }

    public void closeSocket() {
        Socket s = socket;
        if (s != null) {
            try {
                s.close();
            } catch (IOException ignored) {
                System.out.println("[ERROR] could not close socket: " + s.getInetAddress().getHostAddress() + ":" + s.getPort());
            }
        }
    }

    @Override
    public Inet4Address getHost() {
        return super.getHost();
    }

    @Override
    public int getPort() {
        return super.getPort();
    }

    @Override
    public List<byte[]> getFiles() {
        return super.getFiles();
    }
}