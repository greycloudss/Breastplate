import Endpoints.Local.LocalHost;
import helpers.SSHUtil;

public class Main {
    public static void main(String[] args) {
        String sshUser = System.getProperty("user.name");
        String sshPass = "";

        for (int i = 0; i < args.length - 1; i++) {
            switch (args[i]) {
                case "-user" -> sshUser = args[i+1];
                case "-password" -> sshPass = args[i+1];
            }
        }

        try {
            SSHUtil.ensureSSHKey();
            LocalHost local = new LocalHost(args);
            local.getConnectionManager().setSshUser(sshUser);
            local.getConnectionManager().setSshPass(sshPass);
        } catch (Exception e) {
            System.err.println("[ERROR] {Main} " + e.getMessage());
        }
    }
}
