package Endpoints.Local;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SFTP {
    private static final ExecutorService executor = Executors.newCachedThreadPool();


    public static void uploadFile(String user, String pass, String host, int port,
                                  Path local, String remoteRel) {
        executor.submit(() -> runSftp(user, pass, host, port,
                "put", local.toString(), remoteRel.replace('\\','/')));
    }

    public static void downloadFile(String user, String pass, String host, int port,
                                    Path local, String remoteRel) {
        executor.submit(() -> runSftp(user, pass, host, port,
                "get", remoteRel.replace('\\','/'), local.toString()));
    }



    private static void runSftp(String user, String password,
                                String host, int ignoredPort,
                                String cmd, String src, String dst) {

        int sshPort = 22;
        boolean win  = System.getProperty("os.name").toLowerCase().contains("win");
        boolean useP = !password.isEmpty() && !win;
        String khf   = win ? "NUL" : "/dev/null";

        List<String> shell = new ArrayList<>();
        if (useP) { shell.add("sshpass"); shell.add("-p"); shell.add(password); }
        shell.add("sftp");
        shell.add("-q");
        shell.add("-oBatchMode=no");
        shell.add("-oStrictHostKeyChecking=no");
        shell.add("-oUserKnownHostsFile=" + khf);
        shell.add("-oPort=" + sshPort);
        shell.add(user + "@" + host);

        System.out.println("[DEBUG] SFTP cmd: " + String.join(" ", shell));

        ProcessBuilder pb = new ProcessBuilder(shell).redirectErrorStream(true);

        try {
            Process p = pb.start();
            try (var wr = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
                 var rd = new BufferedReader(new InputStreamReader(p.getInputStream()))) {

                if ("put".equals(cmd)) {
                    int cut = dst.lastIndexOf('/');
                    if (cut > 0) {
                        String dir = dst.substring(0, cut).replaceFirst("^/+", "");
                        wr.write("mkdir " + dir + "\n");
                        wr.write("cd "   + dir + "\n");
                    }
                    String remoteName = dst.substring(dst.lastIndexOf('/') + 1);
                    wr.write("put " + src + " " + remoteName + "\n");
                } else {
                    Path loc = Paths.get(dst).toAbsolutePath();
                    Files.createDirectories(loc.getParent());
                    wr.write("get " + dst.replaceFirst("^/+", "") + " " + dst + "\n");
                }

                wr.write("bye\n"); wr.flush();

                String line;
                while ((line = rd.readLine()) != null)
                    System.out.println("[INFO] {SFTP} " + line);
            }

            int code = p.waitFor();
            if (code != 0) System.err.println("[ERROR] {SFTP} exited with " + code);

        } catch (IOException | InterruptedException e) {
            System.err.println("[ERROR] {SFTP} " + e.getMessage());
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