package Endpoints.Local;

import Endpoints.Node.OutputHost;
import helpers.Pair;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/*          ---------------TODO LIST ---------------
*       [1] Instead of raw tcp with basically no encryption add  ztna or at least sslsockets
*
*       [2] optimise due to vm maintaining costing money
*
*       [3] how about actually finishing this project lol
*/



/*          ---------------RANDOM COMMENTS---------------

    0) if should check -> broadcast -> receive answer -> 1,2,3

    1) if answer bad then SFTP in the files as update

    2) if answer good then return to 0

    3) if answer mixed elect via democracy to update 4,5

    4) if democracy then 1

    5) if tie try to remove/add your own as answer due to it becoming 2n+1 return to 0

*/



public class ConnectionManager {
    private final LocalHost host;

    private volatile ArrayList<File> files = new ArrayList<>();

    private volatile ArrayList<OutputHost> endpoints = new ArrayList<>();

    private volatile ArrayList<OutputHost> updEndpoints;

    private Map<File, Long> fileMap = new HashMap<>();

    ArrayList<Pair<File, String>> fileHashPairs = new ArrayList<>();
    ArrayList<String> replaceFiles = new ArrayList<>();



    private Thread dirScannerThread, mainLoopThread, listenerThread, senderThread;

    volatile boolean recheck;

    volatile boolean killSwitch;

    private String returnHashString(byte[] hash) {
        StringBuilder string = new StringBuilder();
        for (byte b : hash)
            string.append(String.format("%02x", b));

        return string.toString();
    }

    private byte[] encodeHash256(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            return digest.digest();
        } catch (Exception e) {
            System.out.println("[ERROR] {CM enc256} failed to encode hash");
            return null;
        }
    }

    List<File> recursiveFileScan(File file) {
        List<File> subDirFiles = new ArrayList<>();
        File[] files = file.listFiles();

        if (files == null) return subDirFiles;

        for (File f : files) {
            if (f.isDirectory()) subDirFiles.addAll(recursiveFileScan(f));
            else subDirFiles.add(f);
        }

        return subDirFiles;
    }

    void recursiveScan() {
        while (!killSwitch) {
            System.gc();

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                System.out.println("[Error] {CM scan} Failed to recursive scan due to sleep");
                System.out.println(e.getMessage());
            }

            if (files.isEmpty() || files == null) {
                this.files = (ArrayList<File>) recursiveFileScan(host.getDirectory());

            } else {
                Map<File, Long> oldFileMap = files.stream().collect(Collectors.toMap(f -> f, File::lastModified, Math::max));

                this.files = (ArrayList<File>) recursiveFileScan(host.getDirectory());

                fileMap = files.stream().collect(Collectors.toMap(f -> f, File::lastModified, Math::max));

                if (oldFileMap.size() == files.size() || fileMap.entrySet().iterator().next() == oldFileMap.entrySet().iterator().next())
                    continue;
            }
            System.out.println("[Info] {CM scan} Starting scan");
            fileHashPairs.clear();

            for (File loopFile : files)
                fileHashPairs.add(new Pair<>(loopFile, returnHashString(Objects.requireNonNull(encodeHash256(loopFile)))));


            recheck = false;
        }
    }

    // some of these sending funcs might be redundant but ill keep them for now

    void sendFiles() {
        StringBuilder a = new StringBuilder();
        for (Pair<File, String> pair : fileHashPairs) {
            a.append(pair.Val()).append("\n");
        }

        broadcast(a.toString());
    }

    boolean[] multicast(ArrayList<OutputHost> endpointList, String output) {
        boolean[] multicasted = new boolean[endpointList.size()];

        for (int i = 0; i < endpointList.size(); i++)
            multicasted[i] = (unicast(endpointList.get(i), output));

        return multicasted;
    }

    boolean unicast(OutputHost endpoint, String output) {

        //basically retry connection until it can be established if not after retrying 3 times - quit

        for (int i = 0; i < 3; i++) {
            try {
                endpoint.getSocket().getOutputStream().write(output.getBytes());
                endpoint.getSocket().getOutputStream().flush(); // subject for change

            } catch (Exception e) {
                System.out.println("[ERROR] {CM unicast} failed to write to socket");
                if (i == 2) return false;
            }

            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                System.out.println("[ERROR] {CM unicast} failed to sleep");
            }
        }

        return true;
    }

    void broadcast(String output) {
        for (OutputHost endpoint : endpoints) {
            try (Socket socket = endpoint.getSocket()) {
                socket.getOutputStream().write(output.getBytes());

            } catch (IOException e) {
                System.out.println("[ERROR] {SendFunc} were not able to initiate connection with: " + endpoint.getHost() + ":" + endpoint.getPort());
            }
        }
    }



    void listen() {
        if (killSwitch || host == null) return;

        try (ServerSocket serverSocket = new ServerSocket(host.getPort())) {
            do {
                endpoints.add(new OutputHost(serverSocket.accept()));

            } while (!killSwitch);

        } catch (Exception e) {
            System.err.println("[ERROR] {CM listen}could not bind server socket on port " + host.getPort());
            //got to think about what i want to add here
        } finally {
            //cleanup
            for (OutputHost endpoint : endpoints)
                endpoint.closeSocket();

            endpoints = null;
        }
    }

    boolean democracy() {
        int against = 0;

        for (OutputHost endpoint : endpoints) {
            //add each endpoint disagreement pair of pos and returnHash
        }

        return endpoints.size() - against >= against;
    }

    void mainLoop() {
        while (!killSwitch) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                System.out.println("[Error] {CM main} Failed to scan due to sleep");
            }

            if (!recheck) continue;
            System.out.println("[Info] {CM main} Starting scan");

            sendFiles();

            System.gc();
        }
    }

    public boolean recheckStatus() {
        return recheck;
    }

    public void forceRecheck() {
        recheck = true;
    }


    ConnectionManager(LocalHost localHost, List<OutputHost> endpoints) {
        this.endpoints.addAll(endpoints);

        recheck = true;
        host = localHost;

        killSwitch = false;

        dirScannerThread = new Thread(this::recursiveScan);
        mainLoopThread = new Thread(this::mainLoop);
        listenerThread = new Thread(this::listen);

        dirScannerThread.start();
        mainLoopThread.start();
        listenerThread.start();

    }

    ConnectionManager(LocalHost localHost) {
        recheck = true;
        host = localHost;

        killSwitch = false;

        dirScannerThread = new Thread(this::recursiveScan);
        mainLoopThread = new Thread(this::mainLoop);
        listenerThread = new Thread(this::listen);

        dirScannerThread.start();
        mainLoopThread.start();
        listenerThread.start();

    }
}
