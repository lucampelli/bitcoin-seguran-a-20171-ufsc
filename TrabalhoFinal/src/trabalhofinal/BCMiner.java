/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trabalhofinal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import static java.lang.Thread.yield;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author luca Programa Minerador
 */
public class BCMiner extends BCClient {

    private BlockChain chain;
    private Block working = null;
    private List<Block> pending;

    private InetAddress server;
    private HashMap<String, InetAddress> peers;
    private HashMap<String, InetAddress> miners;

    private String hashID = "";

    private DatagramSocket socket;

    private float timeup = 5;

    public BCMiner() {
        try {

            //hashID = BCTimestampServer.bytesToHex(MessageDigest.getInstance("SHA-512").digest((new Date().getTime() + "" + MouseInfo.getPointerInfo().getLocation().x + "" + MouseInfo.getPointerInfo().getLocation().y).getBytes()));
            hashID = "AC84B32E9D61A4422D1F7AABEF96C326CD2BDD61BFDBF46C2E193EC645B1CA40DD72662FD25B194A1403EDF76B80D18042A220C4DC97966DE718E37F64FFCF9A";
            System.out.println("Your Miner ID:" + hashID);

            peers = new HashMap();
            miners = new HashMap();
            pending = new ArrayList<>();

            socket = new DatagramSocket();
            socket.setBroadcast(true);

            byte[] data = (BCTimestampServer.DISCOVERY + ":" + hashID).getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), BCTimestampServer.SERVERRECEIVEPORT); // broadcast for peers and server
            DatagramPacket packet1 = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), BCTimestampServer.MINERRECEIVEPORT);// para conseguir testar em um computador só
            DatagramPacket packet2 = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), BCTimestampServer.WALLETRECEIVEPORT);

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

                String recData = new String(receivePacket.getData()).trim();
                
                if (recData.split(":")[0].equals(BCTimestampServer.SERVERDISCOVERYRESPONSE + "")) {
                    server = receivePacket.getAddress();
                    System.out.println("Server acknowledged: " + (receivePacket.getAddress()).getHostAddress());
                }
                if (recData.split(":")[0].equals(BCTimestampServer.PEERRESPONSE + "")) {
                    peers.put(recData.split(":")[1],receivePacket.getAddress());
                    System.out.println("Peer acknowledged: " + (receivePacket.getAddress()).getHostAddress());
                }
                if (recData.split(":")[0].equals(BCTimestampServer.MINERRESPONSE + "")) {
                    miners.put(recData.split(":")[1],receivePacket.getAddress());
                    System.out.println("Miner acknowledged: " + (receivePacket.getAddress()).getHostAddress());
                }

                currentTime = new Date().getTime();
                System.out.println("End Cycle:" + ((currentTime - startTime) / 1000.0));
            }

        } catch (Exception ex) {
        }

        chain = getBlockchainFromServer();
        System.out.println(chain.toStringLines());

        new Thread(new BCMinerSocket(this)).start();

        while (true) {
            if (working == null) {
                if (!pending.isEmpty()) {
                    System.out.println("New work");
                    working = pending.get(0);
                }
            } else if (proofOfWork() != -1) {
                System.out.println("POW success");
                sendBlockValidated();
                chain.addBlock(working);
                pending.remove(working);
                working = null;
                System.out.println(chain.toStringLines());
            }
            yield();
        }

    }

    private BlockChain getBlockchainFromServer() {
        byte[] data = (BCTimestampServer.ASKFORCHAIN + "").getBytes();
        if(server == null){
            return new BlockChain();
        }
        byte[] recvBuf = new byte[50 * 1024];
        try {
            socket.setSoTimeout(5);
            socket.send(new DatagramPacket(data, data.length,server,BCTimestampServer.SERVERRECEIVEPORT));
            DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
            socket.receive(packet);
            
            ByteArrayInputStream byteStream = new ByteArrayInputStream(packet.getData());
            ObjectInputStream objectIStream = new ObjectInputStream(new BufferedInputStream(byteStream));
            Object o = objectIStream.readObject();
            objectIStream.close();
            return (BlockChain) o;
        } catch (Exception e) {
        }
        
        
        return new BlockChain();
    }

    @Override
    public void addPeer(String HashID, InetAddress address) {
        System.out.println("Receiving peer");
        if (peers.containsKey(HashID)) {
            return;
        }
        peers.put(HashID, address);
    }

    public void receiveBlockValidationRequest(Block b) {
        if (CheckValid(b)) {
            pending.add(b);
            System.out.println(pending.get(pending.size() - 1));
        } else {
            System.out.println("Bloco não valido");
        }
        
    }

    public int proofOfWork() {
        try {
            int difficulty = 4;
            String block = working.Hash();
            String hash = "";
            int nonce = 0;
            while (!hash.startsWith(POWdiff(difficulty)) && working != null) {
                hash = BCTimestampServer.bytesToHex(MessageDigest.getInstance("SHA-512").digest((block + "" + nonce).getBytes()));
                nonce++;
                System.out.println(nonce);
            }
            if (working != null) {
                System.out.println(hash.substring(0, 5));
                return nonce;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void receiveBlockValidatedRequest(Block b) {
        chain.addBlock(b);
        if (working.equals(b)) {
            working = null;
        }
        pending.remove(b);
    }

    private void sendBlockValidated() {
        try {
            DatagramPacket packet;
            byte[] message = (BCTimestampServer.TRANSACTIONCONFIRMEDBROADCAST + ":" + working.Hash()).getBytes();

            for (InetAddress a : peers.values()) {
                packet = new DatagramPacket(message, message.length, a, BCTimestampServer.WALLETRECEIVEPORT);
                socket.send(packet);
            }

            for (InetAddress a : miners.values()) {
                packet = new DatagramPacket(message, message.length, a, BCTimestampServer.MINERRECEIVEPORT);
                socket.send(packet);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean CheckValid(Block b) {
        if(chain.getBlockByHash(b.fundBlock()) == null){
            return false;
        }
        if(!chain.getBlockByHash(b.fundBlock()).target().equals(b.ID())){
            return false;
        }
        if(chain.getBlockByHash(b.fundBlock()).Value() < b.Value()){
            return false;
        }
        if(!b.previousBlock().equals(chain.Head().Hash())){
            return false;
        }
        return true;
    }

    public String POWdiff(int diff){
        String ans = "";
        for (int i = 0; i < diff; i++){
            ans += "0";
        }
        return ans.trim();
    }
}
