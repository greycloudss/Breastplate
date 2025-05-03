package helpers;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.security.KeyStore;

public class SSLUtil {
    private static SSLServerSocketFactory serverFactory;
    private static SSLSocketFactory       clientFactory;

    static {
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream("keystore.jks"), "changeit".toCharArray());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, "changeit".toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            serverFactory = ctx.getServerSocketFactory();
            clientFactory = ctx.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static SSLServerSocketFactory serverFactory() {
        return serverFactory;
    }

    public static SSLSocketFactory clientFactory() {
        return clientFactory;
    }
}