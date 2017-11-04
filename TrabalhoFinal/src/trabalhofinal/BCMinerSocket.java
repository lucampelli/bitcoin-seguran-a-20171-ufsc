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
 * Thread para os mineradores receberem broadcasts de novos blocos
 * e peers
 */
public class BCMinerSocket implements Runnable {

    MulticastSocket socket;
    BCMiner client;

    /**
     * Este Socket se mantém ativo para o minerador receber as mensagens independente do que estiver fazendo
     * @param c O minerador atrelado a este socket
     */
    public BCMinerSocket(BCMiner c) {
        this.client = c;
    }

    @Override
    public void run() {
        try {
            socket = new MulticastSocket(MINERRECEIVEPORT);
            socket.joinGroup(InetAddress.getByName(MULTICAST_GROUP_ADDRESS));
            while (true) {
                byte[] buf = new byte[15000];
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                try {
                    socket.receive(p);
                } catch(Exception e){
                    
                }
                
                byte[] r;

                String[] data = new String(p.getData()).trim().split(":");

                if (data[0].equals(DISCOVERY + "")) {
                    System.out.println("Client Received Discovery");
                    client.addPeer(data[1], p.getAddress());
                    r = (MINERRESPONSE + ":" + client.ID()).getBytes();
                    DatagramPacket response = new DatagramPacket(r, r.length, p.getAddress(), p.getPort());
                    socket.send(response);
                }
                if (data[0].equals(TRANSACTIONSTARTBROADCAST + "")) {
                    System.out.println("Miner Received New Transaction ");
                    client.receiveBlockValidationRequest(new Block(data[1]));
                }
                if(data[0].equals(TRANSACTIONCONFIRMEDBROADCAST + "")){
                    System.out.println("Miner Received Transaction Confirmation");
                    client.receiveBlockValidatedRequest(new Block(data[1]));
                }
                
                if(data[0].equals(TRANSACTIONDENIEDBROADCAST + "")){
                    System.out.println("Miner Received Note of a Denied Transaction");  //Untested
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
