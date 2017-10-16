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
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author luca
 */
public class BCServerHandler implements Runnable{
    
    private DatagramSocket socket;
    DatagramPacket commPacket;
    String response = "";
    String command = "";
    
    public BCServerHandler(DatagramPacket comPacket) throws SocketException, UnknownHostException{        
        this.socket = new DatagramSocket(8889, InetAddress.getByName("0.0.0.0"));
        this.commPacket = comPacket;
    }

    @Override
    public void run() {
        command = new String(commPacket.getData()).trim();
        int com = Integer.parseInt(command);
        switch(com){
            case BCTimestampServer.DISCOVERY:
                response = BCTimestampServer.SERVERDISCOVERYRESPONSE + "";
                break;
            default:
                break;
        }
        
        byte[] resBytes = response.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(resBytes, resBytes.length, commPacket.getAddress(), commPacket.getPort());
        try {
            socket.send(sendPacket);
            System.out.println("Server sent response " + new String(sendPacket.getData()) + " to " + sendPacket.getAddress().getHostAddress());
            socket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
}
