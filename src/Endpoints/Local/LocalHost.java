package Endpoints.Local;

import Endpoints.Host;
import Endpoints.Node.OutputHost;

import java.io.File;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
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

    private static Inet4Address localAddress() {
        try {
            return (Inet4Address) Inet4Address.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }


    ArrayList<OutputHost> parseFlags(String[] args) {
        ArrayList<OutputHost> endpoints = new ArrayList<>();
        int mode = 0;

        // modes will tell what type of flag is being cur read

        for (int i = 0; i < args.length; i++) {

            if (args[i].equals("-dir")) {
                mode = 0;
                continue;
            }

            if (args[i].equals("-port")) {
                mode = 1;
                continue;
            }

            if (args[i].equals("-add")) {
                mode = 2;
                continue;
            }


            switch (mode) {
                case 0:
                    directory = new File(args[i]);
                    break;
                case 1:
                    setPort(Integer.parseInt(args[i]));
                    break;
                case 2:
                    endpoints.add(new OutputHost(args[i]));
                    break;
            }
        }

        return endpoints;
    }

    public LocalHost() throws UnknownHostException {
        super((Inet4Address) Inet4Address.getLocalHost(), 22);

        connectionManager = new ConnectionManager(this);
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
