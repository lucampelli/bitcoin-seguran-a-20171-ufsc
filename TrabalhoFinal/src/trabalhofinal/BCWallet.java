/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trabalhofinal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
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
    private HashMap<String, InetAddress> miners;

    private String hashID = "";

    private DatagramSocket socket;

    private float balance;

    private float timeup = 5;

    /**
     * Carteira que mostra o balanco e cria novas transações
     */
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

                String recData = new String(receivePacket.getData()).trim();

                if (recData.split(":")[0].equals(BCTimestampServer.SERVERDISCOVERYRESPONSE + "")) {
                    server = receivePacket.getAddress();
                    System.out.println("Server acknowledged: " + (receivePacket.getAddress()).getHostAddress());
                }
                if (recData.split(":")[0].equals(BCTimestampServer.MINERRESPONSE + "")) {
                    miners.put(recData.split(":")[1], receivePacket.getAddress());
                    System.out.println("Miner acknowledged: " + (receivePacket.getAddress()).getHostAddress());
                }

                currentTime = new Date().getTime();
                System.out.println("End Cycle:" + ((currentTime - startTime) / 1000.0));
            }

        } catch (Exception ex) {
        }

        chain = getBlockchainFromServer();
        System.out.println(chain.toStringLines());

        new Thread(new BCClientSocket(this)).start();

        String target = "AC84B32E9D61A4422D1F7AABEF96C326CD2BDD61BFDBF46C2E193EC645B1CA40DD72662FD25B194A1403EDF76B80D18042A220C4DC97966DE718E37F64FFCF9A";

        System.out.println("Your Balance: " + getBalance());

        createTransaction(target, 1.0f);

    }

    /**
     * recupera o balanço desta carteira analisando a blockchain
     *
     * @return o valor do balanco
     */
    private float getBalance() {
        float sum = 0;
        for (Block b : chain.getAllBlocksToUser(hashID)) {
            sum += b.Value();
        }
        for (Block b : chain.getAllBlocksFromUser(hashID)) {
            sum += b.change() - b.Value();
        }
        return sum;
    }

    /**
     * Procura na corrente um bloco de fundos válido para que se utilize na
     * transação TODO: Encontrar todos os blocos de fundos que satisfaçam o
     * valor
     *
     * @param value Valor necessário à transação
     * @return Bloco encontrado ou null se não houver;
     */
    private Block SearchValidFundBlock(float value) {
        for (Block b : chain.getAllBlocksToUser(hashID)) {
            if (b.Value() >= value) {
                return b;
            }
        }
        return null;
    }

    /**
     * Cria uma transação e a envia aos pares
     *
     * @param targetHash Usuário alvo da transação
     * @param value Valor a ser transferido
     */
    private void createTransaction(String targetHash, float value) {
        try {

            Block fund = SearchValidFundBlock(value);
            if (fund == null) {
                System.out.println("Sem um bloco de fundos válido");
                return;
            }

            Block b = new Block(this.hashID, targetHash, new Date(), chain.Head().Hash(),
                    fund, value);

            unconfirmedTransactions.add(b);

            //Broadcast
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

    /**
     * Adiciona um par à lista desta carteira
     *
     * @param HashID Id do par
     * @param address Endereco eletronico do par
     */
    @Override
    public void addPeer(String HashID, InetAddress address) {
        System.out.println("Receiving peer");
        if (miners.containsKey(HashID)) {
            return;
        }
        miners.put(HashID, address);
    }

    /**
     * Recupera a blockchain atualizada do servidor
     *
     * @return a Blockchain atualizada
     */
    private BlockChain getBlockchainFromServer() {
        byte[] data = (BCTimestampServer.ASKFORCHAIN + "").getBytes();

        byte[] recvBuf = new byte[50 * 1024];
        try {
            socket.send(new DatagramPacket(data, data.length, server, BCTimestampServer.SERVERRECEIVEPORT));
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
     * Recebe a confirmação que uma transação foi validada
     *
     * @param blockHash
     */
    public void confirmTransaction(String blockHash) {
        Block conf = null;
        for (Block b : unconfirmedTransactions) {
            if (b.Hash().equals(blockHash)) {
                conf = b;
                myTransactions.add(b);
                unconfirmedTransactions.remove(conf);
                System.out.println("Transaction Confirmed");
                System.out.println(conf.toStringLines());
                System.out.println("Your Balance: " + getBalance());
                return;
            }
        }
    }
    
    public String ID(){
        return this.hashID;
    }

}
