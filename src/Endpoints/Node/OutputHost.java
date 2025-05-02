package Endpoints.Node;

import Endpoints.Host;
import Endpoints.Local.ConnectionManager;

import java.io.InputStream;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class OutputHost extends Host {
    private volatile Socket socket;
    private final Thread connectThread;
    ConnectionManager connectionManager;

    public void setConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    private void connectAndConfigure(Inet4Address host, int port) {
        try {
            this.socket = new Socket(host, port);
            socket.setSoTimeout(10000);
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(true);
            socket.setReuseAddress(true);
            new Thread(() -> {
                try (InputStream in = socket.getInputStream()) {
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        String msg = new String(buf, 0, len, StandardCharsets.UTF_8);
                        System.out.println("[RECEIVED] from " + getHost() + ":" + getPort() + " :\n" + msg);
                    }
                } catch (IOException ignored) {
                }
            }).start();
        } catch (IOException e) {
            System.out.println("[ERROR] could not connect to " + host + ":" + port);
        }
    }

    public OutputHost(Inet4Address host, int port) {
        super(host, port);
        connectThread = new Thread(() -> connectAndConfigure(getHost(), getPort()));
        connectThread.start();
    }

    public OutputHost(Socket socket) {
        super(socket);
        try {
            socket.setSoTimeout(10000);
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(true);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        this.socket = socket;
        new Thread(() -> {
            try (InputStream in = socket.getInputStream()) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) != -1) {
                    String msg = new String(buf, 0, len, StandardCharsets.UTF_8);
                    System.out.println("[RECEIVED] from " + getHost() + ":" + getPort() + " : " + msg);
                }
            } catch (IOException ignored) {
            }
        }).start();
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
