import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashSet;

/**
 * Created by Tharindu on 2015-03-06.
 */
public class Main {
    public static void main(String[] args) throws SocketException {

        Node.setupFiles();
        Node.registerWithBS();

        DatagramSocket myUDPSocket = new DatagramSocket(Util.PORT); //the port used for all UDP communication

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.println("\nEnter command to continue:");
            System.out.println("\t1 - Join the distributed system");
            System.out.println("\t2 - Search for file");
            System.out.println("\t3 - Leave the distributed system and Unregister from bootstrap server");
            System.out.println("\t9 - Exit");
            try {
                switch (Integer.parseInt(br.readLine())) {
                    case 1:
                        Node.joinDistributedSystem(myUDPSocket);
                        break;
                    case 2:
                        System.out.println("Enter filename to search:");
                        String filename = br.readLine();
                        if (Node.localFiles.contains(filename)) {
                            System.out.println("Exact file available locally. No need to search");
                        } else {
                            System.out.println("Exact file not available locally");
                            HashSet<String> available = Node.checkForFileAvailability(filename);
                            for (String name : available) {
                                System.out.println("Similar file available locally: " + name);
                            }
                            System.out.println("Sending the file search query to distributed system");
                            Node.searchFile(myUDPSocket, filename);
                        }
                        break;
                    case 3:
                        Node.leaveDistributedSystem(myUDPSocket);
                        Node.unregisterFromBS();
                        break;
                    case 9:
                        System.exit(0);
                    default:
                        System.out.println("Unknown command. Please try again");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
