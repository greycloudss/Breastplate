package Endpoints.Local;

import Endpoints.Node.OutputHost;
import helpers.Pair;
import helpers.SSLUtil;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
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

    private Map<File, Long> fileMap = new HashMap<>();

    ArrayList<Pair<File, String>> fileHashPairs = new ArrayList<>();
    ArrayList<String> replaceFiles = new ArrayList<>();

    private Thread dirScannerThread, mainLoopThread, listenerThread;

    volatile boolean recheck;

    volatile boolean killSwitch;

    private String sshUser;
    private String sshPass = "";

    public void setSshUser(String sshUser) {
        this.sshUser = sshUser;
    }

    public void setSshPass(String sshPass) {
        this.sshPass = sshPass;
    }

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

    // some of these sending funcs might be redundant but ill keep them for now

    void sendFiles() {
        StringBuilder a = new StringBuilder();
        for (Pair<File, String> pair : fileHashPairs) a.append(pair.Val()).append("\n");

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
        try (SSLServerSocket serverSocket = (SSLServerSocket) SSLUtil.serverFactory().createServerSocket(host.getPort())) {
            serverSocket.setReuseAddress(true);
            while (!killSwitch) {
                SSLSocket s = (SSLSocket)serverSocket.accept();
                endpoints.add(new OutputHost(s));
                if (endpoints.getLast().getConnectionManager()==null)
                    endpoints.getLast().setConnectionManager(this);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] {CM listen} could not bind SSL server socket on port " + host.getPort());
        }
    }

    private File lookupLocalFile(String hash) {
        return new File(host.getDirectory(), hash);
    }


    public void democracy() {
        int n = endpoints.size();
        int threshold = (n + 1) / 2;

        Map<String, Integer> downloadVotes = new HashMap<>();
        Map<String, Integer> sendVotes     = new HashMap<>();
        Map<Pair<String, String>, Integer> replaceVotes = new HashMap<>();

        for (OutputHost peer : endpoints) {
            peer.setConnectionManager(this);
            peer.findMismatchedHashes();
            for (String h : peer.getMissingLocal())   downloadVotes.merge(h, 1, Integer::sum);
            for (String h : peer.getMissingRemote())  sendVotes.merge(h, 1, Integer::sum);
            for (Pair<String, String> p : peer.getMismatches()) replaceVotes.merge(p, 1, Integer::sum);
        }

        List<String> toDownload = downloadVotes.entrySet().stream()
                .filter(e -> e.getValue() >= threshold)
                .map(Map.Entry::getKey)
                .toList();

        List<String> toSend = sendVotes.entrySet().stream()
                .filter(e -> e.getValue() >= threshold)
                .map(Map.Entry::getKey)
                .toList();

        List<Pair<String, String>> toReplace = replaceVotes.entrySet().stream()
                .filter(e -> e.getValue() >= threshold)
                .map(Map.Entry::getKey)
                .toList();

        for (OutputHost peer : endpoints) {
            String peerHost = peer.getHost().getHostAddress();
            int peerPort    = 22;
            for (String hash : toDownload) {
                File f = lookupLocalFile(hash);
                SFTP.downloadFile(sshUser, sshPass, peerHost, peerPort,
                        f.toPath(), "/remote/dir/" + hash);
            }
            for (String hash : toSend) {
                File f = lookupLocalFile(hash);
                SFTP.uploadFile(sshUser, sshPass, peerHost, peerPort,
                        f.toPath(), "/remote/dir/" + hash);
            }
            for (Pair<String, String> p : toReplace) {
                File f = lookupLocalFile(p.Key());
                SFTP.replaceFile(sshUser, sshPass, peerHost, peerPort,
                        f.toPath(), "/remote/dir/" + p.Val());
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

        killSwitch = false;

        dirScannerThread = new Thread(this::recursiveScan);
        mainLoopThread = new Thread(this::mainLoop);
        listenerThread = new Thread(this::listen);

        dirScannerThread.start();
        mainLoopThread.start();
        listenerThread.start();

    }
}
