/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trabalhofinal;

import java.io.IOException;
import java.net.*;

/**
 *
 * Servidor timesstamp que mantém uma cópia atualizada da blockchain
 */
public class BCTimestampServer implements Runnable {

    
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    /**
     * Endereço IP usado para comunicação Multicast
     */
    public static final String MULTICAST_GROUP_ADDRESS = "230.0.0.1";

    /**
     * Definição das portas onde rodará cada processo
     */
    public final static short SERVERRECEIVEPORT = 8888;
    public final static short SERVERSENDPORT = 8889;
    public final static short MINERRECEIVEPORT = 8890;
    public final static short WALLETRECEIVEPORT = 8891;

    /**
     * Identificadores das mensagens
     */
    public final static short DISCOVERY = 1;
    public final static short ASKFORCHAIN = 2;
    public final static short TRANSACTIONSTARTBROADCAST = 3;
    public final static short TRANSACTIONCONFIRMEDBROADCAST = 4;
    public final static short TRANSACTIONDENIEDBROADCAST = 5;

    /**
     * Indetificador das repostas
     */
    public final static short SERVERDISCOVERYRESPONSE = 10;
    public final static short CHAINRESPONSE = 20;
    public final static short PEERRESPONSE = 30;
    public final static short MINERRESPONSE = 40;
    
    public static int matrixCount = 0;

    public static BCTimestampServer INSTANCE;

    public BlockChain chain;
    
    private MulticastSocket socketRec;
    private DatagramSocket socketSend;

    public static BCTimestampServer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BCTimestampServer();
        }
        return INSTANCE;
    }

    private BCTimestampServer() {
        try {
            socketRec = new MulticastSocket(SERVERRECEIVEPORT);
            socketRec.joinGroup(InetAddress.getByName(MULTICAST_GROUP_ADDRESS));
            socketSend = new DatagramSocket(SERVERSENDPORT);
            chain = new BlockChain();
        } catch (IOException e) {
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

                Thread response = new Thread(new BCServerHandler(packet, socketSend, chain));
                response.start();

                System.out.println("current chain");
                System.out.println(chain.toStringLines());

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
