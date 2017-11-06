/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trabalhofinal;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import static java.lang.Thread.yield;
import static trabalhofinal.BCTimestampServer.*;

/**
 *
 * Thread para receber broadcasts de descoberta de outros peers
 */
public class BCWalletSocket implements Runnable {

    MulticastSocket socket;
    BCWallet client;

    /**
     * Este socket se mantém ativo para que uma carteira possa receber mensagens
     * independentemente do que estiver fazendo.
     *
     * @param c A carteira a qual este socket esta atrelado
     */
    public BCWalletSocket(BCWallet c) {
        this.client = c;
    }

    @Override
    public void run() {
        try {
            socket = new MulticastSocket(WALLETRECEIVEPORT);
            socket.joinGroup(InetAddress.getByName(MULTICAST_GROUP_ADDRESS));
            socket.setSoTimeout(1000);
            socket.setBroadcast(true);
            while (true) {
                byte[] buf = new byte[15000];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                try {
                    socket.receive(packet);
                } catch (Exception e) {
                }

                if (packet.getData() == null) {
                    continue;
                }

                byte[] r;
                String data[] = new String(packet.getData()).trim().split(":");

                if (data[0].equals(DISCOVERY + "")) {
                    System.out.println("Client Received Discovery");
                    client.addPeer(data[1], packet.getAddress());
                    r = (PEERRESPONSE + ":" + client.ID()).getBytes();
                    DatagramPacket response = new DatagramPacket(r, r.length, packet.getAddress(), packet.getPort());
                    socket.send(response);
                }

                if (data[0].equals(TRANSACTIONCONFIRMEDBROADCAST + "")) {
                    System.out.println("Received Confirm Transaction");
                    client.confirmTransaction(new Block(data[1]));
                }

                if (data[0].equals(TRANSACTIONDENIEDBROADCAST + "")) {
                    System.out.println("Received Note of a Denied Transaction");    //Untested
                    client.receiveDeniedBlock(new Block(data[1]));
                }

                yield();

            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

}
