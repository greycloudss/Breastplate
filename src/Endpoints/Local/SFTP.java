package Endpoints.Local;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
                                String host, int ignoredPort,
                                String command, String source, String destination) {
        int sshPort = 22;
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        boolean useSshpass = !password.isEmpty() && !isWindows;
        String knownHostsFile = isWindows ? "NUL" : "/dev/null";

        List<String> cmd = new ArrayList<>();
        if (useSshpass) {
            cmd.add("sshpass");
            cmd.add("-p");
            cmd.add(password);
        }
        cmd.add("sftp");
        cmd.add("-q");
        cmd.add("-oBatchMode=no");
        cmd.add("-oStrictHostKeyChecking=no");
        cmd.add("-oUserKnownHostsFile=" + knownHostsFile);
        cmd.add("-oPort=" + sshPort);
        cmd.add(user + "@" + host);

        System.out.println("[DEBUG] SFTP cmd: " + String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);

        try {
            Process proc = pb.start();
            try (var writer = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
                 var reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {

                String remoteDir = destination.contains("/")
                        ? destination.substring(0, destination.lastIndexOf('/'))
                        : "";
                if (!remoteDir.isEmpty()) {
                    writer.write("mkdir " + remoteDir + "\n");
                    writer.write("cd "    + remoteDir + "\n");
                }
                String baseName = Paths.get(command.equals("put") ? source : destination)
                        .getFileName().toString();
                if ("put".equals(command)) {
                    writer.write("put " + source + " " + baseName + "\n");
                } else {
                    writer.write("get " + baseName + " " + destination + "\n");
                }
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