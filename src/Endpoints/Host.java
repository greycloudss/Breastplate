package Endpoints;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public abstract class Host {
    private Inet4Address host;
    private int port;
    private volatile List<byte[]> files = new ArrayList<>();

    public Host(Inet4Address host, int port) {
        try {
            this.host = host;
            this.port = port;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public Host(Socket socket) {
        try {
            setPort(socket.getPort());
            setHost((Inet4Address) socket.getInetAddress());

        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("[ERROR] {HOST} could not connect to host " + getHost());
        }
    }

    private void parseAddress(String address) {
        try {
            String[] parts = address.split(":");
            this.port = Integer.parseInt(parts[1]);

            if (parts.length != 2) {
                System.out.println("[ERROR] Invalid host address (0): " + address);
                return;
            }

            parts = parts[0].trim().toLowerCase().split("\\.");

            if (parts.length != 4) {
                System.out.println("[ERROR] Invalid host address (1): " + address);
                return;
            }

            byte[] arr = new byte[4];

            for (int i = 0; i < 4; i++) arr[i] = (byte) Integer.parseInt(parts[i]);

            host = (Inet4Address) Inet4Address.getByAddress(arr);
        } catch (Exception e) {
            System.out.println("[ERROR] Invalid host address (2): " + address);
        }
    }

    public Host(String address) {
        parseAddress(address);
    }

    public void setHost(Inet4Address host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Inet4Address getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public List<byte[]> getFiles() {
        return files;
    }
}
