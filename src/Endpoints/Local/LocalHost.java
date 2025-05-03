package Endpoints.Local;

import Endpoints.Host;
import Endpoints.Node.OutputHost;

import java.io.File;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static java.net.Inet4Address.*;


public class LocalHost extends Host {
    private final ConnectionManager connectionManager;

    private final File directory;


    private static class Config {
        final List<OutputHost> endpoints;
        final int port;
        final File directory;

        Config(List<OutputHost> endpoints, int port, File directory) {
            this.endpoints = endpoints;
            this.port = port;
            this.directory = directory;
        }
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    private static Config parseArgs(String[] args) {
        List<OutputHost> endpoints = new ArrayList<>();
        File dir = null;
        int port = 0;
        int mode = -1;
        for (String arg : args) {
            switch (arg) {
                case "-dir" -> {
                    mode = 0;
                    continue;
                }
                case "-port" -> {
                    mode = 1;
                    continue;
                }
                case "-add" -> {
                    mode = 2;
                    continue;
                }
            }

            switch (mode) {
                case 0 -> dir = new File(arg);
                case 1 -> port = Integer.parseInt(arg);
                case 2 -> endpoints.add(new OutputHost(arg));
            }
        }
        return new Config(endpoints, port, dir);
    }


    public LocalHost(String[] args) throws UnknownHostException {
        Config cfg = parseArgs(args);
        super((Inet4Address) getLocalHost(), cfg.port);

        this.directory = cfg.directory;
        this.connectionManager = new ConnectionManager(this, cfg.endpoints);
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
