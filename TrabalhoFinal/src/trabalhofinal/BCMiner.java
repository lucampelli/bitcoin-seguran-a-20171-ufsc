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

/**
 *
 * @author luca
 * Programa Minerador
 */
public class BCMiner extends BCClient{

    private BlockChain chain;
    private Block working;
    private ArrayList<Block> pending;

    private InetAddress server;
    private ArrayList<InetAddress> peers;

    private String hashID = "";

    private DatagramSocket socket;

    private float timeup = 5;

    public BCMiner() {
        try {
            
            hashID = BCTimestampServer.bytesToHex(MessageDigest.getInstance("SHA-512").digest((new Date().getTime() + "" + MouseInfo.getPointerInfo().getLocation().x + "" + MouseInfo.getPointerInfo().getLocation().y).getBytes()));
            System.out.println("Your Miner ID:" + hashID);
            
            peers = new ArrayList();
            pending = new ArrayList();

            socket = new DatagramSocket();
            socket.setBroadcast(true);

            byte[] data = (BCTimestampServer.DISCOVERY + "").getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), BCTimestampServer.SERVERRECEIVEPORT); // broadcast for peers and server
            
            DatagramPacket packet1 = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), BCTimestampServer.SERVERRECEIVEPORT + 1);// para conseguir testar em um computador só
            DatagramPacket packet2 = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), BCTimestampServer.SERVERRECEIVEPORT + 2);
            
            socket.send(packet);
            
            socket.send(packet1);// para conseguir testar em um computador só
            socket.send(packet2);

            System.out.println("Looking for Peers: " + socket.getLocalAddress());

            Date date = new Date();

            socket.setSoTimeout(1000);

            long currentTime = date.getTime();

            long startTime = date.getTime();

            while (currentTime - startTime < timeup * 1000) {
                System.out.println("Waiting for a response");
                byte[] recvBuf = new byte[15000];
                DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
                try {
                    socket.receive(receivePacket);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    currentTime = new Date().getTime();
                    System.out.println("End Cycle:" + ((currentTime - startTime) / 1000));
                    continue;
                }

                //We have a response
                System.out.println("Broadcast response: " + new String(receivePacket.getData()).trim() + " " + receivePacket.getAddress().getHostAddress());

                if (new String(receivePacket.getData()).trim().equals(BCTimestampServer.SERVERDISCOVERYRESPONSE + "")) {
                    server = receivePacket.getAddress();
                    System.out.println("Server acknowledged: " + (receivePacket.getAddress()).getHostAddress());
                }
                if (new String(receivePacket.getData()).trim().equals(BCTimestampServer.PEERRESPONSE + "")) {
                    peers.add(receivePacket.getAddress());
                    System.out.println("Peer acknowledged: " + (receivePacket.getAddress()).getHostAddress());
                }

                currentTime = new Date().getTime();
                System.out.println("End Cycle:" + ((currentTime - startTime) / 1000));
            }

        } catch (NoSuchAlgorithmException | HeadlessException | IOException ex) {
        }

        new Thread(new BCMinerSocket(this)).start();
        
        
    }

    @Override
    public void addPeer(InetAddress address) {
        System.out.println("Receiving peer");
        peers.add(address);
        ArrayList<InetAddress> toRemove = new ArrayList();
        for(InetAddress c : peers){
            try {
                System.out.println(c.toString());
                if(!c.isReachable(1)){
                    toRemove.add(c);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        for(InetAddress c : toRemove){
            peers.remove(c);
        }
        toRemove.clear();
    }
    
    public void reciveBlockValidationRequest(Block b){
        pending.add(b);
    }
    
    public void proofOfWork(){
        //Take some time
    }
    
    public void receiveBlockValidatedRequest(Block b){
        chain.addBlock(b);
        if(working.equals(b)){
            working = null;
        }
        pending.remove(b);
    }

}