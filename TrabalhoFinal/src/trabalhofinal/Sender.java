package trabalhofinal;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Sender {

    private DatagramSocket socket;

    public Sender(DatagramSocket socket) {
        this.socket = socket;
    }

    public void sendTo(Object o, String hostname, int port) throws IOException {
        sendTo(o, InetAddress.getByName(hostname), port);
    }

    public void sendTo(Object o, InetAddress address, int port) throws IOException {
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
        }
    }

}
