import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * Created by Tharindu Wijewardane on 2015-03-04.
 */
public class BootstrapCommunicator {

    /**
     * Send a TCP message to the Bootstrap server and return the received reply
     *
     * @param ipAddress of Bootstrap server
     * @param port      of Bootstrap server
     * @param message
     * @return
     */
    public static String sendTCP(String ipAddress, int port, String message) {
        Socket socket = null;
        try {
            socket = new Socket(ipAddress, port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.write(message); // "0027 REG localhost 9900 abg"
            out.flush();
            System.out.println("To BS: " + message);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            StringBuilder stringBuilder = new StringBuilder();
            int a;
            try {
                while ((a = in.read()) != -1) {
                    stringBuilder.append((char) a);
                }
            } catch (IOException e) {
                System.out.println("failed while receiving from BS");
                e.printStackTrace();
            }
            out.close();
            in.close();
            socket.close();

            String response = stringBuilder.toString();
            System.out.println("From BS: " + response);
            return response;

        } catch (IOException e) {
            System.out.println("communication with BS failed");
            e.printStackTrace();
        }

        return null;
    }

}
