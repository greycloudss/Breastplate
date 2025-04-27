package Endpoints.Local;

import Endpoints.Host;
import Endpoints.Node.OutputHost;

import java.io.File;
import java.net.Inet4Address;
import java.util.List;

public class LocalHost extends Host {
    private ConnectionManager connectionManager;

    private File directory;

    private Thread conManThread, mainThread;


    public LocalHost(Inet4Address host, int port) {
        super(host, port);
    }

    public File getDirectory() {
        return directory;
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
