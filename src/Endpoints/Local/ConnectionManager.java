package Endpoints.Local;

import Endpoints.Node.OutputHost;
import helpers.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

public class ConnectionManager {
    private LocalHost host;

    private volatile ArrayList<File> files;

    private volatile ArrayList<OutputHost> endpoints;

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
            return null;
        }
    }

    List<File> recursiveFileScan(File file) {
        List<File> subDirFiles = new ArrayList<>();
        File[] files = host.getDirectory().listFiles();

        if (files == null) return subDirFiles;

        for (File loopFile : files) {
            if (file.isDirectory()) subDirFiles.addAll(recursiveFileScan(loopFile));
            else subDirFiles.add(loopFile);
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

            Map<File, Long> oldFileMap = files.stream().collect(Collectors.toMap(f -> f, File::lastModified, Math::max));

            this.files = (ArrayList<File>) recursiveFileScan(host.getDirectory());

            fileMap = files.stream().collect(Collectors.toMap(f -> f, File::lastModified, Math::max));

            if (oldFileMap.size() == files.size() || fileMap.entrySet().iterator().next() == oldFileMap.entrySet().iterator().next()) continue;

            System.out.println("[Info] {CM scan} Starting scan");

            fileHashPairs.clear();

            for (File loopFile : files)
                fileHashPairs.add(new Pair<>(loopFile, returnHashString(Objects.requireNonNull(encodeHash256(loopFile)))));


            recheck = true;
        }
    }

    void sendFiles() {
        for (Pair<File, String> pair : fileHashPairs) {
            sendFunc(pair.Val());
        }
    }

    void sendFunc(String output) {
        for (OutputHost endpoint : endpoints) {
            try (Socket socket = new Socket(endpoint.getHost(), endpoint.getPort())) {
                socket.getOutputStream().write(output.getBytes());

            } catch (IOException e) {
                System.out.println("[ERROR] {send} were not able to initiate connection with: " + endpoint.getHost() + ":" + endpoint.getPort());
            }
        }
    }

    void listen() {
        while (!killSwitch && host != null) {

            /*

                    Listen to connections and messages from those connections

            */

        }
    }

    void mainLoop() {
        while (host != null) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                System.out.println("[Error] {CM main} Failed to scan due to sleep");
            }


            if (!recheck) continue;
            System.out.println("[Error] {CM main} Starting scan");

            /*

                reach out for scan to OutputHosts

            */

            System.gc();
        }
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
