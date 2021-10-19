package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

public class ChannelMultiplexingServer {

	private static final int BUFF_CAPACITY = 128;
	private Selector selector;
	private ServerSocketChannel socketChannel;
	private SelectionKey key;
	private Set<SelectionKey> selectedKeys;
	private Iterator<SelectionKey> keysIterator;
	private Worth wServer;
	private int RMIport = 8888;
	private int TCPport = 7777;
	private List<InetAddress> availableChatAddress;
	private HashMap<String,InetAddress> projectChatMap;

	public ChannelMultiplexingServer() throws IOException {

		// creazione

		this.socketChannel = ServerSocketChannel.open();
		this.socketChannel.configureBlocking(false);
		this.socketChannel.socket().bind(new InetSocketAddress("localhost", TCPport));
		this.selector = Selector.open();
		this.socketChannel.register(selector, SelectionKey.OP_ACCEPT);
		this.selectedKeys = null;
		this.availableChatAddress = new ArrayList<InetAddress>();
		for (int i = 1; i < 255; i++) {
			availableChatAddress.add(InetAddress.getByName("224.0.0"+i));
			projectChatMap = new HashMap<String,InetAddress>();
			
		}
		//System.out.println(availableChatAddress);
		// this.keysIterator = selectedKeys.iterator();

		// Register con RMI
		
		wServer = new Worth();

		ServiceRMI stub = (ServiceRMI) UnicastRemoteObject.exportObject(wServer, 9999);

		LocateRegistry.createRegistry(RMIport);
		Registry r = LocateRegistry.getRegistry(RMIport);

		r.rebind("WORTH-SERVER", stub);
	}
	
	

	public void start() throws IOException {

		while (true) {

			int n = selector.select();
			if (n == 0)
				continue;
			this.selectedKeys = selector.selectedKeys();
			this.keysIterator = selectedKeys.iterator();

			while (keysIterator.hasNext()) {
				key = (SelectionKey) keysIterator.next();
				keysIterator.remove();
				if (key.isAcceptable()) {
					accept();
				} else if (key.isReadable()) {
					read();
				} else if (key.isWritable()) {
					write();
				}
			}

		}
	}

	private void accept() throws IOException {

		// apro la connessione
		// prendo il socketchannel che sta provando a connettersi

		SocketChannel scClient = socketChannel.accept();
		if (scClient == null)
			return;
		scClient.configureBlocking(false);

		// registro la key su readable
		scClient.register(selector, SelectionKey.OP_READ);
		// informo che ho accettato una nuova connessione

		System.out.println("accettata nuova connessione dal client");

		/*
		 * SocketChannel sc; ssc = (ServerSocketChannel) key.channel(); sc=ssc.accept();
		 * if (sc == null) continue; System.out.println("Receiving connection");
		 * bb.clear(); bb.put("HelloClient\n".getBytes()); bb.flip();
		 * System.out.println("Writing message to client"); while (bb.hasRemaining())
		 * sc.write(bb); sc.close(); } it.remove();
		 */

		// server close?

	}

