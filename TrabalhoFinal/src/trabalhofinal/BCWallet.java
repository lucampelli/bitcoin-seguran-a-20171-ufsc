/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trabalhofinal;

import java.awt.HeadlessException;
import java.awt.MouseInfo;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import sun.text.normalizer.Utility;

/**
 *
 * @author luca
 */
public class BCWallet {

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private BlockChain chain;

    private Inet4Address server;
    private ArrayList<Inet4Address> peers;

    private String hashID = "";

    private DatagramSocket socket;

    private float balance;

    private float timeup = 5;

    public BCWallet() {
        try {
            balance = 0;
            hashID = bytesToHex(MessageDigest.getInstance("SHA-512").digest((new Date().getTime() + "" + MouseInfo.getPointerInfo().getLocation().x + "" + MouseInfo.getPointerInfo().getLocation().y).getBytes()));
            System.out.println("Your ID:" + hashID);

            socket = new DatagramSocket();
            socket.setBroadcast(true);

            byte[] data = (BCTimestampServer.DISCOVERY + "").getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), BCTimestampServer.SERVERRECEIVEPORT); // broadcast for peers and server
            socket.send(packet);

            System.out.println("Looking for Peers");

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
                    server = (Inet4Address) receivePacket.getAddress();
                    System.out.println("Server acknowledged: " + ((Inet4Address) receivePacket.getAddress()).getHostAddress());
                }
                if (new String(receivePacket.getData()).trim().equals(BCTimestampServer.PEERRESPONSE + "")) {
                    peers.add((Inet4Address) receivePacket.getAddress());
                    System.out.println("Peer acknowledged: " + ((Inet4Address) receivePacket.getAddress()).getHostAddress());
                }

                currentTime = new Date().getTime();
                System.out.println("End Cycle:" + ((currentTime - startTime) / 1000));
            }

        } catch (NoSuchAlgorithmException | HeadlessException | IOException ex) {
        }

    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

}
