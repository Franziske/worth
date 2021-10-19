package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import server.ServiceRMI;
import server.Worth;

public class Connections {

	// String nik;
	int RMIport;
	int TCPport;
	private InetSocketAddress addressTCP, addressRMI;
	private static SocketChannel scClientTCP, scClientRMI;
	private ByteBuffer buffer;
	private DatagramSocket UDPSocket;

	public Connections() {

		this.RMIport = 8888;
		this.TCPport = 7777;
		this.addressTCP = new InetSocketAddress("localhost", TCPport);
		this.buffer = ByteBuffer.allocate(2048);
		
		try {
			scClientTCP = SocketChannel.open();
			scClientTCP.connect(addressTCP);
			this.UDPSocket = new DatagramSocket();
			System.out.println("CONNECTED");
		} catch (IOException e) {

			System.out.println("IO EXCEPT");
		}
	}

	public boolean register(String nik, String psswd) {
		try {
			Registry reg = LocateRegistry.getRegistry(RMIport);
			Remote remoteObject = reg.lookup("WORTH-SERVER");
			ServiceRMI serverObject = (ServiceRMI) remoteObject;

			boolean registration = serverObject.register(nik, psswd);
			// if (registration) this.nik = nik;

			return registration;

		} catch (RemoteException e) {

			return false;
		} catch (NotBoundException e) {
			return false;
		}
	}

	public String sendRequest(String req) {
		try {
			buffer = ByteBuffer.wrap(req.getBytes());

			scClientTCP.write(buffer);

			buffer.clear();
			buffer.flip();

			buffer = ByteBuffer.allocate(2048);
			scClientTCP.read(buffer);

			buffer.clear();
			buffer.flip();

			String response = new String(buffer.array(), "ASCII");
			/*
			 * buffer.clear(); buffer.flip();
			 */

			return response;
		} catch (IOException e) {
			return " I/O exception occurred";
		}

	}

	public void sendChatMsg(String msg, InetAddress chatAddress) throws IOException {
		System.out.println("tento di inviare " + msg);
		
		//InetAddress mcIPAddress = InetAddress.getByName(chatAddress);
		byte[] buffer = msg.getBytes();
	    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
	    packet.setAddress(chatAddress);
	    packet.setPort(9991);
	    UDPSocket.send(packet);
	    System.out.println(msg + " inviato.");
    	UDPSocket.close();
		
	}

}
