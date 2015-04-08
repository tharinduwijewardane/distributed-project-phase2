import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;

/**
 * Created by Tharindu Wijewardane on 2015-03-03.
 */
public class Node {

    static GUI gui;

    static ArrayList<String> localFiles = new ArrayList<String>();
    static HashSet<Neighbour> neighbours = new HashSet<Neighbour>(); // routing table
    static Vector<String> recentSearchQueries = new Vector<String>();

    static Thread receiver;

    /**
     * Add recent search query to a list and delete it after 5 seconds.
     * return true if the query already exists in list and false otherwise.
     *
     * @param query
     * @return
     */
    static boolean checkInRecentQueries(final String query) {

        if (recentSearchQueries.contains(query)) {
            return true;
        } else {
            recentSearchQueries.add(query);
            new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(5 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    recentSearchQueries.remove(query);
                }
            }.start();
            return false;
        }
    }

    /**
     * Return a set of similar files (filename matching part of available filenames) available locally.
     * Empty set if no similar file available
     *
     * @param filename
     * @return
     */
    static HashSet<String> checkForFileAvailability(String filename) {
        HashSet<String> availableFileNames = new HashSet<String>();
        String[] words = filename.split(" ");

        for (String file : localFiles) {
            for (int i = 0; i < words.length; i++) {
                if (file.contains(words[i] + " ") || file.contains(" " + words[i]) || file.equals(words[i])) {
                    availableFileNames.add(file);
                }
            }
        }
        return availableFileNames;
    }

    /**
     * Send a search query to neighbouring nodes
     *
     * @param myUDPSocket
     * @param filename
     */
    static void searchFile(final DatagramSocket myUDPSocket, String filename) {

        String basicQuery = "SER " + Util.IP + " " + Util.PORT + " " + filename;
        checkInRecentQueries(basicQuery); // just to add the entry

        int hopCount = 4; //TODO decide a value for initial hop count

        for (Neighbour neighbour : neighbours) {
            NodeSender.sendUDP(myUDPSocket, Util.formMessage("SER " + Util.IP + " " + Util.PORT + " " + filename + " " + hopCount),
                    neighbour.ipAddress, neighbour.port);
        }
    }

    /**
     * Forward a search query to neighbouring nodes.
     *
     * @param myUDPSocket
     * @param searcherIp
     * @param searcherPort
     * @param filename
     * @param hops
     */
    static void forwardSearchRequest(DatagramSocket myUDPSocket, String searcherIp, int searcherPort, String filename, int hops) {

        for (Neighbour neighbour : neighbours) {
            NodeSender.sendUDP(myUDPSocket, Util.formMessage("SER " + searcherIp + " " + searcherPort + " " + filename + " " + hops),
                    neighbour.ipAddress, neighbour.port);

        }
    }

    /**
     * Send the unregister command to the Bootstrap server
     */
    static void unregisterFromBS() {
        String response = BootstrapCommunicator.sendTCP(Util.BS_IP, Util.BS_PORT, Util.formMessage("UNREG " + Util.IP + " " + Util.PORT + " " + Util.USERNAME));
        Node.decodeServerResponse(response);
    }

    /**
     * Send leaving message to neighbouring nodes and stop the UDP receiver
     *
     * @param myUDPSocket
     */
    static void leaveDistributedSystem(DatagramSocket myUDPSocket) {

        for (Neighbour neighbour : neighbours) {
            NodeSender.sendUDP(myUDPSocket, Util.formMessage("LEAVE " + Util.IP + " " + Util.PORT), neighbour.ipAddress, neighbour.port);
        }
        receiver.interrupt(); //turn off UDP receiver thread
        neighbours = new HashSet<Neighbour>(); // empty the routing table
    }

    /**
     * Send the joining message to the neighbouring nodes and start the UDP receiver
     *
     * @param myUDPSocket
     */
    static void joinDistributedSystem(final DatagramSocket myUDPSocket) {

        receiver = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        System.out.println("receiver loop started");
                        DatagramPacket receivedPacket = NodeReceiver.receiveUDP(myUDPSocket);
                        decodeNodePacket(myUDPSocket, receivedPacket);
                    }
                } catch (IOException e) {
                    System.out.println("receiver loop failed");
                    e.printStackTrace();
                }

            }
        };
        receiver.start();

        for (Neighbour neighbour : neighbours) {
            NodeSender.sendUDP(myUDPSocket, Util.formMessage("JOIN " + Util.IP + " " + Util.PORT), neighbour.ipAddress, neighbour.port);
        }

    }

    /**
     * Decode received a UDP packet and take relevant actions
     *
     * @param myUDPSocket
     * @param packet
     */
    private static void decodeNodePacket(DatagramSocket myUDPSocket, DatagramPacket packet) {

        if (packet == null) {
            return;
        }

        String ip = packet.getAddress().toString().substring(1);
        int port = packet.getPort();
        String message = new String(packet.getData(), 0, packet.getLength());

        StringTokenizer st = new StringTokenizer(message);
        try {
            int length = Integer.parseInt(st.nextToken());
            String keyword = st.nextToken();
            if (keyword.equalsIgnoreCase("JOIN")) { // received join request
                Neighbour n = new Neighbour(st.nextToken(), Integer.parseInt(st.nextToken()));
                neighbours.add(n);
                gui.refreshNeighbourList();
                NodeSender.sendUDP(myUDPSocket, Util.formMessage("JOINOK 0"), ip, port);
                System.out.println("neighbour joined: " + n.ipAddress + " " + n.port);

            } else if (keyword.equalsIgnoreCase("JOINOK")) { // received join response
                switch (st.nextToken()) {
                    case "0": // successful
                        Neighbour n = new Neighbour(ip, port);
                        neighbours.add(n);
                        gui.refreshNeighbourList();
                        System.out.println("neighbour added: " + n.ipAddress + " " + n.port);
                        break;

                    case "9999": // error while adding new node to routing table
                        break;
                }

            } else if (keyword.equalsIgnoreCase("LEAVE")) { // received leave request
                String neighbourIp = st.nextToken(); // use given ip:port (or can use source ip:port)
                int neighbourPort = Integer.parseInt(st.nextToken());
                Iterator<Neighbour> neighboursItr = neighbours.iterator();
                while (neighboursItr.hasNext()) {
                    Neighbour neighbour = neighboursItr.next();
                    if (neighbour.ipAddress.equalsIgnoreCase(neighbourIp) && neighbour.port == neighbourPort) {
                        neighboursItr.remove();
                    }
                }
                gui.refreshNeighbourList();

                NodeSender.sendUDP(myUDPSocket, Util.formMessage("LEAVEOK 0"), ip, port); //send to the source ip:port
                System.out.println("neighbour leaved: " + neighbourIp + " " + neighbourPort);

                if (neighbours.isEmpty()) { // if there are no neighbours available
                    System.out.println("No neighbours available. Re-registering with BS");
                    unregisterFromBS();
                    registerWithBS(); // register again to get some neighbours
                }

            } else if (keyword.equalsIgnoreCase("LEAVEOK")) { // received response to leave request
                switch (st.nextToken()) {
                    case "0": // successful
                        break;

                    case "9999": // error
                        break;
                }

            } else if (keyword.equalsIgnoreCase("SER")) { // received search request

                String searcherIp = st.nextToken();
                int searcherPort = Integer.parseInt(st.nextToken());

                String filename = ""; // filename sent by searcher
                String lastToken = "";
                while (st.hasMoreTokens()) {
                    filename += lastToken;
                    lastToken = st.nextToken() + " ";
                }
                filename = filename.substring(0, filename.length() - 1); // strip space at end
                lastToken = lastToken.substring(0, lastToken.length() - 1); // strip space at end

                int hops = Integer.parseInt(lastToken);
                hops = hops - 1; // decrement hop count

                String basicQuery = "SER " + searcherIp + " " + searcherPort + " " + filename;
                if (checkInRecentQueries(basicQuery)) {
                    System.out.println("query discarded");
                    return;
                }

                HashSet<String> availableFileNames = checkForFileAvailability(filename);
                int numberOfFiles = availableFileNames.size();

                if (numberOfFiles > 0) { // if i have similar files

                    String fileString = "";
                    for (String s : availableFileNames) {
                        s = s.replace(" ", "_"); // replace spaces in filename with underscore
                        fileString += s + " ";
                    }
                    fileString = fileString.substring(0, fileString.length() - 1); // strip last space

                    NodeSender.sendUDP(myUDPSocket, Util.formMessage("SEROK " + numberOfFiles + " "
                            + Util.IP + " " + Util.PORT + " " + hops + " " + fileString), searcherIp, searcherPort);

                } else { // if i don't have

                    NodeSender.sendUDP(myUDPSocket, Util.formMessage("SEROK 0 "
                            + Util.IP + " " + Util.PORT + " " + hops + " " + filename), searcherIp, searcherPort);

                    if (hops > 0) { // forward the search request to other nodes
                        forwardSearchRequest(myUDPSocket, searcherIp, searcherPort, filename, hops);
                    }
                }

            } else if (keyword.equalsIgnoreCase("SEROK")) { // received response to search request

                int numberOfFiles = Integer.parseInt(st.nextToken());
                if (numberOfFiles > 0) {
                    String locatedIp = st.nextToken(); // ip of the node which has the file
                    int locatedPort = Integer.parseInt(st.nextToken());
                    int hops = Integer.parseInt(st.nextToken());

                    while (st.hasMoreTokens()) {
                        String msg = "File found: " + st.nextToken() + " in " + locatedIp;
                        System.out.println(msg);
                        gui.textAreaResults.append(msg + " \n");
                    }
                } else {
                    switch (numberOfFiles) {
                        case 0:
                            System.out.println("file not found in node");
                            gui.textAreaResults.append("file not found in node \n");
                            break;
                        case 9999: // failure due to node unreachable
                            break;
                        case 9998: // some other error
                            break;
                    }
                }

            }

        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }

    }

    /**
     * Decode a received response from Bootstrap server and take relevant actions
     *
     * @param response
     */
    static void decodeServerResponse(String response) {

        StringTokenizer st = new StringTokenizer(response);
        try {
            int length = Integer.parseInt(st.nextToken());
            if (st.nextToken().equals("REGOK")) {
                switch (st.nextToken()) {
                    case "0": // request is successful, no nodes in the system
                        System.out.println("request is successful, no nodes in the system");
                        break;

                    case "1": // request is successful, 1 contacts will be returned
                        Neighbour n1 = new Neighbour(st.nextToken(), Integer.parseInt(st.nextToken()));
                        neighbours.add(n1);
                        gui.refreshNeighbourList();
                        System.out.println("neighbour added from BS. " + n1.ipAddress + " " + n1.port);
                        break;

                    // even though the sever sends multiple neighbours (due to a bug in server), first 2 are taken
                    case "4":
                    case "5":
                    case "6":
                    case "7":
                    case "8":
                    case "9":
                    case "10":
                    case "11":
                    case "12":
                    case "13":
                    case "14":
                    case "15":
                    case "16":
                    case "17":
                    case "18":
                    case "19":
                    case "20":
                    case "3":
                        List<Neighbour> obtainedNeighbours = new ArrayList<Neighbour>();
                        while ((st.hasMoreTokens())) {
                            Neighbour n = new Neighbour(st.nextToken(), Integer.parseInt(st.nextToken()));
                            st.nextToken(); // username
                            obtainedNeighbours.add(n);
                        }

                        for (int i = 0; i < 2; i++) { //add only 2 neighbours
                            Neighbour n = obtainedNeighbours.remove(new Random().nextInt(obtainedNeighbours.size()));
                            neighbours.add(n);
                            gui.refreshNeighbourList();
                            System.out.println("neighbour added from BS. " + n.ipAddress + " " + n.port);
                        }
                        break;

                    case "2": // request is successful, 2 nodes' contacts will be returned
                        Neighbour n2 = new Neighbour(st.nextToken(), Integer.parseInt(st.nextToken()));
                        st.nextToken(); // username
                        neighbours.add(n2);
                        gui.refreshNeighbourList();
                        System.out.println("neighbour added from BS. " + n2.ipAddress + " " + n2.port);

                        Neighbour n3 = new Neighbour(st.nextToken(), Integer.parseInt(st.nextToken()));
                        st.nextToken(); // username
                        neighbours.add(n3);
                        gui.refreshNeighbourList();
                        System.out.println("neighbour added from BS. " + n3.ipAddress + " " + n3.port);
                        break;

                    case "9999": // failed, there is some error in the command
                        System.out.println("9999: failed, there is some error in the command");
                        break;

                    case "9998": // failed, already registered to you, unregister first
                        System.out.println("9998: failed, already registered to you, unregister first");
                        break;

                    case "9997": // failed, registered to another user, try a different IP and port
                        System.out.println("9997: failed, registered to another user, try a different IP and port");
                        break;

                    case "9996":// failed, can’t register. BS full
                        System.out.println("9996: failed, can’t register. BS full");
                        break;

                    default:
                        System.out.println("BS response not identified");
                }

            } else if (st.nextToken().equals("UNROK")) {
                switch (st.nextToken()) {
                    case "0": // successful
                        System.out.println("Unregged from BS successfully");
                        break;
                    case "9999": // error while unregistering. IP and port may not be in the registry or command is incorrect.
                        System.out.println("Error while unregistering");
                        break;
                    default:
                        System.out.println("BS response not identified");
                }
            }

        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }

    }

    /**
     * Send the register message to the Bootstrap server
     */
    static void registerWithBS() {

        String response = BootstrapCommunicator.sendTCP(Util.BS_IP, Util.BS_PORT, Util.formMessage("REG " + Util.IP + " " + Util.PORT + " " + Util.USERNAME));
        Node.decodeServerResponse(response);
    }

    /**
     * Setup random 3 or 4 or 5 files to be available locally.
     */
    static void setupFiles() {

        int numberOfFiles = 3 + (new Random().nextInt(3)); //get a number between 3 and 5 (inclusive)

        ArrayList<Integer> idxs = new ArrayList<Integer>(); // to keep track of already added files
        idxs.add(-1);

        System.out.println("Locally available files in this node:");

        for (int i = 0; i < numberOfFiles; i++) {
            int idx = -1;
            while (idxs.contains(idx)) {
                idx = new Random().nextInt(20);
            }
            idxs.add(idx);
            localFiles.add(Util.allFiles.get(idx));
            System.out.println("\t" + Util.allFiles.get(idx));
        }
    }

}
