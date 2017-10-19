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
 * @author luca Thread para os mineradores receberem broadcasts de novos blocos
 * e peers
 */
public class BCMinerSocket implements Runnable {

    DatagramSocket socket;
    BCMiner client;

    public BCMinerSocket(BCMiner c) {
        this.client = c;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(BCTimestampServer.MINERRECEIVEPORT, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);
            while (true) {
                byte[] buf = new byte[15000];
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                socket.receive(p);

                byte[] r;

                String inst = new String(p.getData()).trim().split("|")[0];

                if (inst.equals(BCTimestampServer.DISCOVERY + "")) {
                    System.out.println("Client Received Discovery");
                    client.addPeer(p.getAddress());
                    r = (BCTimestampServer.MINERRESPONSE + "").getBytes();
                    DatagramPacket response = new DatagramPacket(r, r.length, p.getAddress(), p.getPort());
                    socket.send(response);
                }
                if (inst.equals(BCTimestampServer.TRANSACTIONSTARTBROADCAST + "")) {
                    String data = new String(p.getData()).trim().split("|")[1];
                    System.out.println("Miner Received New Transaction");
                    client.receiveBlockValidationRequest(new Block(data));
                }

            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

}
