package helpers;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

public class SSLUtil {
    private static SSLServerSocketFactory serverFactory;
    private static SSLSocketFactory       clientFactory;

    static {
        try {
            String ksPath = System.getProperty("javax.net.ssl.keyStore", "keystore.jks");
            char[] ksPass = System.getProperty("javax.net.ssl.keyStorePassword", "changeit")
                    .toCharArray();

            KeyStore ks = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(ksPath)) {
                ks.load(fis, ksPass);
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, ksPass);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

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