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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 *
 * @author luca Programa Carteira
 */
public class BCWallet extends BCClient {

    private BlockChain chain;
    private ArrayList<Block> myTransactions;
    private ArrayList<Block> unconfirmedTransactions;
    
    private InetAddress server;
    private HashMap<String,InetAddress> miners;

    private String hashID = "";

    private DatagramSocket socket;

    private float balance;

    private float timeup = 5;

    public BCWallet() {
        try {
            balance = 0;
            //hashID = BCTimestampServer.bytesToHex(MessageDigest.getInstance("SHA-512").digest((new Date().getTime() + "" + MouseInfo.getPointerInfo().getLocation().x + "" + MouseInfo.getPointerInfo().getLocation().y).getBytes()));
            hashID = "AC84B32E9D61A4422D1F7AABEF96C326CD2BDD61BFDBF46C2E193EC645B1CA40DD72662FD25B194A1403EDF76B80D18042A220C4DC97966DE718E37F64FFCF9B";
            
            System.out.println("Your Wallet ID:" + hashID);

            miners = new HashMap();
            myTransactions = new ArrayList<>();
            unconfirmedTransactions = new ArrayList<>();

            socket = new DatagramSocket();
            socket.setBroadcast(true);

            byte[] data = (BCTimestampServer.DISCOVERY + ":" + hashID).getBytes();
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
                    miners.put("",receivePacket.getAddress());  //TODO get correct Hash ID
                    System.out.println("Miner acknowledged: " + (receivePacket.getAddress()).getHostAddress());
                }

                currentTime = new Date().getTime();
                System.out.println("End Cycle:" + ((currentTime - startTime) / 1000.0));
            }

        } catch (Exception ex) {
        }

        new Thread(new BCClientSocket(this)).start();
        
        String target = "AC84B32E9D61A4422D1F7AABEF96C326CD2BDD61BFDBF46C2E193EC645B1CA40DD72662FD25B194A1403EDF76B80D18042A220C4DC97966DE718E37F64FFCF9A";
        
        chain = getBlockchainFromServer();
        
        createTransaction(target, 1.0f);

    }
    
    private float getBalance(){
        float sum = 0;
        for(Block b : myTransactions){
            sum += b.Value();
        }
        return sum;
    }
    
   private Block SearchValidFundBlock(float value){
        for(Block b : myTransactions){
            if(b.Value() >= value){
                return b;
            }
        }
        return chain.Head();
    }
    

    private void createTransaction(String targetHash, float value) {
        try {
            
            Block b = new Block(this.hashID, targetHash, new Date(), chain.Head().Hash(),
                    SearchValidFundBlock(value),value);
            
            unconfirmedTransactions.add(b);
            
            //BroadCast
            byte[] data = (BCTimestampServer.TRANSACTIONSTARTBROADCAST + ":" + b.toString()).getBytes();
            
            DatagramPacket p;
            
            for (InetAddress a : miners.values()) {
                p = new DatagramPacket(data, data.length, a, BCTimestampServer.MINERRECEIVEPORT);
                System.out.println("New Block");
                socket.send(p);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void addPeer(String HashID, InetAddress address) {
        System.out.println("Receiving peer");
        if(miners.containsKey(HashID)){
            return;
        }
        miners.put(HashID,address);
    }

    private BlockChain getBlockchainFromServer() {
        //TODO mudar para pegar do server mesmo
        return new BlockChain();
    }
    
    public void confirmTransaction(String blockHash){
        Block conf = null;
        for(Block b : unconfirmedTransactions){
            if(b.Hash().equals(blockHash)){
                conf = b;
                myTransactions.add(b);
                break;
            }
        }
        unconfirmedTransactions.remove(conf);
        System.out.println("Transaction Confirmed");
        System.out.println(conf.toStringLines());
    }

}
