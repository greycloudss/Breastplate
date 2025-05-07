package Endpoints.Local;

import Endpoints.Node.OutputHost;
import helpers.Pair;
import helpers.SSLUtil;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.nio.file.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    String user, pass;

    private Map<File, Long> fileMap = new HashMap<>();

    ArrayList<Pair<File, String>> fileHashPairs = new ArrayList<>();
    ArrayList<String> replaceFiles = new ArrayList<>();

    private Thread dirScannerThread, mainLoopThread, listenerThread;

    volatile boolean recheck;

    volatile boolean killSwitch;

    public ArrayList<String> getAllHashes() {
        ArrayList<String> hashes = new ArrayList<>();

        for (Pair<File, String> pair : fileHashPairs)
            hashes.add(pair.Val());

        return hashes;
    }


    public ArrayList<Pair<File, String>> getFileHashPairs() {
        return fileHashPairs;
    }

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

                if (oldFileMap.size() == files.size() && fileMap.entrySet().iterator().next() == oldFileMap.entrySet().iterator().next())
                    continue;
            }
            System.out.println("[Info] {CM scan} Starting scan");
            fileHashPairs.clear();

            for (File loopFile : files) fileHashPairs.add(new Pair<>(loopFile, returnHashString(Objects.requireNonNull(encodeHash256(loopFile)))));

            sendFiles();
            democracy();

            recheck = false;
        }
    }


    void sendFiles() {
        StringBuilder sb = new StringBuilder();
        Path root = host.getDirectory().toPath().getParent();

        for (Pair<File,String> p : fileHashPairs) {
            String rel = root.relativize(p.Key().toPath())
                    .toString()
                    .replace('\\','/');
            System.out.println("[DEBUG] {CM sendFiles} queuing --->" + rel);
            sb.append(rel).append('|').append(p.Val()).append('\n');
        }

        broadcast(sb.toString());
        System.out.println("[DEBUG] {CM sendFiles} broadcast complete");
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
                DataOutputStream a = new DataOutputStream(endpoint.getSocket().getOutputStream());
                a.writeUTF(output);
                a.flush();

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

    void broadcast(String payload) {
        List<OutputHost> removables = new ArrayList<>();

        for (OutputHost endpoint : endpoints) {
            Socket socket = endpoint.getSocket();
            if (socket == null || socket.isClosed()) {
                removables.add(endpoint);
                continue;
            }

            String tagged = endpoint.getHost() + ":" + endpoint.getPort() + ";;;" + payload;

            try {

                DataOutputStream a = new DataOutputStream(socket.getOutputStream());
                a.writeUTF(tagged);
                a.flush();

            } catch (IOException e) {
                System.out.println("[ERROR] {SendFunc} could not send to: "
                        + endpoint.getHost() + ":" + endpoint.getPort());
                removables.add(endpoint);
            }
        }

        endpoints.removeAll(removables);
    }


    private void closeSockets() {
        for (OutputHost endpoint : endpoints) {
            endpoint.closeSocket();
        }
        endpoints.clear();
    }

    void listen() {
        if (killSwitch || host == null) return;
        if (host.getPort() <= 0) {
            System.out.println("[INFO] Passive mode: not listening (no valid port)");
            return;
        }
        try (SSLServerSocket serverSocket =
                     (SSLServerSocket) SSLUtil.serverFactory().createServerSocket(host.getPort())) {
            serverSocket.setReuseAddress(true);
            while (!killSwitch) {
                SSLSocket s = (SSLSocket) serverSocket.accept();
                OutputHost newPeer = new OutputHost(s);
                newPeer.setConnectionManager(this);
                endpoints.add(newPeer);
                this.forceRecheck();
            }
        } catch (Exception e) {
            System.err.println("[ERROR] {CM listen} Failed to bind SSL server on port " + host.getPort());
        }
    }

    private File lookupLocalFile(String hash) {
        for (Pair<File,String> p : fileHashPairs)
            if (p.Val().equals(hash))
                return p.Key();
        return null;
    }

    public void democracy() {
        endpoints.removeIf(p -> {
            Socket s = p.getSocket();
            return s == null || s.isClosed();
        });
        int n = endpoints.size();
        if (n == 0) return;

        int threshold = (n + 1) / 2;
        Map<String,Integer> dlVotes = new HashMap<>();
        Map<String,Integer> ulVotes = new HashMap<>();

        for (OutputHost peer : endpoints) {
            peer.setConnectionManager(this);
            peer.findMismatchedHashes();
            for (String h : peer.getMissingLocal())  dlVotes.merge(h, 1, Integer::sum);
            for (String h : peer.getMissingRemote()) ulVotes.merge(h, 1, Integer::sum);
        }

        List<String> toDownload = dlVotes.entrySet().stream()
                .filter(e -> e.getValue() >= threshold)
                .map(Map.Entry::getKey).toList();

        List<String> toUpload = ulVotes.entrySet().stream()
                .filter(e -> e.getValue() >= threshold)
                .map(Map.Entry::getKey).toList();

        Path dir   = host.getDirectory().toPath();
        String top = dir.getParent().getFileName().toString();

        for (OutputHost peer : endpoints) {
            String addr = peer.getHost().getHostAddress();

            for (String h : toDownload) {
                String rel = peer.getPathForHash(h);
                if (rel == null) continue;
                rel = rel.replaceFirst("^/*", "");
                Path localDst = dir.getParent().getParent()
                        .resolve(rel);
                SFTP.downloadFile(user, pass, addr, 22, localDst, rel);
            }

            for (String h : toUpload) {
                File f = lookupLocalFile(h);
                if (f == null) continue;

                String rel = peer.getPathForHash(h);
                if (rel == null) {
                    String inner = dir.relativize(f.toPath())
                            .toString()
                            .replace('\\','/');
                    rel = top + "/" + inner;
                } else {
                    rel = rel.replaceFirst("^/*", "");
                }
                SFTP.uploadFile(user, pass, addr, 22, f.toPath(), rel);
            }
        }
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


    ConnectionManager(LocalHost localHost, List<OutputHost> endpoints, String user, String pass) {
        this.endpoints.addAll(endpoints);

        for (OutputHost endpoint : endpoints)
            endpoint.setConnectionManager(this);

        recheck = true;
        host = localHost;

        this.user = user;
        this.pass = pass;

        killSwitch = false;

        dirScannerThread = new Thread(this::recursiveScan);
        mainLoopThread = new Thread(this::mainLoop);
        listenerThread = new Thread(this::listen);

        dirScannerThread.start();
        mainLoopThread.start();
        listenerThread.start();

    }

    ConnectionManager(LocalHost localHost, String user, String pass) {
        recheck = true;
        host = localHost;

        this.user = user;
        this.pass = pass;

        killSwitch = false;

        dirScannerThread = new Thread(this::recursiveScan);
        mainLoopThread = new Thread(this::mainLoop);
        listenerThread = new Thread(this::listen);

        dirScannerThread.start();
        mainLoopThread.start();
        listenerThread.start();

    }
}
