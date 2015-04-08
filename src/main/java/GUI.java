import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by Tharindu Wijewardane on 2015-04-08.
 */
public class GUI {

    private static JFrame frame;

    private JPanel panel1;
    private JTextField textField1;
    private JButton searchButton;
    JTextArea textAreaResults;
    private JTextArea textAreaMyFiles;
    private JTextArea textAreaMyNeighbours;
    private JButton joinButton;
    private JButton leaveButton;
    private JTextField textFieldBsIp;
    private JTextField textFieldBsPort;
    private JTextField textFieldMyPort;
    private JTextField textFieldMyIp;
    private JTextField textFieldMyUsername;

    static DatagramSocket myUDPSocket;

    public GUI() {

        textFieldBsIp.setText(Util.BS_IP);
        textFieldBsPort.setText(Integer.toString(Util.BS_PORT));
        textFieldMyIp.setText(Util.IP);
        textFieldMyPort.setText(Integer.toString(Util.PORT));
        textFieldMyUsername.setText(Util.USERNAME);

        Node.setupFiles();
        textAreaMyFiles.setText("");
        for (String s : Node.localFiles) {
            textAreaMyFiles.append(s + " \n");
        }

        joinButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                Util.BS_IP = textFieldBsIp.getText();
                Util.BS_PORT = Integer.parseInt(textFieldBsPort.getText());
                Util.IP = textFieldMyIp.getText();
                Util.PORT = Integer.parseInt(textFieldMyPort.getText());
                Util.USERNAME = textFieldMyUsername.getText();

                textFieldBsIp.setEnabled(false);
                textFieldBsPort.setEnabled(false);
                textFieldMyIp.setEnabled(false);
                textFieldMyPort.setEnabled(false);
                textFieldMyUsername.setEnabled(false);

                Node.registerWithBS();

                textAreaMyNeighbours.setText("");
                for (Neighbour n : Node.neighbours) {
                    textAreaMyNeighbours.append(n.ipAddress + " : " + n.port + " \n");
                }

                Node.joinDistributedSystem(myUDPSocket);

                joinButton.setEnabled(false);
                leaveButton.setEnabled(true);
                searchButton.setEnabled(true);
            }
        });
        leaveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Node.leaveDistributedSystem(myUDPSocket);
                Node.unregisterFromBS();

                joinButton.setEnabled(true);
                leaveButton.setEnabled(false);
                searchButton.setEnabled(false);

                textFieldBsIp.setEnabled(true);
                textFieldBsPort.setEnabled(true);
                textFieldMyIp.setEnabled(true);
                textFieldMyPort.setEnabled(true);
                textFieldMyUsername.setEnabled(true);
            }
        });
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String filename = textField1.getText();
                textAreaResults.setText("");
                if (Node.localFiles.contains(filename)) {
                    textAreaResults.append("Exact file available locally. No need to search \n");
                } else {
                    textAreaResults.append("Exact file not available locally \n");
                    HashSet<String> available = Node.checkForFileAvailability(filename);
                    for (String name : available) {
                        textAreaResults.append("Similar file available locally: " + name + " \n");
                    }
                    textAreaResults.append("Sending the file search query to distributed system \n");
                    Node.searchFile(myUDPSocket, filename);
                }
            }
        });

        leaveButton.setEnabled(false);
        searchButton.setEnabled(false);

        Node.gui = this;
    }

    public static void init() throws SocketException {

//        try {
//            UIManager.setLookAndFeel(new SyntheticaBlackStarLookAndFeel());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        frame = new JFrame("Node");
        frame.setContentPane(new GUI().panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack(); // packs the window according to components inside. this is not removed because its required to correct layouts

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setBounds(0, 0, screenSize.width * 1 / 2, screenSize.height * 2 / 3);

        frame.setVisible(true);

        myUDPSocket = new DatagramSocket(Util.PORT); //the port used for all UDP communication
    }

}
