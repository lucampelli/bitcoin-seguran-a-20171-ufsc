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
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;

/**
 *
 * @author luca Programa Minerador
 */
public class BCMiner extends BCClient {

    private BlockChain chain;
    private Block working = null;
    private ArrayList<Block> pending;

    private InetAddress server;
    private ArrayList<InetAddress> peers;
    private ArrayList<InetAddress> miners;

    private String hashID = "";

    private DatagramSocket socket;

    private float timeup = 5;

    public BCMiner() {
        try {

            //hashID = BCTimestampServer.bytesToHex(MessageDigest.getInstance("SHA-512").digest((new Date().getTime() + "" + MouseInfo.getPointerInfo().getLocation().x + "" + MouseInfo.getPointerInfo().getLocation().y).getBytes()));
            hashID = "AC84B32E9D61A4422D1F7AABEF96C326CD2BDD61BFDBF46C2E193EC645B1CA40DD72662FD25B194A1403EDF76B80D18042A220C4DC97966DE718E37F64FFCF9A";
            System.out.println("Your Miner ID:" + hashID);

            peers = new ArrayList();
            miners = new ArrayList();
            pending = new ArrayList();

            socket = new DatagramSocket();
            socket.setBroadcast(true);

            byte[] data = (BCTimestampServer.DISCOVERY + "").getBytes();
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

                if (new String(receivePacket.getData()).trim().equals(BCTimestampServer.SERVERDISCOVERYRESPONSE + "")) {
                    server = receivePacket.getAddress();
                    System.out.println("Server acknowledged: " + (receivePacket.getAddress()).getHostAddress());
                }
                if (new String(receivePacket.getData()).trim().equals(BCTimestampServer.PEERRESPONSE + "")) {
                    peers.add(receivePacket.getAddress());
                    System.out.println("Peer acknowledged: " + (receivePacket.getAddress()).getHostAddress());
                }
                if (new String(receivePacket.getData()).trim().equals(BCTimestampServer.MINERRESPONSE + "")) {
                    miners.add(receivePacket.getAddress());
                    System.out.println("Miner acknowledged: " + (receivePacket.getAddress()).getHostAddress());
                }

                currentTime = new Date().getTime();
                System.out.println("End Cycle:" + ((currentTime - startTime) / 1000.0));
            }

        } catch (Exception ex) {
        }

        chain = getBlockchainFromServer();

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
            }
            yield();
        }

    }

    private BlockChain getBlockchainFromServer() {
        //TODO mudar para pegar do server mesmo
        return new BlockChain();
    }

    @Override
    public void addPeer(InetAddress address) {
        System.out.println("Receiving peer");
        if (peers.contains(address)) {
            return;
        }
        peers.add(address);
        ArrayList<InetAddress> toRemove = new ArrayList();
        for (InetAddress c : peers) {
            try {
                System.out.println(c.toString());
                if (!c.isReachable(100)) {
                    toRemove.add(c);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        for (InetAddress c : toRemove) {
            peers.remove(c);
        }
        toRemove.clear();
    }

    public void receiveBlockValidationRequest(Block b) {
        if (CheckValid(b)) {
            pending.add(b);
        } else {
            
        }
        System.out.println(pending.get(pending.size() - 1));
    }

    public int proofOfWork() {
        try {
            int difficulty = 3;
            String block = working.Hash();
            String hash = "";
            int nonce = 0;
            while (!hash.startsWith("000") && working != null) {
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

            for (InetAddress a : peers) {
                packet = new DatagramPacket(message, message.length, a, BCTimestampServer.WALLETRECEIVEPORT);
                socket.send(packet);
            }

            for (InetAddress a : miners) {
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
        if(chain.getBlockByHash(b.fundBlock()).Value() < b.Value()){
            return false;
        }
        if(!b.previousBlock().equals(chain.getBlockByHash(b.previousBlock()).Hash())){
            return false;
        }
        return true;
    }

}
