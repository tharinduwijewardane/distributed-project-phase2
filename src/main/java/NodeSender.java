import java.io.IOException;
import java.net.*;

/**
 * Created by Tharindu Wijewardane on 2015-03-04.
 */
public class NodeSender {

    /**
     * Send a message to a node using UDP
     *
     * @param myUDPSocket DatagramSocket used
     * @param message
     * @param ipAddress   of receiver
     * @param port        of sender
     */
    public static void sendUDP(DatagramSocket myUDPSocket, String message, String ipAddress, int port) {

        try {
            byte[] buf = message.getBytes();
            InetAddress address = InetAddress.getByName(ipAddress);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
            myUDPSocket.send(packet);
            System.out.println("To Node: " + packet.getAddress().toString() + ":" + packet.getPort() + ":- " + message);

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