	private void write() throws IOException {

		// System.out.println("ESEGUO LA WRITE");
		String reply = new String();
		ByteBuffer buffer = ByteBuffer.allocate(BUFF_CAPACITY);
		SocketChannel scClient = (SocketChannel) key.channel();
		String attach = (String) key.attachment();
		String command = new String();
		command = "";
		ArrayList<String> parameters = new ArrayList<String>();

		StringTokenizer st = new StringTokenizer(attach);
		command = st.nextToken();
		while (st.hasMoreTokens()) {
			if (command != "")
				parameters.add(st.nextToken());

		}
		System.out.println(command);
		System.out.println(parameters);
		switch (command) {

		case "login":
			reply = wServer.login(parameters.get(0), parameters.get(1));
			 ArrayList<String> projects = wServer.listMyProjects(parameters.get(0));
			 for(String p : projects) {
				 if(projectChatMap.get(p) == null) {
					 InetAddress a = availableChatAddress.get(0);
					 projectChatMap.put(parameters.get(0), a);
					 wServer.sendNewChatAddress(parameters.get(0), p,a);
				 }
				 else 
					 wServer.sendNewChatAddress(parameters.get(0), p, projectChatMap.get(p));
				 
			 }

			break;

		case "list_users":
			reply = (wServer.listUsers()).toString();
			break;

		case "list_online_users":
			reply = (wServer.listOnlineUsers()).toString();
			break;

		case "list_projects":
			reply = (wServer.listProjects()).toString();
			break;

		case "list_my_projects":
			reply = (wServer.listMyProjects(parameters.get(0))).toString();
			break;

		case "create_project":
			try {
				InetAddress chatIP = availableChatAddress.remove(0);
			reply = wServer.createProject(parameters.get(0), parameters.get(1), chatIP);
			if(reply.contains("successfully")) this.projectChatMap.put(parameters.get(1), chatIP);
			} catch( IndexOutOfBoundsException e) {
			reply = "no more chat address available";
			}
			
			break;

		case "add_member":
			InetAddress chatIP = projectChatMap.get(parameters.get(1));
			reply = wServer.addMember(parameters.get(0), parameters.get(1), parameters.get(2), chatIP);
		
			break;

		case "show_members":
			reply = wServer.showMembers(parameters.get(0), parameters.get(1));
			break;

		case "show_cards":
			reply = wServer.showCards(parameters.get(0), parameters.get(1));
			break;

		case "show_card":
			reply = wServer.showCard(parameters.get(0), parameters.get(1), parameters.get(2));
			break;

		case "add_card":
			reply = wServer.addCard(parameters.get(0), parameters.get(1), parameters.get(2), parameters.get(3));
			break;

		case "move_card":
			// index out of bound exc
			try {
				for (CardState from : CardState.values()) {
					if (from.name().equals(parameters.get(3))) {
						for (CardState to : CardState.values()) {
							if (to.name().equals(parameters.get(4))) {
								reply = wServer.moveCard(parameters.get(0), parameters.get(1), parameters.get(2), from,
										to);
								break;
							}
						}
						break;
					}
				}
			} catch (IndexOutOfBoundsException e) {
				////// scrivi
			}
			break;

		case "get_card_history":
			reply = wServer.getCardHistory(parameters.get(0), parameters.get(1), parameters.get(2));
			break;

		case "read_chat":
			wServer.readChat(parameters.get(0), parameters.get(1));
			break;
		case "send_chat_message":
			wServer.sendChatMsg(parameters.get(0), parameters.get(1), parameters.get(2));
			break;
		case "cancel_project":
			reply = wServer.cancelProject(parameters.get(0), parameters.get(1));
			if(reply.contains("deleted")) {
				
				InetAddress a = projectChatMap.remove(parameters.get(1));
				if(!(a == null)) availableChatAddress.add(a);
				
				// non dovrebbe essere nulla l'ho cancellato quindi esisteva
			}
			break;
		case "logout":
			reply = wServer.logout(parameters.get(0));

			key.channel().close();
			key.cancel();

			break;

		default: {
			reply = "Command or parameters of this request are not valid";

		}
		}

		int bytesToWrite = scClient.write(ByteBuffer.wrap(reply.getBytes()));

		// int bytesToWrite= scClient.write(buffer);

		if (bytesToWrite == -1) {
			key.cancel();
			scClient.close();

			// connection interrupted TOGLI ONLINE
		} else {
			if (buffer.position() == BUFF_CAPACITY) {

				buffer.flip();
				// attach = attach + new String(buffer.array(), "ASCII");
				key.interestOps(SelectionKey.OP_WRITE);
				// CLEAR?
			} else {
				key.interestOps(SelectionKey.OP_READ);
				buffer.clear();
				key.attach(null);
				// attach = attach + new String(buffer.array(), "ASCII");
			}

		}

	}

	private void read() throws IOException {

		System.out.println("Eseguo la read");

		ByteBuffer buffer = ByteBuffer.allocate(BUFF_CAPACITY);
		SocketChannel scClient = (SocketChannel) key.channel();
		String attach = (String) key.attachment();
		StringBuffer sb = new StringBuffer();

		// System.out.println(sb);
		// String data;
		// if(attach == null ) data = "";
		// else data = (String) attach;

		buffer.clear();

		int bytesRead = scClient.read(buffer);

		int position = buffer.position();
		buffer.rewind();
		// String filename = "";
		int startPosition = 0;

		// StringBuilder sb = new StringBuilder();

		while (startPosition < position) {
			sb.append((char) buffer.get());
			startPosition++;
		}

		attach = sb.toString();

		System.out.println(bytesRead + "\n");

		if (bytesRead == -1 && key.attachment() == null) {
			// if(attach == null || attach == "") { //NULL
			key.cancel();
			scClient.close();

			// connection interrupted TOGLI ONLINE
			/*
			 * } else { key.interestOps(SelectionKey.OP_WRITE).attach(attach); attach =
			 * attach + StandardCharsets.US_ASCII.decode(buffer).toString();
			 */
		} else {
			if (position < BUFF_CAPACITY) {

				/*
				 * if(attach!= null) attach = attach.concat(new String(buffer.array(),
				 * "ASCII")); else attach = new String(buffer.array(), "ASCII");
				 */

				key.attach(attach);
				key.interestOps(SelectionKey.OP_WRITE);
				// CLEAR?
				buffer.flip();
			} else {
				/*
				 * if(attach!= null) attach = attach.concat(new String(buffer.array(),
				 * "ASCII")); /* buffer.toString(); else attach = new String(buffer.array(),
				 * "ASCII");
				 */

				key.attach(attach);
				key.interestOps(SelectionKey.OP_READ);
			}

		}

	}

}