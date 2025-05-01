import Endpoints.Local.ConnectionManager;
import Endpoints.Local.LocalHost;
import Endpoints.Node.OutputHost;

import java.io.File;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        try {
            new LocalHost(args);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
