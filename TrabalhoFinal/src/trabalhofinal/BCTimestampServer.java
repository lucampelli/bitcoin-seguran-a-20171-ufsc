/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trabalhofinal;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 *
 * @author luca
 * Servidor timesstamp que mantém uma cópia atualizada da blockchain
 */
public class BCTimestampServer implements Runnable {

    
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    
    public final static short SERVERRECEIVEPORT = 8888;
    public final static short SERVERSENDPORT = 8889;
    public final static short MINERRECEIVEPORT = 8890;
    public final static short WALLETRECEIVEPORT = 8891;

    public final static short DISCOVERY = 1;
    public final static short ASKFORCHAIN = 2;
    public final static short TRANSACTIONSTARTBROADCAST = 3;
    public final static short TRANSACTIONCONFIRMEDBROADCAST = 4;
    public final static short TRANSACTIONDENIEDBROADCAST = 5;

    public final static short SERVERDISCOVERYRESPONSE = 10;
    public final static short CHAINRESPONSE = 20;
    public final static short PEERRESPONSE = 30;
    public final static short MINERRESPONSE = 40;

    public static BCTimestampServer INSTANCE;

    private DatagramSocket socketRec;
    private DatagramSocket socketSend;

    public static BCTimestampServer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BCTimestampServer();
        }
        return INSTANCE;
    }

    BCTimestampServer() {
        try {
            socketRec = new DatagramSocket(SERVERRECEIVEPORT, InetAddress.getByName("0.0.0.0"));
            socketRec.setBroadcast(true);
            socketSend = new DatagramSocket(SERVERSENDPORT, InetAddress.getByName("0.0.0.0"));

        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println("Server address:" + socketRec.getLocalAddress().getHostAddress() + ":" + socketRec.getLocalPort());
        while (true) {
            try {
                System.out.println(getClass().getName() + " is ready to receive requests");

                byte[] recBuf = new byte[15000];
                DatagramPacket packet = new DatagramPacket(recBuf, recBuf.length);
                socketRec.receive(packet);

                //packet received
                System.out.println("Received Packet from: " + packet.getAddress().getHostAddress() + ":" + packet.getPort());
                System.out.println("Saying:" + new String(packet.getData()));

                Thread response = new Thread(new BCServerHandler(packet, socketSend));
                response.start();

                System.out.println("Sent");

            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

}
