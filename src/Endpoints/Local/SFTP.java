package Endpoints.Local;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SFTP {
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static void downloadFile(String user, String password,
                                    String host, int port,
                                    Path localPath, String remotePath) {
        executor.submit(() ->
                runSftp(user, password, host, port, "get", remotePath, localPath.toString())
        );
    }

    public static void uploadFile(String user, String password,
                                  String host, int port,
                                  Path localPath, String remotePath) {
        executor.submit(() ->
                runSftp(user, password, host, port, "put", localPath.toString(), remotePath)
        );
    }

    public static void replaceFile(String user, String password,
                                   String host, int port,
                                   Path localPath, String remotePath) {
        executor.submit(() ->
                runSftp(user, password, host, port, "get", remotePath, localPath.toString())
        );
    }

    private static void runSftp(String user, String password,
                                String host, int port,
                                String command, String source, String destination) {
        List<String> cmd = new ArrayList<>();
        if (!password.isEmpty()) {
            cmd.add("sshpass"); cmd.add("-p"); cmd.add(password);
        }
        cmd.add("sftp"); cmd.add("-P"); cmd.add(String.valueOf(port));
        cmd.add(user + "@" + host);

        ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);

        try {
            Process proc = pb.start();
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
                 BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {

                if ("put".equals(command)) {
                    String dir = destination.substring(0, destination.lastIndexOf('/'));
                    writer.write("mkdir " + dir + "\n");
                }

                writer.write(command + " " + source + " " + destination + "\n");
                writer.write("bye\n");
                writer.flush();

                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[INFO] {SFTP} " + line);
                }
            }

            int code = proc.waitFor();
            if (code != 0) {
                System.err.println("[ERROR] {SFTP} exited with code " + code);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("[ERROR] {SFTP} error: " + e.getMessage());
        }
    }


    public static void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ignored) {
            executor.shutdownNow();
        }
    }
}