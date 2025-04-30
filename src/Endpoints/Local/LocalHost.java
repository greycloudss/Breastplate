package Endpoints.Local;

import Endpoints.Host;
import Endpoints.Node.OutputHost;

import java.io.File;
import java.net.Inet4Address;
import java.util.List;

/*
        insight[] = {
        should allow new connections,
        allow updates,
        broadcasting enabled?
        }
*/

public class LocalHost extends Host {
    private ConnectionManager connectionManager;
    private boolean[] insight;
    private File directory;

    private Thread conManThread, mainThread;

    public LocalHost(Inet4Address host, int port) {
        super(host, port);
        insight = new boolean[]{true, true, true};
    }

    public File getDirectory() {
        return directory;
    }


    void changeInsights(boolean[] newInsights) {
        insight = newInsights;
    }

    boolean[] returnOperationalInsight() {
        return insight;
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
