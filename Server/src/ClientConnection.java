
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

/**
 * 
 * @author brom
 */
public class ClientConnection {

	static double TRANSMISSION_FAILURE_RATE = 0.3;

	private final String m_name;
	private final InetAddress m_address;
	private final int m_port;
	private byte[] m_OutBuf = new byte[4096];
	private DatagramPacket m_OutPacket;

	public ClientConnection(String name, InetAddress address, int port) {
		m_name = name;
		m_address = address;
		m_port = port;
	}

	public void sendMessage(String message, DatagramSocket socket) throws IOException 
	{
		Random generator = new Random();
		double failure = generator.nextDouble();

		if (failure > TRANSMISSION_FAILURE_RATE) 
		{
			// TODO: send a message to this client using socket.
			m_OutBuf = message.getBytes();
			m_OutPacket = new DatagramPacket(m_OutBuf, m_OutBuf.length, m_address, m_port);
			
			socket.send(m_OutPacket);
		} 
		else 
		{
			// Message got lost
			System.out.println("The message '" + message + "' was lost!");
		}
	}

	public boolean hasName(String testName) {
		return testName.equals(m_name);
	}

	public String getName()
	{
		return m_name;
	}
}
