import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.UUID;

public class ChatClient {

    String serverAddress;
    int port;
    Scanner in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(50);
    JTextArea messageArea = new JTextArea(16, 50);
    JButton sendButton = new JButton("Send");
    boolean isCoordinator = false; // Flag to indicate if this client is the coordinator

    public ChatClient() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // If Nimbus is not available, fall back to another look and feel.
        }

        textField.setEditable(false);
        messageArea.setEditable(false);
        messageArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);

        frame.getContentPane().setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        inputPanel.add(textField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        frame.getContentPane().add(inputPanel, BorderLayout.SOUTH);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        frame.pack();

        // Action for the text field and send button
        ActionListener sendListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                out.println(textField.getText());
                textField.setText("");
            }
        };

        textField.addActionListener(sendListener);
        sendButton.addActionListener(sendListener);
    }

    private String getServerAddress() {
        return JOptionPane.showInputDialog(
                frame,
                "Enter the Server IP:",
                "Welcome to the Chatter",
                JOptionPane.PLAIN_MESSAGE
        );
    }

    private int getServerPort() {
        while (true) {
            String portString = JOptionPane.showInputDialog(
                    frame,
                    "Enter the Server Port:",
                    "Welcome to the Chatter",
                    JOptionPane.PLAIN_MESSAGE
            );

            if (portString == null) {
                System.exit(0); // Handle cancellation
            }

            try {
                int port = Integer.parseInt(portString);
                if (port == 59001) {
                    return port;
                } else {
                    JOptionPane.showMessageDialog(frame, "Invalid port. You must enter 59001.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(frame, "Invalid port number. Please enter a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String getName() {
        String name = JOptionPane.showInputDialog(
                frame,
                "Choose a screen name:",
                "Screen name selection",
                JOptionPane.PLAIN_MESSAGE
        );
        if (name == null || name.trim().isEmpty()) {
            return "Anonymous" + getUniqueID(); // Fallback for empty names
        }
        return name.trim() + "#" + getUniqueID(); // Append a unique ID to the name
    }

    private String getUniqueID() {
        return UUID.randomUUID().toString().substring(0, 5); // Generate a short unique identifier
    }

    private void run() throws IOException {
        serverAddress = getServerAddress();
        port = getServerPort();
        Socket socket = new Socket(serverAddress, port);
        in = new Scanner(socket.getInputStream());
        out = new PrintWriter(socket.getOutputStream(), true);

        while (in.hasNextLine()) {
            String line = in.nextLine();
            if (line.startsWith("SUBMITNAME")) {
                out.println(getName());
            } else if (line.startsWith("NAMEACCEPTED")) {
                textField.setEditable(true);
                frame.setTitle("Chatter - " + line.substring(13) + (isCoordinator ? " (Coordinator)" : ""));
            } else if (line.startsWith("MESSAGE")) {
                messageArea.append(line.substring(8) + "\n");
            } else if (line.startsWith("COORDINATOR")) {
                isCoordinator = true;
                frame.setTitle(frame.getTitle() + " (Coordinator)");
                messageArea.append("You are the coordinator.\n");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}
