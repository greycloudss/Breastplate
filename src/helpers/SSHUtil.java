package helpers;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class SSHUtil {
    public static void ensureSSHKey() throws IOException, InterruptedException {
        Path home   = Path.of(System.getProperty("user.home"));
        Path sshDir = home.resolve(".ssh");
        Path priv   = sshDir.resolve("id_ed25519");
        Path pub    = sshDir.resolve("id_ed25519.pub");

        if (Files.exists(priv) && Files.exists(pub)) return;

        if (!Files.exists(sshDir)) {
            Files.createDirectories(sshDir);
            String os = System.getProperty("os.name").toLowerCase();
            if (!os.contains("win")) {
                try {
                    Files.setPosixFilePermissions(sshDir, Set.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE,
                            PosixFilePermission.OWNER_EXECUTE
                    ));
                } catch (UnsupportedOperationException ignored) { }
            }
        }

        Process p = new ProcessBuilder(
                "ssh-keygen", "-t", "ed25519",
                "-f", priv.toString(),
                "-N", ""
        ).inheritIO().start();

        if (p.waitFor(30, TimeUnit.SECONDS)) {
            if (p.exitValue() != 0) {
                throw new IOException("ssh-keygen failed: exit " + p.exitValue());
            }
        } else {
            p.destroyForcibly();
            throw new IOException("ssh-keygen timed out");
        }
    }
}
