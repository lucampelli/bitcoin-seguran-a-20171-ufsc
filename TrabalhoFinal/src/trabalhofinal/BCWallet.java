package trabalhofinal;

import MinerUI.LoadFrame;
import WalletUI.WalletFrame;

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

import static trabalhofinal.BCTimestampServer.*;

/**
 * Programa Carteira
 */
public class BCWallet extends BCClient {

    private BlockChain chain;
    private ArrayList<Block> unconfirmedTransactions;

    private InetAddress server;
    private HashMap<String, InetAddress> miners;
    private HashMap<String, InetAddress> peers;

    private String hashID = "";

    private MulticastSocket socket;

    private float balance;

    private float timeup = 5;

    private WalletFrame frame;

    /**
     * Carteira que mostra o balanco e cria novas transações
     */
    public BCWallet() {
        try {
            balance = 0;
            hashID = bytesToHex(MessageDigest.getInstance("SHA-256").digest(JOptionPane.showInputDialog("Insira seu nome por favor").getBytes()));

            System.out.println("Your Wallet ID:" + hashID);
            peers = new HashMap<>();
            miners = new HashMap<>();
            unconfirmedTransactions = new ArrayList<>();

            socket = new MulticastSocket();
            socket.joinGroup(InetAddress.getByName(MULTICAST_GROUP_ADDRESS));

            byte[] data = (DISCOVERY + ":" + hashID).getBytes();
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

            System.out.println("Looking for Peers...");

            Date date = new Date();

            socket.setSoTimeout(1000);

            long currentTime = date.getTime();

            long startTime = date.getTime();

            LoadFrame loading = new LoadFrame();

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
                    currentTime = new Date().getTime();
                    loading.updateLoadingBar((int) ((currentTime - startTime) / 1000));
                    continue;
                }

                //Recebemos resposta
                System.out.println("Broadcast response: " + new String(receivePacket.getData()).trim() + " " + receivePacket.getAddress().getHostAddress());

                String recData = new String(receivePacket.getData()).trim();

                if (recData.split(":")[0].equals(SERVERDISCOVERYRESPONSE + "")) {
                    server = receivePacket.getAddress();
                    System.out.println("Server acknowledged: " + (receivePacket.getAddress()).getHostAddress());
                }
                if (recData.split(":")[0].equals(MINERRESPONSE + "")) {
                    miners.put(recData.split(":")[1], receivePacket.getAddress());
                    System.out.println("Miner acknowledged: " + (receivePacket.getAddress()).getHostAddress());
                }
                if (recData.split(":")[0].equals(PEERRESPONSE + "")) {
                    peers.put(recData.split(":")[1], receivePacket.getAddress());
                    System.out.println("Peer acknowledged: " + (receivePacket.getAddress()).getHostAddress());
                }

                currentTime = new Date().getTime();
                loading.updateLoadingBar((int) ((currentTime - startTime) / 1000));
            }

