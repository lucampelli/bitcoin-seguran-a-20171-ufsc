/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trabalhofinal;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author luca Thread que cuida de responder as mensagens do servidor
 */
public class BCServerHandler implements Runnable {

    private DatagramSocket socket;
    DatagramPacket commPacket;
    BlockChain chain;
    String response = "";
    String command = "";
    long mediaAlvo = 10000;
    long media = 0;
    long bias = 700;
    long lastArrived = 0;
    float dificuldade = 4;
    
    /**
     * Handler para que o servidor nao pare
     * @param comPacket
     * @param socket
     * @throws SocketException
     * @throws UnknownHostException 
     */
    public BCServerHandler(DatagramPacket comPacket, DatagramSocket socket) throws SocketException, UnknownHostException {
        this.socket = socket;
        this.commPacket = comPacket;
        chain = null;
    }

    /**
     * Handler para que o servidor nao pare, já com uma chain criada
     * @param comPacket
     * @param socket
     * @throws SocketException
     * @throws UnknownHostException 
     */
    public BCServerHandler(DatagramPacket comPacket, DatagramSocket socket, BlockChain chain) throws SocketException, UnknownHostException {
        this.socket = socket;
        this.commPacket = comPacket;
        this.chain = chain;
    }

    @Override
    public void run() {
        command = new String(commPacket.getData()).trim().split(":")[0];
        System.out.println(command);
        int com = Integer.parseInt(command);
        switch (com) {
            case BCTimestampServer.DISCOVERY:
                boolean t = false;
                for (Block b : chain.getAllBlocksToUser(new String(commPacket.getData()).trim().split(":")[1])) {
                    if (b.ID() == "") {
                        t = true;
                    }
                }
                if (!t) {
                    chain.addBlock(new Block(new String(commPacket.getData()).trim().split(":")[1], BCTimestampServer.matrixCount, 1));
                    BCTimestampServer.matrixCount++;
                }
                response = BCTimestampServer.SERVERDISCOVERYRESPONSE + "";
                System.out.println("Server sent response " + response);
                break;
            case BCTimestampServer.ASKFORCHAIN:
                sendTo(chain, commPacket.getAddress(), commPacket.getPort());
                System.out.println("Server sent chain");
                response = "";
                break;
            case BCTimestampServer.TRANSACTIONCONFIRMEDBROADCAST:
                Block b = new Block(new String(commPacket.getData()).trim().split(":")[1]);
                System.out.println("Server Received a transaction completed");
                
                long intervalo = b.getTime() - lastArrived;
                media = (media + intervalo)/2;
                if(media < mediaAlvo - bias){
                    dificuldade -= 0.1f;
                }
                if(media > mediaAlvo + bias){
                    dificuldade += 0.1f;
                }
                lastArrived = b.getTime();
                chain.addBlock(b);
                break;
            default:
                break;
        }

        if (response.equals(BCTimestampServer.SERVERDISCOVERYRESPONSE + "")) {
            byte[] resBytes = response.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(resBytes, resBytes.length, commPacket.getAddress(), commPacket.getPort());
            try {
                socket.send(sendPacket);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

    /**
     * Função que envia um objeto à um par
     * @param o
     * @param hostname
     * @param port
     * @throws IOException 
     */
    public void sendTo(Object o, String hostname, int port) throws IOException {
        sendTo(o, InetAddress.getByName(hostname), port);
    }

    /**
     * Função que envia um objeto à um par
     * @param o
     * @param address
     * @param port 
     */
    public void sendTo(Object o, InetAddress address, int port) {
        try (
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream(50 * 1024);
                ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(byteStream))) {
            os.flush();
            os.writeObject(o);
            os.flush();
            //retrieves byte array
            byte[] sendBuf = byteStream.toByteArray();
            DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, address, port);
            socket.send(sendPacket);
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
