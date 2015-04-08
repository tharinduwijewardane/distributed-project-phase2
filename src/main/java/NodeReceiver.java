import java.io.IOException;
import java.net.*;

/**
 * Created by Tharindu Wijewardane on 2015-03-06.
 */
public class NodeReceiver {

    /**
     * Listen to the given DatagramSocket and return the received packet
     *
     * @param myUDPSocket DatagramSocket used
     * @return
     * @throws IOException
     */
    public static DatagramPacket receiveUDP(DatagramSocket myUDPSocket) throws IOException {

        byte[] buf = new byte[200];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        myUDPSocket.receive(packet);
        String received = new String(packet.getData(), 0, packet.getLength());
        System.out.println("From Node " + packet.getAddress().toString() + ":" + packet.getPort() + ":- " + received);

        return packet;
    }

}
