package Endpoints.Node;

import Endpoints.Host;

import java.net.Inet4Address;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

public class OutputHost extends Host {
    Socket socket;

    public OutputHost(Inet4Address host, int port) {
        super(host, port);
        try {
            socket.setSoTimeout(10000);
            socket.setTcpNoDelay(true);
            socket = new Socket(super.getHost(), super.getPort());

        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("[ERROR] {OutputHost} could not connect to host " + super.getHost());
        }
    }

    public OutputHost(Socket socket) {
        super(socket);
        try {
            socket.setSoTimeout(10000);
            socket.setTcpNoDelay(true);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        this.socket = socket;

    }

    public void closeSocket() {
        if (socket == null) return;
        do {
            try {
                Thread.sleep(3000);
                socket.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                System.out.println("[ERROR] {OutputHost} could not close host " + super.getHost());
            }
        } while (socket != null);

    }

    public Socket getSocket() {
        return socket;
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
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public List<byte[]> getFiles() {
        return super.getFiles();
    }
}
