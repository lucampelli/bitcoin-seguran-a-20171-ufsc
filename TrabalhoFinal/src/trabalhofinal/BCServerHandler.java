package trabalhofinal;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.*;

/**
 * Thread que cuida de responder as mensagens do servidor
 */
public class BCServerHandler implements Runnable {

    private DatagramSocket socket;
    DatagramPacket commPacket;
    BlockChain chain;
    String response = "";
    String command = "";

    /**
     * Handler para que o servidor nao pare
     *
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
     *
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
                    if (b.ID().isEmpty()) {
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
                chain.addBlock(b);
                break;
            default:
                break;
        }

        if (response.equals(BCTimestampServer.SERVERDISCOVERYRESPONSE + "")) {
            byte[] resBytes = response.getBytes();

            System.out.println("Sending DISCOVERY response to: " + commPacket.getSocketAddress());
            DatagramPacket sendPacket = new DatagramPacket(resBytes, resBytes.length, commPacket.getSocketAddress());
            try {
                socket.send(sendPacket);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

    /**
     * Função que envia um objeto à um par
     *
     * @param o       Objeto que será serializado
     * @param address endereço do destinatário
     * @param port    porta do processo do destinatário
     */
    public void sendTo(Object o, InetAddress address, int port) {
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

            System.out.println("Sending requested blockchain to: " + commPacket.getSocketAddress());
            socket.send(sendPacket);
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
