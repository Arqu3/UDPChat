
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Random;

/**
 *
 * @author brom
 */
public class ServerConnection {

	// Artificial failure rate of 30% packet loss
	static double TRANSMISSION_FAILURE_RATE = 0.3;

	private DatagramSocket m_socket = null;
	private InetAddress m_serverAddress = null;
	private int m_serverPort = -1;
	private byte[] m_OutBuf = new byte[4096];
	private byte[] m_InBuf = new byte[4096];
	private DatagramPacket m_InPacket;
	private DatagramPacket m_OutPacket;
	private boolean m_IsConnected = false;
	private int m_MessageID = 0;
	private boolean m_HasSentMessage = false;
	private boolean m_HasConfirmedMessage = true;
	private String m_SentMessage = "";
	private int m_Timeout = 250;
	private String m_name = "";

	public ServerConnection(String hostName, int port) 
	{
		m_serverPort = port;
		// TODO:
		// * get address of host based on parameters and assign it to
		// m_serverAddress
		try {
			m_serverAddress = InetAddress.getByName(hostName);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// * set up socket and assign it to m_socket
		try {
			m_socket = new DatagramSocket();
			m_socket.setSoTimeout(m_Timeout);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public boolean handshake(String name) throws IOException 
	{
		// TODO:
		// * marshal connection message containing user name
		// * send message via socket
		if (!m_HasSentMessage)
		{
			m_MessageID++;
		}
		m_HasSentMessage = true;
		String id = Integer.toString(m_MessageID);
		String send = id + " " + GetConnected() + " " + m_name + " /Connect";
		m_SentMessage = send;
		
		m_OutBuf = m_SentMessage.getBytes();
		m_OutPacket = new DatagramPacket(m_OutBuf, m_OutBuf.length, m_serverAddress, m_serverPort);
		
		try {
			m_socket.send(m_OutPacket);
		} catch (IOException e) {
			System.out.println("Unable to send packet " + e.toString());
		}
		
		m_InPacket = new DatagramPacket(m_InBuf, m_InBuf.length);
		try 
		{
			m_socket.receive(m_InPacket);
			System.out.println("Packet received");
			m_HasSentMessage = false;
			m_SentMessage = "";
		} 
		catch (SocketTimeoutException e) 
		{
			System.out.println("Handshake was timed out\n");
		}
		
		// * receive response message from server
		// * unmarshal response message to determine whether connection was
		// successful
		// * return false if connection failed (e.g., if user name was taken)
		String input = new String(m_InBuf, 0, m_InPacket.getLength());
		String inputs[] = input.split(" ");
		if (inputs[2].contains("Welcome"))
		{
			m_IsConnected = true;
			return true;
		}
		else
		{
			m_IsConnected = false;
			return false;
		}
	}

	public String receiveChatMessage() throws IOException
	{
		// TODO:
		// * receive message from server
		// * unmarshal message if necessary
		m_InPacket = new DatagramPacket(m_InBuf, m_InBuf.length);
		try 
		{
			m_socket.receive(m_InPacket);
			System.out.println("Packet received\n");
		} 
		catch (SocketTimeoutException e) 
		{
			//If message was timed out & an unconfirmed message was sent
			if (m_HasSentMessage && !m_HasConfirmedMessage)
			{
				System.out.println("Acknowledgement of message was not received, retrying\n");
				sendChatMessage(m_SentMessage);	
			}
			return "";
		}
		
		String input = new String(m_InBuf, 0, m_InPacket.getLength());
		String inputs[] = input.split(" ");
		
		//Check if message has not been confirmed and a message has been sent
		if (m_HasSentMessage && !m_HasConfirmedMessage)
		{
			//Compare ID with response, if correct then done
			if (inputs[0].equals(Integer.toString(m_MessageID)))
			{
				m_HasSentMessage = false;
				m_HasConfirmedMessage = true;
				m_SentMessage = "";
				System.out.println("success\n");
			}
		}
		else
		{
			//Send ack
			sendChatMessage("");
		}
		
		//Handle if client is connected or not
		if (!m_IsConnected)
		{
			if (inputs[1].contains("true"))
				m_IsConnected = true;
		}
		else
		{
			if (inputs[1].contains("false"))
				m_IsConnected = false;
		}
		
		//Format output differently depending on input
		String returnString = "";
		if (inputs[1].contains("true") || inputs[1].contains("false"))
		{
			if (inputs[2].equals(m_name))
				returnString = input.substring(inputs[0].length() + inputs[1].length() + inputs[2].length() + 3, input.length());
			else
				returnString = input.substring(inputs[0].length() + inputs[1].length() + 2, input.length());
		}
		else 
			returnString = input.substring(inputs[0].length() + 1, input.length());
		
		//If the received message was an acknowledgement, do not print it
		if (inputs.length == 3)
		{
			System.out.print("Input was an ack, returning");
			m_HasConfirmedMessage = true;
			m_HasSentMessage = false;
			m_SentMessage = "";
			return "";
		}

		// Note that the main thread can block on receive here without
		// problems, since the GUI runs in a separate thread

		// Update to return message contents
		return returnString;
	}

	public void sendChatMessage(String message) throws IOException 
	{
		Random generator = new Random();
		if (!m_HasSentMessage)
		{
			m_MessageID++;
			String send = Integer.toString(m_MessageID) + " " + GetConnected() + " " + m_name + " " + message;
			m_SentMessage = send;
		}

		double failure = generator.nextDouble();
		if (failure > TRANSMISSION_FAILURE_RATE) 
		{
			// TODO:
			// * marshal message if necessary
			// * send a chat message to the server
			m_OutBuf = m_SentMessage.getBytes();
			System.out.println("Sent message is: " + m_SentMessage + "\n");
			m_OutPacket = new DatagramPacket(m_OutBuf, m_OutBuf.length, m_serverAddress, m_serverPort);
			m_socket.send(m_OutPacket);	
		} 
		else 
		{
			//Message got lost
			System.out.println("The message: '" + m_SentMessage + "' was lost!\n");
		}
		//If message sent was not an ack
		if (m_SentMessage.split(" ").length > 3)
		{
			m_HasSentMessage = true;
			m_HasConfirmedMessage = false;	
		}
	}
	
	//Return different string depending on if client is connected
	public String GetConnected()
	{
		if (m_IsConnected)
			return "true";
		else
			return "false";
	}

	public void SetName(String name)
	{
		if (m_name.length() < 1)
			m_name = name;
	}
}
