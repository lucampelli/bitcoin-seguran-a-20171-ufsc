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
    
    private int powdif = 4;

    /**
     * Construtor para o programa minerador
     */
    public BCMiner() {
        try {
            //ToDo maneira compartilhavel de hash...
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

            //Por 5 segundos espera respostas de peers ou miners ou do servidor
            
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

                //resposta recebida
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
            ex.printStackTrace();
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
            } else if (proofOfWork(powdif) != -1) {
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

    /**
     * Envia uma mensagem para receber a blockchain do servidor
     * @return A blockchain atualizada do servidor
     */
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

    /**
     * Adiciona uma nova conexão as existentes
     * @param HashID ID do peer a adicionar
     * @param address Endereço da net do peer
     */
    @Override
    public void addPeer(String HashID, InetAddress address) {
        System.out.println("Receiving peer");
        if (peers.containsKey(HashID)) {
            return;
        }
        peers.put(HashID, address);
    }

    /**
     * Recebe um novo bloco para trabalhar
     * @param b o novo bloco
     */
    public void receiveBlockValidationRequest(Block b) {
        
        if (CheckValid(b)) {
            pending.add(b);
            System.out.println(pending.get(pending.size() - 1));
        } else {
            System.out.println("Invalid Block");
        }
        
    }

    /**
     * Faz a proof of work, um quebra cabeça dificil que leva tempo e garante que os blocos demorem a ser trabalhados.
     * @return o resultado do quebra cabeça
     */
    public int proofOfWork(int difficulty) {
        try {
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

    /**
     * Recebe uma mensagem de que um bloco foi validado
     * @param b o bloco validado
     */
    public void receiveBlockValidatedRequest(Block b) {
        chain.addBlock(b);
        if (working.equals(b)) {
            working = null;
        }
        pending.remove(b);
    }

    /**
     * Envia a mensagem de que este minerador validou um bloco
     */
    private void sendBlockValidated() {
        try {
            DatagramPacket packet;
            working.timeStamp(new Date(), chain.Head().Hash());
            byte[] message = (BCTimestampServer.TRANSACTIONCONFIRMEDBROADCAST + ":" + working.toString()).getBytes();

            for (InetAddress a : peers.values()) {
                packet = new DatagramPacket(message, message.length, a, BCTimestampServer.WALLETRECEIVEPORT);
                socket.send(packet);
            }

            for (InetAddress a : miners.values()) {
                packet = new DatagramPacket(message, message.length, a, BCTimestampServer.MINERRECEIVEPORT);
                socket.send(packet);
            }

            packet = new DatagramPacket(message, message.length, server, BCTimestampServer.SERVERRECEIVEPORT);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Confere se um bloco é valido
     * @param b o Bloco
     * @return True se é valido
     */
    private boolean CheckValid(Block b) {
        this.chain = getBlockchainFromServer();
        if(chain.getBlockByHash(b.fundBlock()) == null){
            System.out.println("Inexistant fund block");
            return false;
        }
        if(!chain.getBlockByHash(b.fundBlock()).target().equals(b.ID())){
            System.out.println("Invalid fund block target");
            return false;
        }
        if(chain.getBlockByHash(b.fundBlock()).Value() < b.Value()){
            System.out.println("Invalid fund block value");
            return false;
        }
        return true;
    }

    /**
     * Utilidade para facilitar a comparação de Strings e a variação da dificuldade do POW
     * @param diff  a dificuldade do teste
     * @return Uma string com tantos 0s quanto a dificuldade
     */
    private String POWdiff(int diff){
        String ans = "";
        for (int i = 0; i < diff; i++){
            ans += "0";
        }
        return ans.trim();
    }
    
    /**
     * Retorna o ID do minerador
     * @return ID
     */
    public String ID(){
        return this.hashID;
    }
}
