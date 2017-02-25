import java.awt.event.*;
import java.io.IOException;
//import java.io.*;

public class Client implements ActionListener {

	private String m_name = null;
	private final ChatGUI m_GUI;
	private ServerConnection m_connection = null;

	public static void main(String[] args) throws IOException {
		if (args.length < 3) {
			System.err.println("Usage: java Client serverhostname serverportnumber username");
			System.exit(-1);
		}

		try {
			Client instance = new Client(args[2]);
			instance.connectToServer(args[0], Integer.parseInt(args[1]));
		} catch (NumberFormatException e) {
			System.err.println("Error: port number must be an integer.");
			System.exit(-1);
		}
	}

	private Client(String userName) {
		m_name = userName;

		// Start up GUI (runs in its own thread)
		m_GUI = new ChatGUI(this, m_name);
	}

	private void connectToServer(String hostName, int port) throws IOException {
		// Create a new server connection
		m_connection = new ServerConnection(hostName, port);
		m_connection.SetName(m_name);
		if (m_connection.handshake(m_name)) {
			listenForServerMessages();
		}
	}

	private void listenForServerMessages() throws IOException {
		// Use the code below once m_connection.receiveChatMessage() has been
		// implemented properly.
		do {
			//Only display message if it isn't empty
			String msg = m_connection.receiveChatMessage();
			if (!msg.equals(""))
				m_GUI.displayMessage(msg);	
			
		} while(true);
	}

	// Sole ActionListener method; acts as a callback from GUI when user hits
	// enter in input field
	@Override
	public void actionPerformed(ActionEvent e) {
		// Since the only possible event is a carriage return in the text input
		// field,
		// the text in the chat input field can now be sent to the server.
		String send = m_GUI.getInput();
		try {
			if (!send.equals(""))
				m_connection.sendChatMessage(send);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		m_GUI.clearInput();
	}
}
