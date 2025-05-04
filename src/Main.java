import Endpoints.Local.LocalHost;
import helpers.SSHUtil;

public class Main {
    public static void main(String[] args) {


        try {
            SSHUtil.ensureSSHKey();
            LocalHost local = new LocalHost(args);

        } catch (Exception e) {
            System.err.println("[ERROR] {Main} " + e.getMessage());
        }
    }
}
