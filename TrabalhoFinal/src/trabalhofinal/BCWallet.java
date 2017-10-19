/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trabalhofinal;

import java.awt.HeadlessException;
import java.awt.MouseInfo;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author luca Programa Carteira
 */
public class BCWallet extends BCClient {

    private BlockChain chain;
    private ArrayList<Block> myTransactions;

    private InetAddress server;
    private ArrayList<InetAddress> miners;

    private String hashID = "";

    private DatagramSocket socket;

    private float balance;

    private float timeup = 5;

    public BCWallet() {
        try {
            balance = 0;
            hashID = BCTimestampServer.bytesToHex(MessageDigest.getInstance("SHA-512").digest((new Date().getTime() + "" + MouseInfo.getPointerInfo().getLocation().x + "" + MouseInfo.getPointerInfo().getLocation().y).getBytes()));
            System.out.println("Your Wallet ID:" + hashID);

            miners = new ArrayList();
            myTransactions = new ArrayList<>();

            socket = new DatagramSocket();
            socket.setBroadcast(true);

            byte[] data = (BCTimestampServer.DISCOVERY + "").getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), BCTimestampServer.SERVERRECEIVEPORT); // broadcast for peers and server

            DatagramPacket packet1 = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), BCTimestampServer.MINERRECEIVEPORT);
            DatagramPacket packet2 = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), BCTimestampServer.WALLETRECEIVEPORT);

            socket.send(packet);

            socket.send(packet1);// para conseguir testar em um computador só
            socket.send(packet2);

            System.out.println("Looking for Peers: " + socket.getLocalAddress());

            Date date = new Date();

            socket.setSoTimeout(1000);

            long currentTime = date.getTime();

            long startTime = date.getTime();

            //Por 5 segundos espera quantas respostas vierem
            while (currentTime - startTime < timeup * 1000) {
                System.out.println("Waiting for a response");
                byte[] recvBuf = new byte[15000];
                DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
                try {
                    socket.receive(receivePacket);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    currentTime = new Date().getTime();
                    System.out.println("End Cycle:" + ((currentTime - startTime) / 1000.0));
                    continue;
                }

                //Recebemos resposta
                System.out.println("Broadcast response: " + new String(receivePacket.getData()).trim() + " " + receivePacket.getAddress().getHostAddress());

                if (new String(receivePacket.getData()).trim().equals(BCTimestampServer.SERVERDISCOVERYRESPONSE + "")) {
                    server = receivePacket.getAddress();
                    System.out.println("Server acknowledged: " + (receivePacket.getAddress()).getHostAddress());
                }
                if (new String(receivePacket.getData()).trim().equals(BCTimestampServer.MINERRESPONSE + "")) {
                    miners.add(receivePacket.getAddress());
                    System.out.println("Peer acknowledged: " + (receivePacket.getAddress()).getHostAddress());
                }

                currentTime = new Date().getTime();
                System.out.println("End Cycle:" + ((currentTime - startTime) / 1000.0));
            }

        } catch (NoSuchAlgorithmException | HeadlessException | IOException ex) {
        }

        new Thread(new BCClientSocket(this)).start();
        
        String target = "";
        
        createTransaction(target, 1.0f);

    }
    
    public float getBalanceFromChain(){
        //TODO
        return 0;
    }

    public void createTransaction(String targetHash, float value) {
        try {
            //TODO new Block()
            //BroadCast
            byte[] data = (BCTimestampServer.TRANSACTIONSTARTBROADCAST + "").getBytes(); //TODO este é o bloco
            
            DatagramPacket p = new DatagramPacket(data, data.length, server, BCTimestampServer.SERVERRECEIVEPORT);
            socket.send(p);
            
            for (InetAddress a : miners) {
                p = new DatagramPacket(data, data.length, a, BCTimestampServer.SERVERRECEIVEPORT);
                socket.send(p);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void addPeer(InetAddress address) {
        System.out.println("Receiving peer");
        miners.add(address);
        ArrayList<InetAddress> toRemove = new ArrayList();
        for (InetAddress c : miners) {
            try {
                System.out.println(c.toString());
                if (!c.isReachable(1)) {
                    toRemove.add(c);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        for (InetAddress c : toRemove) {
            miners.remove(c);
        }
        toRemove.clear();
    }

}
