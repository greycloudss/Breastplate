package Endpoints.Local;

import Endpoints.Node.OutputHost;

import java.util.ArrayList;

public class SFTP {
    Thread sendThread, receiveThread;
    LocalHost host;
    volatile ArrayList<Integer> endpointSendIndexes, endpointReceiveIndexes;

    boolean send, receive;

    SFTP(LocalHost host) {
        this.host = host;
        sendThread = new Thread(this::sendFiles);
        receiveThread = new Thread(this::receiveFiles);

        sendThread.start();
        receiveThread.start();
    }



    public void sendFiles() {
        while (true) {
            if (!send) { return; }
            try {

            } catch (Exception e) {
                System.out.println("[ERROR] {SFTP send} unable to send files");
            }
            send = false;
        }
    }

    public boolean checkEndpointPerms() {

        // basically check if the endpoint is allowed to be
        // sending data to us to avoid hosts sending possibly spicy content

        return false;
    }

    public void receiveFiles() {
        while (true) {
            if (!receive) { return; }
            try {

            } catch (Exception e) {
                System.out.println("[ERROR] {SFTP receive} unable to send files");
            }
            receive = false;
            endpointReceiveIndexes.clear();
        }
    }
}