            loading.setVisible(false);
            loading.dispose();

        } catch (Exception ex) {
        }

        chain = getBlockchainFromServer();
        System.out.println(chain.toStringLines());

        new Thread(new BCWalletSocket(this)).start();

        frame = new WalletFrame(this);
        frame.setVisible(true);

        /*
        String target = "AC84B32E9D61A4422D1F7AABEF96C326CD2BDD61BFDBF46C2E193EC645B1CA40DD72662FD25B194A1403EDF76B80D18042A220C4DC97966DE718E37F64FFCF9A";

        System.out.println("Your Balance: " + getBalance());

        createTransaction(target, 1.0f);
         */
    }

    /**
     * recupera o balanço desta carteira analisando a blockchain
     *
     * @return o valor do balanco
     */
    public float getBalance() {
        float sum = 0;
        for (Block b : chain.getAllBlocksToUser(hashID)) {
            sum += b.Value();
        }
        for (Block b : chain.getAllBlocksFromUser(hashID)) {
            sum += b.change() - b.Value();
        }

        int index = 0;

        for (Block b : unconfirmedTransactions) {
            long current = new Date().getTime();
            if (b.getTime() > current + 10000) {
                break;
            }
            index++;
        }
        if (index < unconfirmedTransactions.size()) {
            unconfirmedTransactions.remove(index);
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
                if(chain.getAllBlocksFromUser(hashID).isEmpty()) {
                    return b;
                }
                boolean spent = false;
                for (Block block : chain.getAllBlocksFromUser(hashID)) {
                    if (block.fundBlock().equals(b.Hash())) {
                        spent = true;
                        break;
                    }
                }
                if(!spent){
                    return b;
                }

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
    public void createTransaction(String targetHash, float value) {
        try {
            Block fund = SearchValidFundBlock(value);
            if (fund == null) {
                System.out.println("Sem um bloco de fundos válido");
                return;
            }

            Block b = new Block(this.hashID, targetHash, new Date().getTime(), chain.Head().Hash(),
                    fund, value);

            unconfirmedTransactions.add(b);

            //Broadcast
            byte[] data = (TRANSACTIONSTARTBROADCAST + ":" + b.toString()).getBytes();

            DatagramPacket packet = new DatagramPacket(
                    data,
                    data.length,
                    InetAddress.getByName(MULTICAST_GROUP_ADDRESS),
                    MINERRECEIVEPORT
            );

            System.out.println("New Block");
            socket.send(packet);
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
     * Retorna todos os pares que esta carteira possui registrados
     *
     * @return
     */
    public HashMap<String, InetAddress> getPeers() {
        HashMap<String, InetAddress> temp = new HashMap<>();
        if (!peers.isEmpty()) {
            temp.putAll(peers);
        }
        if (!miners.isEmpty()) {
            temp.putAll(miners);
        }
        return temp;
    }

    /**
     * Recupera a blockchain atualizada do servidor
     *
     * @return a Blockchain atualizada
     */
    private BlockChain getBlockchainFromServer() {
        byte[] data = (ASKFORCHAIN + "").getBytes();

        byte[] recvBuf = new byte[50 * 1024];
        try {
            socket.send(new DatagramPacket(data, data.length, server, SERVERRECEIVEPORT));
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
     * @param confirmed Bloco da transação que foi validada
     */
    public void confirmTransaction(Block confirmed) {
        chain.addBlock(confirmed);
        int index = -1;
        for (Block b : unconfirmedTransactions) {
            index++;
            if (b.Hash().equals(confirmed.Hash())) {
                unconfirmedTransactions.remove(index);
                break;
            }
        }
        frame.updateText();
    }

    /**
     * Retorna para a GUI todas as transações feitas por, enviadas a este
     * usuário de forma legível
     *
     * @return uma string com todas as transações em forma de string
     * simplificada
     */
    public String getTransactionsAsString() {
        StringBuilder ans = new StringBuilder();
        for (Block b : chain.getAllBlocksFromUser(hashID)) {
            ans.append("Transação:").append(System.lineSeparator())
                    .append("    Alvo: ").append(b.target()).append(System.lineSeparator())
                    .append("    Value: ").append(b.Value())
                    .append("    Change: ").append(b.change()).append(System.lineSeparator())
                    .append("Hash:").append(b.Hash()).append(System.lineSeparator())
                    .append("Status: Confirmada").append(System.lineSeparator());
        }
        for (Block b : unconfirmedTransactions) {
            ans.append("Transação:").append(System.lineSeparator())
                    .append("    Alvo: ").append(b.target()).append(System.lineSeparator())
                    .append("    Value: ").append(b.Value())
                    .append("    Change: ").append(b.change()).append(System.lineSeparator())
                    .append("Hash:").append(b.Hash()).append(System.lineSeparator())
                    .append("Status: Pendente").append(System.lineSeparator());
        }
        for (Block b : chain.getAllBlocksToUser(hashID)) {
            String owner = b.ID();
            if (owner.equals("")) {
                owner = "<Matriz>";
            }
            ans.append("Transação:").append(System.lineSeparator())
                    .append("    Dono: ").append(owner).append(System.lineSeparator())
                    .append("    Value: ").append(b.Value()).append(System.lineSeparator())
                    .append("    Hash: ").append(b.Hash()).append(System.lineSeparator());
        }

        return ans.toString();
    }

    public String ID() {
        return this.hashID;
    }

    /**
     * Recebe a mensagem que uma transação foi negada
     *
     * @param block o bloco inválido
     */
    void receiveDeniedBlock(Block block) {
        int index = 0;
        for (Block b : unconfirmedTransactions) {
            if (b.Hash().equals(block.Hash())) {
                unconfirmedTransactions.remove(index);
                break;
            }
            index++;
        }
        frame.updateText();
    }

}
