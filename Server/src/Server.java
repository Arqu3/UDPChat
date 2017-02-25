
//
// Source file for the server side. 
//
// Created by Sanny Syberfeldt
// Maintained by Marcus Brohede
//

import java.io.IOException;
import java.net.*;
//import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

public class Server {

	private ArrayList<ClientConnection> m_connectedClients = new ArrayList<ClientConnection>();
	private DatagramSocket m_socket;
	private byte[] m_OutBuf = new byte[4096];
	private byte[] m_InBuf = new byte[4096];
	private DatagramPacket m_InPacket;
	private DatagramPacket m_OutPacket;
	private String m_ListOfClients = "";
	private String m_SentMessage = "";
	//private boolean m_HasSentMessage = false;
	//private boolean m_HasConfirmedMessage = false;
	private int m_TimeOut = 250;
	private ArrayList<String> m_ReplyNames = new ArrayList<String>();

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.err.println("Usage: java Server portnumber");
			System.exit(-1);
		}
		try {
			Server instance = new Server(Integer.parseInt(args[0]));
			instance.listenForClientMessages();
		} catch (NumberFormatException e) {
			System.err.println("Error: port number must be an integer.");
			System.exit(-1);
		}
	}

	private Server(int portNumber) throws IOException {
		// TODO: create a socket, attach it to port based on portNumber, and
		// assign it to m_socket
		m_socket = new DatagramSocket(portNumber);
		m_socket.setSoTimeout(m_TimeOut);
	}

	private void listenForClientMessages() throws IOException {
		System.out.println("Waiting for client messages... ");
		while(true)
		{
			// TODO: Listen for client messages.
			m_InPacket = new DatagramPacket(m_InBuf, m_InBuf.length);
			try
			{
				m_socket.receive(m_InPacket);
			}
			catch (SocketTimeoutException e) 
			{
				if (m_SentMessage.split(" ").length > 3)
				{
					for (int i = 0; i < m_ReplyNames.size(); i++)
					{
						sendPrivateMessage(m_SentMessage, m_ReplyNames.get(i));
						System.out.println(m_SentMessage + " : " + m_ReplyNames.get(i));	
					}	
				}
				//String replies[] = m_SentMessage.split(" ");
				/*if (m_HasSentMessage && !m_HasConfirmedMessage)
				{
					//sendPrivateMessage(m_SentMessage, replies[2]);
				}*/
				continue;
			}
			
			// On reception of message, do the following:
			// * Unmarshal message
			// * Depending on message type, either
			// - Try to create a new ClientConnection using addClient(), send
			// response message to client detailing whether it was successful
			String indata = new String(m_InBuf, 0, m_InPacket.getLength());
			System.out.println("Server received message: " + indata);
			String inputs[] = indata.split(" ");
			
			//Check if message has not been confirmed and a message has been sent
			/*if (m_HasSentMessage && !m_HasConfirmedMessage)
			{
				//Compare ID with response, if correct then done
				for (int i = 0; i < m_ReplyNames.size(); i++)
				{
					if (inputs[2].equals(m_ReplyNames.get(i)))
					{
						m_HasSentMessage = false;
						m_HasConfirmedMessage = true;
						m_SentMessage = "";
						System.out.println("success 1");
						break;
					}		
				}	
			}*/
			String replyID = inputs[0];
			//Client must be connected
			if (inputs[1].contains("true"))
			{
				String name = inputs[2];
				//Is command?
				if (inputs.length > 3)
				{
					if (inputs[3].startsWith("/"))
					{
						String command = inputs[3].substring(1, inputs[3].length());
						//Check if something is written after /
						if (command.length() > 0)
						{
							//Connect command
							if (command.equals("Connect"))
							{
								//If client is already connected and attempts to connect again
								String reply = replyID + " true " + name + " You are already connected!";
								sendPrivateMessage(reply, name);
							}
							else if (command.equals("List"))
							{
								//Display list to requested client
								String reply = replyID + " true " + name + " List of connected clients:\n" + getList();
								sendPrivateMessage(reply, name);
							}
							else if (command.equals("Disconnect"))
							{
								//Disconnect requested client
								if (removeClient(name))
								{
									String reply = replyID + " false " + name + " You were disconnected";
									m_OutBuf = reply.getBytes();
									m_OutPacket = new DatagramPacket(m_OutBuf, m_OutBuf.length, m_InPacket.getAddress(), m_InPacket.getPort());
									m_socket.send(m_OutPacket);
									
									broadcast(replyID + " true " + name +" disconnected");
								}
								else
								{
									System.out.println("Could not disconnect client with name " + name);
								}
							}
							// - Send a private message to a user using sendPrivateMessage()
							else if (command.equals("Whisper") || command.equals("whisper") || command.equals("W") || command.equals("w"))
							{
								//Whisper another client
								String toName = inputs[4];
								//Check if client exists
								if (CheckIfClientExists(toName))
								{
									//Success, send message back to client that whispered
									String combinedReply = "";
									for (int i = 5; i < inputs.length; i++)
									{
										//Recreate message sent from client and send to receiver
										combinedReply += inputs[i];
										if (i < inputs.length)
											combinedReply += " ";
									}
									//Send back confirmation to client that whispered
									String sendBack = replyID + " true " + name + " To " + toName + ": " + combinedReply;
									sendPrivateMessage(sendBack, name);
									
									//Send message to client that was whispered
									String sendTo = replyID + " true " + toName + " From " + name + ": " +combinedReply;
									sendPrivateMessage(sendTo, toName);
								}
								else
								{
									//Client does not exist
									String sendBack = replyID + " true " + name + " Client with name " + toName + " does not exist";
									sendPrivateMessage(sendBack, name);
								}
							}
							else
							{
								//Command was invalid
								String reply = replyID + " true " + name + " ERROR, the command " + command + " is unknown";
								sendPrivateMessage(reply, name);
							}	
						}
						else
						{
							//Command was null
						}
					}
					else if (inputs[3].equals("From"))
					{
						//Message received was a whisper
					}
					// - Broadcast the message to all connected users using broadcast()
					else
					{
						//Was not a command
						String reply = new String(m_InBuf, 0, m_InPacket.getLength());
						System.out.println("Broadcasted: " + reply);
						broadcast(reply);
					}
				}
				else
				{
					//Message received was an ack
					for (int i = 0; i < m_ReplyNames.size(); i++)
					{
						if (inputs[2].equals(m_ReplyNames.get(i)))
						{
							//m_HasSentMessage = false;
							//m_HasConfirmedMessage = true;
							m_SentMessage = "";
							m_ReplyNames.remove(i);
							System.out.println("success ack");
							break;
						}		
					}
				}
			}
			else
			{
				//Client is not connected
				if (inputs[3].contains("/Connect"))
				{
					if (addClient(inputs[2], m_InPacket.getAddress(), m_InPacket.getPort()))
					{
						String reply = replyID + " true Welcome " + inputs[2];
						m_OutBuf = reply.getBytes();
						m_OutPacket = new DatagramPacket(m_OutBuf, m_OutBuf.length, m_InPacket.getAddress(), m_InPacket.getPort());
						m_socket.send(m_OutPacket);
						
						broadcast(replyID + " true " + inputs[2] + " connected!");
					}
					else
					{
						String reply = replyID +" false Could not connect, type /Connect to connect";
						m_OutBuf = reply.getBytes();
						m_OutPacket = new DatagramPacket(m_OutBuf, m_OutBuf.length, m_InPacket.getAddress(), m_InPacket.getPort());
						m_socket.send(m_OutPacket);
						
						m_OutPacket = new DatagramPacket(m_OutBuf, m_OutBuf.length, m_InPacket.getAddress(), m_InPacket.getPort());
						m_socket.send(m_OutPacket);
					}
				}
				else
				{
					//Tried to type something when wasn't connected
					String reply = replyID + " false You are not connected, type /Connect to connect";
					m_OutBuf = reply.getBytes();
					m_OutPacket = new DatagramPacket(m_OutBuf, m_OutBuf.length, m_InPacket.getAddress(), m_InPacket.getPort());
					m_socket.send(m_OutPacket);
				}
			}
			
			//Check if message has not been confirmed and a message has been sent
			//This code is to try and prevent sending duplicates
			/*if ((m_HasSentMessage && !m_HasConfirmedMessage))
			{
				String input = new String(m_InBuf, 0, m_InPacket.getLength());
				String inputs1[] = input.split(" ");
				//Compare ID with response, if correct then done
				for (int i = 0; i < m_ReplyNames.size(); i++)
				{
					if (inputs1[2].equals(m_ReplyNames.get(i)))
					{
						m_HasSentMessage = false;
						m_HasConfirmedMessage = true;
						m_SentMessage = "";
						System.out.println("success 2");
						break;
					}		
				}
			}*/
		}
	}

	public boolean addClient(String name, InetAddress address, int port) {
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			if (c.hasName(name)) {
				System.out.println(name + " already exists!");
				return false; // Already exists a client with this name
			}
		}
		System.out.println(name + " successfully added to connections");
		m_connectedClients.add(new ClientConnection(name, address, port));
		return true;
	}
	
	public boolean removeClient(String name)
	{
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			if (c.hasName(name)) {
				System.out.println("Removed client " + name);
				itr.remove();
				return true; //Remove client
			}
		}
		//Could not find client to remove
		System.out.println("Client with name " + name + " does not exist!");
		return false;
	}
	
	public boolean CheckIfClientExists(String name)
	{
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			if (c.hasName(name)) {
				System.out.println("Client with name: " + name + " exists");
				return true;
			}
		}
		//Could not find client to remove
		System.out.println("Client with name " + name + " does not exist!");
		return false;
	}

	public boolean sendPrivateMessage(String message, String name) throws IOException {
		m_SentMessage = message;
		/*if (!m_HasSentMessage)
		{
			
		}
		m_HasSentMessage = true;
		m_HasConfirmedMessage = false;*/
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			if (c.hasName(name)) {
				AddToNameList(name);
				c.sendMessage(message, m_socket);
				System.out.println("Server sent message: " + message + " to name " + name);
			}
		}
		return false;
	}

	public void broadcast(String message) throws IOException {
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			itr.next().sendMessage(message, m_socket);
			m_SentMessage = message;
			AddToNameList(message.split(" ")[2]);
			//m_HasSentMessage = true;
			//m_HasConfirmedMessage = false;
		}
	}
	
	public String getList() {
		m_ListOfClients = "";
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			m_ListOfClients += itr.next().getName() + "\n";
		}
		return m_ListOfClients.substring(0, m_ListOfClients.length() - 1);
	}
	
	void AddToNameList(String name)
	{
		if (!m_ReplyNames.contains(name))
		{
			System.out.println("Added: " + name + " to name list");
			m_ReplyNames.add(name);	
		}
	}
}
