/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trabalhofinal;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 *
 * @author luca
 * Thread para receber broadcasts de descoberta de outros peers
 */
public class BCClientSocket implements Runnable {

    DatagramSocket socket;
    BCClient client;
    
    public BCClientSocket(BCClient c){
        this.client = c;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(BCTimestampServer.SERVERRECEIVEPORT,InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);
            while (true) {
                byte[] buf = new byte[15000];
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                socket.receive(p);

                byte[] r;
                //Change
                if (new String(p.getData()).trim().equals(BCTimestampServer.DISCOVERY + "")) {
                    System.out.println("Client Received Discovery");
                    client.addPeer(p.getAddress());
                    r = (BCTimestampServer.PEERRESPONSE + "").getBytes();
                    DatagramPacket response = new DatagramPacket(r, r.length, p.getAddress(), p.getPort());
                    socket.send(response);
                }

            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

}
