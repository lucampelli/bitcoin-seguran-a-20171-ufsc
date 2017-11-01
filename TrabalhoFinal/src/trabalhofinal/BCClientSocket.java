/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trabalhofinal;

import static java.lang.Thread.yield;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 *
 * @author luca Thread para receber broadcasts de descoberta de outros peers
 */
public class BCClientSocket implements Runnable {

    DatagramSocket socket;
    BCWallet client;

    /**
     * Este socket se mantém ativo para que uma carteira possa receber mensagens independentemente do que estiver fazendo.
     * @param c A carteira a qual este socket esta atrelado
     */
    public BCClientSocket(BCWallet c) {
        this.client = c;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(BCTimestampServer.WALLETRECEIVEPORT, InetAddress.getByName("0.0.0.0"));
            socket.setSoTimeout(1000);
            socket.setBroadcast(true);
            while (true) {
                byte[] buf = new byte[15000];
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                try {
                    socket.receive(p);
                } catch(Exception e){
                    
                }
                byte[] r;
                //Change
                
                
                if(p.getData() == null){
                    continue;
                }

                String data[] = new String(p.getData()).trim().split(":");

                if (data[0].equals(BCTimestampServer.DISCOVERY + "")) {
                    System.out.println("Client Received Discovery");
                    client.addPeer(data[1], p.getAddress());
                    r = (BCTimestampServer.PEERRESPONSE + ":" + client.ID()).getBytes();
                    DatagramPacket response = new DatagramPacket(r, r.length, p.getAddress(), p.getPort());
                    socket.send(response);
                }

                if (data[0].equals(BCTimestampServer.TRANSACTIONCONFIRMEDBROADCAST + "")) {
                    System.out.println("Received Confirm Transaction");
                    client.confirmTransaction(new Block(data[1]));
                }
                
                yield();

            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

}
