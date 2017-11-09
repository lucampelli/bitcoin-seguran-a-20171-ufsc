package trabalhofinal;

import MinerUI.LoadFrame;
import MinerUI.MinerFrame;

import javax.swing.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.MessageDigest;
import java.util.*;

import static java.lang.Thread.yield;
import static trabalhofinal.BCTimestampServer.*;

/**
 * Programa Minerador
 */
public class BCMiner extends BCClient {

    private BlockChain chain;
    private Block working = null;
    private List<Block> pending;

    private InetAddress server;
    private HashMap<String, InetAddress> peers;
    private HashMap<String, InetAddress> miners;

    private String hashID = "";

    private MulticastSocket socket;

    private float timeup = 5;

    private int nonce = 0;

    private MinerFrame frame;

    private long mediaAlvo = 10000;
    private long media = 0;
    private long bias = 700;
    private long lastArrived = 0;
    private float dificuldade = 4;

    /**
     * Construtor para o programa minerador
     */
    public BCMiner() {
        try {
            hashID = BCTimestampServer.bytesToHex(MessageDigest.getInstance("SHA-256").digest(JOptionPane.showInputDialog("Insira seu nome por favor").getBytes()));
            System.out.println("Your Miner ID:" + hashID);

            peers = new HashMap();
            miners = new HashMap();
            pending = new ArrayList<>();

            socket = new MulticastSocket();
            socket.joinGroup(InetAddress.getByName(MULTICAST_GROUP_ADDRESS));

            byte[] data = (BCTimestampServer.DISCOVERY + ":" + hashID).getBytes();
            sendBroadcast(data);

            System.out.println("Looking for Peers...");

            Date date = new Date();

            socket.setSoTimeout(1000);

            long currentTime = date.getTime();

            long startTime = date.getTime();

            LoadFrame loading = new LoadFrame();

            //Por 5 segundos espera respostas de peers ou miners ou do servidor
            while (currentTime - startTime < timeup * 1000) {
                System.out.println("Waiting for a response");
                byte[] recvBuf = new byte[15000];
                DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
                try {
                    socket.receive(receivePacket);
                    System.out.println("Receive packet from: " + receivePacket.getSocketAddress());
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    currentTime = new Date().getTime();
                    loading.updateLoadingBar((int) ((currentTime - startTime) / 1000));
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
                    peers.put(recData.split(":")[1], receivePacket.getAddress());
                    System.out.println("Peer acknowledged: " + (receivePacket.getAddress()).getHostAddress());
                }
                if (recData.split(":")[0].equals(BCTimestampServer.MINERRESPONSE + "")) {
                    miners.put(recData.split(":")[1], receivePacket.getAddress());
                    System.out.println("Miner acknowledged: " + (receivePacket.getAddress()).getHostAddress());
                }

                currentTime = new Date().getTime();
                loading.updateLoadingBar((int) ((currentTime - startTime) / 1000));
                System.out.println("End Cycle:" + ((currentTime - startTime) / 1000.0));
            }
            loading.setVisible(false);
            loading.dispose();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        frame = new MinerFrame(this);

        chain = getBlockchainFromServer();
        System.out.println(chain.toStringLines());

        new Thread(new BCMinerSocket(this)).start();

        while (true) {
            if (working == null) {
                if (!pending.isEmpty()) {
                    System.out.println("New work");
                    working = pending.get(0);
                }
            } else if (proofOfWork((int) dificuldade) != -1) {
                chain.addBlock(working);
                pending.removeIf((Block p) -> p.Hash().equals(working.Hash()));

                System.out.println("POW success");
                sendBlockValidated();
                working = null;
                System.out.println(chain.toStringLines());
            }
            yield();
        }

    }

    public void sendBroadcast(byte[] data) throws IOException {
        List<Short> ports = Arrays.asList(SERVERRECEIVEPORT, MINERRECEIVEPORT, WALLETRECEIVEPORT);
        for (short port : ports) {
            DatagramPacket packet = new DatagramPacket(
                data,
                data.length,
                InetAddress.getByName(MULTICAST_GROUP_ADDRESS),
                port
            ); // broadcast for peers and server
            socket.send(packet);
        }
    }

    /**
     * Envia uma mensagem para receber a blockchain do servidor
     *
     * @return A blockchain atualizada do servidor
     */
    private BlockChain getBlockchainFromServer() {
        byte[] data = (BCTimestampServer.ASKFORCHAIN + "").getBytes();
        if (server == null) {
            return new BlockChain();
        }
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
     * Retorna para a GUI o estado do bloco sendo trabalhado atualmente
     *
     * @return O Bloco sendo trabalhado atualmente
     */
    public String getWorkingAsString() {
        String ans = "";
        if (working != null) {
            ans = "Hash: " + working.Hash() + System.lineSeparator() + "Owner: " + working.ID()
                + System.lineSeparator() + "Target: " + working.target() + System.lineSeparator()
                + "Nonce: " + nonce;
        }
        return ans;
    }

    /**
     * Adiciona uma nova conexão as existentes
     *
     * @param HashID  ID do peer a adicionar
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
     *
     * @param b o novo bloco
     */
    public void receiveBlockValidationRequest(Block b) {
        if (CheckValid(b)) {
            pending.add(b);
            System.out.println(pending.get(pending.size() - 1));
        } else {
            System.out.println("Invalid Block");
            sendDeniedBroadcast(b);
        }

    }

    /**
     * Faz a proof of work, um quebra cabeça dificil que leva tempo e garante
     * que os blocos demorem a ser trabalhados.
     *
     * @return o resultado do quebra cabeça
     */
    public int proofOfWork(int difficulty) {
        try {
            String block = working.Hash();
            String hash = "";
            nonce = 0;
            while (!hash.startsWith(POWdiff(difficulty)) && working != null) {
                hash = BCTimestampServer.bytesToHex(MessageDigest.getInstance("SHA-512").digest((block + "" + nonce).getBytes()));
                nonce++;
                frame.updateText();
            }
            if (working != null) {
                System.out.println(hash.substring(0, 5));
                frame.updateText();
                return nonce;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Recebe uma mensagem de que um bloco foi validado
     *
     * @param b o bloco validado
     */
    public void receiveBlockValidatedRequest(Block b) {
        pending.removeIf((Block p) -> p.Hash().equals(b.Hash()));
        chain.addBlock(b);

        if (working != null && working.Hash().equals(b.Hash())) {
            working = null;
        }

        long intervalo = b.getTime() - lastArrived;
        media = (media + intervalo) / 2;
        if (media < mediaAlvo - bias) {
            dificuldade -= 0.1f;
        }
        if (media > mediaAlvo + bias) {
            dificuldade += 0.1f;
        }
        lastArrived = b.getTime();
    }

    /**
     * Envia a mensagem de que este minerador validou um bloco
     */
    private void sendBlockValidated() {
        try {
            DatagramPacket packet;
            working.timeStamp(new Date(), chain.Head().Hash());
            byte[] message = (BCTimestampServer.TRANSACTIONCONFIRMEDBROADCAST + ":" + working.toString()).getBytes();

            long intervalo = working.getTime() - lastArrived;
            media = (media + intervalo) / 2;
            if (media < mediaAlvo - bias) {
                dificuldade -= 0.1f;
            }
            if (media > mediaAlvo + bias) {
                dificuldade += 0.1f;
            }
            lastArrived = working.getTime();

            sendBroadcast(message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Confere se um bloco é valido
     *
     * @param b o Bloco
     * @return True se é valido
     */
    private boolean CheckValid(Block b) {
        this.chain = getBlockchainFromServer();
        if (chain.getBlockByHash(b.fundBlock()) == null) {
            System.out.println("Inexistant fund block");
            return false;
        }
        if (!chain.getBlockByHash(b.fundBlock()).target().equals(b.ID())) {
            System.out.println("Invalid fund block target");
            return false;
        }
        if (chain.getBlockByHash(b.fundBlock()).Value() < b.Value()) {
            System.out.println("Invalid fund block value");
            return false;
        }
        for (Block block : chain.getAllBlocksFromUser(b.ID())) {
            if (block.fundBlock().equals(b.fundBlock())) {
                System.out.println("Block already spent");
                return false;
            }
        }
        return true;
    }

    /**
     * Utilidade para facilitar a comparação de Strings e a variação da
     * dificuldade do POW
     *
     * @param diff a dificuldade do teste
     * @return Uma string com tantos 0s quanto a dificuldade
     */
    private String POWdiff(int diff) {
        StringBuilder ans = new StringBuilder();
        for (int i = 0; i < diff; i++) {
            ans.append("0");
        }
        return ans.toString().trim();
    }

    /**
     * Retorna o ID do minerador
     *
     * @return ID
     */
    public String ID() {
        return this.hashID;
    }

    public int Port() {
        return socket.getPort();
    }

    /**
     * Recebe a mensagem que o bloco b foi negado
     *
     * @param b o bloco negado
     */
    public void receiveDeniedBlock(Block b) {
        pending.removeIf((Block p) -> p.Hash().equals(b.Hash()));

        if (working != null && working.Hash().equals(b.Hash())) {
            working = null;
        }
    }

    public void sendDeniedBroadcast(Block b) {
        try {
            DatagramPacket packet;
            byte[] message = (BCTimestampServer.TRANSACTIONDENIEDBROADCAST + ":" + b.toString()).getBytes();

            sendBroadcast(message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
