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
 * @author luca
 * Thread que cuida de responder as mensagens do servidor 
 */
public class BCServerHandler implements Runnable{
    
    private DatagramSocket socket;
    DatagramPacket commPacket;
    BlockChain chain;
    String response = "";
    String command = "";
    
    public BCServerHandler(DatagramPacket comPacket, DatagramSocket socket) throws SocketException, UnknownHostException{        
        this.socket = socket;
        this.commPacket = comPacket;
    }
    
    public BCServerHandler(DatagramPacket comPacket, DatagramSocket socket, BlockChain chain) throws SocketException, UnknownHostException{        
        this.socket = socket;
        this.commPacket = comPacket;
        this.chain = chain;
    }

    @Override
    public void run() {
        command = new String(commPacket.getData()).trim().split(" ")[0];
        int com = Integer.parseInt(command);
        switch(com){
            case BCTimestampServer.DISCOVERY:
                chain.addBlock(new Block(new String(commPacket.getData()).trim().split(" ")[1], BCTimestampServer.matrixCount, 1));
                BCTimestampServer.matrixCount++;
                response = BCTimestampServer.SERVERDISCOVERYRESPONSE + "";
                break;
            case BCTimestampServer.ASKFORCHAIN:
                sendTo(chain,commPacket.getAddress(),commPacket.getPort());
                break;
            default:
                break;
        }
        
        byte[] resBytes = response.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(resBytes, resBytes.length, commPacket.getAddress(), commPacket.getPort());
        try {
            socket.send(sendPacket);
            System.out.println("Server sent response " + new String(sendPacket.getData()) + " to " + sendPacket.getAddress().getHostAddress());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
        
    public void sendTo(Object o, String hostname, int port) throws IOException {
        sendTo(o, InetAddress.getByName(hostname), port);
    }

    public void sendTo(Object o, InetAddress address, int port){
        try (
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(50 * 1024);
            ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(byteStream))
        ) {
            os.flush();
            os.writeObject(o);
            os.flush();
            //retrieves byte array
            byte[] sendBuf = byteStream.toByteArray();
            DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, address, port);
            socket.send(sendPacket);
            os.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
}
