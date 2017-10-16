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
 */
public class BCTimestampServer implements Runnable {
    
    public final static short SERVERRECEIVEPORT = 8888;
    
    public final static short DISCOVERY = 1;
    public final static short ASKFORCHAIN = 2;
    public final static short TRANSACTIONSTARTBROADCAST = 3;
    public final static short TRANSACTIONCONFIRMEDBROADCAST = 4;
    public final static short TRANSACTIONDENIEDBROADCAST = 5;
    
    public final static short SERVERDISCOVERYRESPONSE = 10;
    public final static short CHAINRESPONSE = 20;
    public final static short PEERRESPONSE = 30;

    public static BCTimestampServer INSTANCE;

    private DatagramSocket socket;

    public static BCTimestampServer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BCTimestampServer();
        }
        return INSTANCE;
    }

    BCTimestampServer() {
        try {
            socket = new DatagramSocket(8888, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);
            
        } catch (SocketException | UnknownHostException e){
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println("Server address:" + socket.getLocalAddress().getHostAddress());
        while(true){
            try {
                System.out.println(getClass().getName() + " is ready to receive requests");
                
                byte[] recBuf = new byte[15000];
                DatagramPacket packet = new DatagramPacket(recBuf, recBuf.length);
                socket.receive(packet);
                
                //packet received
                
                System.out.println("Received Packet from:" + packet.getAddress().getHostAddress());
                System.out.println("Saying:" + new String(packet.getData()));
                
                Thread response = new Thread(new BCServerHandler(packet));
                response.start();
                
                System.out.println("Sent");
                
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            
            
            
        }
    }

}
