package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import server.UserState;

class ChatData {

	private Map<String, Vector<String>> messages;
	private Map<InetAddress, String> IPproject;
	private UDPReceiver thread;

	public ChatData() {

		this.IPproject = new HashMap<InetAddress, String>();
		this.messages = new ConcurrentHashMap<String, Vector<String>>();
		thread = new UDPReceiver(IPproject, messages);
		thread.run();
	}

	public void interrupt() {
		thread.interrupt();
	}

	public Iterator<String> getMessages(String projectName) {
		return messages.get(projectName).iterator();
	}
	
	public boolean addChat(String projectName, InetAddress address) {
		try {
		thread.addChat(projectName, address);
		return true;
	} catch (IOException e) {
		System.out.println("Error in joining the chat group");
		
	}
		return false;
	}
	
	public boolean removeChat(String projectName) {
		try {
		thread.removeChat(projectName);
		return true;
		} catch (IOException e) {
			System.out.println("Error in leaving the chat group");
			
		}
		return false;
	}
	
}

	@SuppressWarnings("deprecation")

	class UDPReceiver implements Runnable {
		// InetAddress address;
		MulticastSocket mcSocket;
		Map<InetAddress, String> IPproject;
		Map<String, Vector<String>> messages;

		public UDPReceiver(Map<InetAddress, String> IPproject, Map<String, Vector<String>> messages) {
			this.IPproject = IPproject;
			this.messages = messages;
			this.mcSocket = null;
		}

		@Override
		public void run() {
			System.out.println("thread partito");
			int mcPort = 9991;
			MulticastSocket mcSocket;
			try {
				mcSocket = new MulticastSocket(mcPort);

				System.out.println("Multicast Receiver running at:" + mcSocket.getLocalSocketAddress());

				while (true) {
					// Iterator i = IPproject.keySet().iterator();
					DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
					System.out.println("Waiting for a  multicast message...");
					mcSocket.receive(packet);

					String projectSender = IPproject.get(packet.getAddress()); // controlla null
					String msg = new String(packet.getData(), packet.getOffset(), packet.getLength());
					messages.get(projectSender).add(msg);
					System.out.println("messaggio nel gruppo : " + projectSender);
					System.out.println("# messaggi " + messages.size());
				}
			} catch (IOException e) {
				/////////////////////
				e.printStackTrace();
			}
		}

		public void interrupt() {
			System.out.println("Interruzione UDP receiver thread");

			mcSocket.close();

		}

		public void addChat(String projectName, InetAddress address) throws IOException {
			
				mcSocket.joinGroup(address);
				this.IPproject.put(address, projectName);
		

		}

		public void removeChat(String projectName) throws IOException {

				Iterator<Map.Entry<InetAddress, String>> i = IPproject.entrySet().iterator();
				while (i.hasNext()) {
					Map.Entry<InetAddress, String> entry = i.next();
					if (entry.getValue().equals(projectName))
						mcSocket.leaveGroup(entry.getKey());
					this.IPproject.remove(entry.getKey());
				}
			

		}

	}

public class Client implements ClientInterfaceRMI {

	private String nickName; // username dell'utente loggato nel client
	private UserState clientState;
	private Map<String, Integer> functionParams;// flag settato a false di default, viene impostato a true quando											// l'utente effettua il login
	private Map<String, UserState> stateOfUsers;// mappa nickname-stato
	private ChatData chatData;
	private Scanner scanner;
	private Map<String,InetAddress> projectIP;
	// private StringTokenizer tokenizer;
	private Connections connections; // o lo creo dopo???
	private int RMIport = 8888;

	

	public Client() {

		this.nickName = null;
		this.clientState = UserState.OFFLINE;
		this.stateOfUsers = new HashMap<>();
		this.chatData = new ChatData();
		this.scanner = new Scanner(System.in);
		// this.tokenizer = new StringTokenizer(null);
		this.connections = new Connections();
		this.functionParams = new HashMap<>();
		this.projectIP = new HashMap<String,InetAddress>();
		functionParams.put("register", 2);
		functionParams.put("login", 2);
		functionParams.put("list_users", 0);
		functionParams.put("list_online_users", 0);
		functionParams.put("list_projects", 0);
		functionParams.put("create_project", 1);
		functionParams.put("cancel_project", 1);
		functionParams.put("add_card", 3);
		functionParams.put("add_member", 2);
		functionParams.put("list_my_projects", 0);
		functionParams.put("show_members", 1);
		functionParams.put("move_card", 4);
		functionParams.put("show_card", 2);
		functionParams.put("show_cards", 1);
		functionParams.put("get_card_history", 2);
		functionParams.put("read_chat", 1);
		functionParams.put("send_chat_message", 2);
		functionParams.put("logout", 0);
		functionParams.put("close", 0);
		functionParams.put("HELP", 0);

	}

	///// metodo ausiliario

	private boolean checkParameters(String function, List<String> params) {

		Integer expectedParam = functionParams.get(function);
		if (expectedParam == null) {
			System.out.println("This function does note exists\n");
			return false;
		}

		// confronto in integer????
		if (!(expectedParam.equals(params.size()))) {
			System.out.println("wrong number of parameters for function f\n");
			return false;
		}

		return true;
	}

	@Override
	public String getnickName() {
		return this.nickName;
	}

	private List<String> getOnlineUsers() {

		ArrayList<String> onlineUsers = new ArrayList<>();

		for (var entry : stateOfUsers.entrySet()) {

			if (entry.getValue().equals(UserState.ONLINE))
				onlineUsers.add(entry.getKey());
		}
		return onlineUsers;

	}

	public void start() throws RemoteException, NotBoundException {

		Registry r = LocateRegistry.getRegistry(RMIport);
		// creo stub di oggetto remoto
		ServiceRMI stubS = (ServiceRMI) r.lookup("WORTH-SERVER");
		ClientInterfaceRMI stubC = (ClientInterfaceRMI) UnicastRemoteObject.exportObject(this, 0);

		boolean done = false;

		String request = new String();
		String response = new String();

		while (!done) {

			String toProcess = scanner.nextLine();

			ArrayList<String> parameters = new ArrayList<>();
			String command = new String();
			StringTokenizer st = new StringTokenizer(toProcess);

			try {
				command = st.nextToken();
			} catch (NoSuchElementException e) {
				System.out.println("empty command");
			}

			/// se ho un comando vuoto esco

			while (st.hasMoreTokens()) {
				// if(command != "")
				parameters.add(st.nextToken());
			}

			if (command.equals("add_card") && parameters.size() == 2)
				parameters.add("Empty");

			if (checkParameters(command, parameters)) {

				// concateno nella stringa request command e i suoi parametri
				/*
				 * request = command; for(String s : parameters) request = request+ " " + s;
				 */

				switch (command) {

				case ("register"):
					if (connections.register(parameters.get(0), parameters.get(1))) {
						this.nickName = parameters.get(0);

						System.out.println("Registration successfully done");
					}

					break;

				case ("login"):

					// concateno nella stringa request command e i suoi parametri
					request = command;
					for (String s : parameters)
						request = request + " " + s;

					// System.out.println(request);
					// this.nickName == null
					if (clientState.equals(UserState.OFFLINE)) {
						this.nickName = parameters.get(0);
						stubS.registerForCallback(stubC);
						response = connections.sendRequest(request);
						if (response.contains("Successfully logged in")) {
							this.clientState = UserState.ONLINE;
							// this.nickName = parameters.get(0);

							String allUsers = connections.sendRequest("list_users");
							// String name = new String();

							allUsers = allUsers.replace("[", "").replace("]", "");
							StringTokenizer t = new StringTokenizer(allUsers, ",");

							while (t.hasMoreTokens())
								stateOfUsers.put(t.nextToken().trim(), UserState.OFFLINE);
							stateOfUsers.put(nickName, UserState.ONLINE);
						}
						String onlineUsers = connections.sendRequest("list_online_users");
						// String name = new String();

						onlineUsers = onlineUsers.replace("[", "").replace("]", "");
						StringTokenizer t = new StringTokenizer(onlineUsers, ",");

						while (t.hasMoreTokens())
							stateOfUsers.put(t.nextToken().trim(), UserState.ONLINE);
						// this.nickName = null;
						System.out.println(response);
					} else
						System.out.println("Already logged in");

					break;

				// case that don't need identification but just to be loggedin

				case ("list_projects"):

					// concateno nella stringa request command e i suoi parametri
					request = command;
					for (String s : parameters)
						request = request + " " + s;

					System.out.println(request);

					if (clientState.equals(UserState.ONLINE)) {
						response = connections.sendRequest(request);
						System.out.println(response);
					} else
						System.out.println("You need to be logged in before this request");

					break;

				case ("list_users"):

					if (clientState.equals(UserState.ONLINE)) {
						// response = this.stateOfUsers.keySet().toArray().toString();
						System.out.println(this.stateOfUsers);
					} else
						System.out.println("You need to be logged in before this request");

					break;

				case ("list_online_users"):

					if (clientState.equals(UserState.ONLINE)) {
						// response = this.getOnlineUsers().toArray().toString();
						System.out.println(this.getOnlineUsers());
					} else
						System.out.println("You need to be logged in before this request");

					break;
				// case that need to check if user is a project member && logged
				// it's necessary to pass client nickName

				case ("create_project"):

				case ("cancel_project"):

				case ("add_card"):

				case ("add_member"):

				case ("list_my_projects"):

				case ("show_members"):

				case ("move_card"):

				case ("show_card"):

				case ("show_cards"):

				case ("get_card_history"):

				case ("logout"):

					if (clientState.equals(UserState.ONLINE)) {

						// concateno nella stringa request command e i suoi parametri
						request = command + " " + this.nickName;
						for (String s : parameters)
							request = request + " " + s;

						System.out.println(request);

						response = connections.sendRequest(request);

						System.out.println(response);

					} else
						System.out.println("You need to be logged in before this request");

					break;
				case ("send_chat_message"):
					if (clientState.equals(UserState.ONLINE)) {

						InetAddress chatAddress = projectIP.get(parameters.get(0));
						if (chatAddress.equals(null)) {
							System.out.println("you are not member of this project");
						}

						try {
							connections.sendChatMsg(this.nickName + ": " + parameters.get(1), chatAddress);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					} else
						System.out.println("You need to be logged in before this request");

					break;

				case ("read_chat"):
					if (clientState.equals(UserState.ONLINE)) {

						
						Iterator<String> i = chatData.getMessages(parameters.get(0));
						if (!(i.hasNext()))
							System.out.println("NO new message in this chat");

						else
							while (i.hasNext()) {
								System.out.println(i.next());
								i.remove();
							}

					} else
						System.out.println("You need to be logged in before this request");
					break;

				case ("close"):
					response = connections.sendRequest("logout");
					clientState = UserState.OFFLINE;
					stubS.unregisterForCallback(this);
					this.chatData.interrupt();
					done = true;
					break;

				case ("HELP"):
					printHelp();
					break;

				}

				if (command.equals("logout")) {
					clientState = UserState.OFFLINE;
					stubS.unregisterForCallback(this);
					this.chatData.interrupt();
				}

			} else {
				System.out.println("Error in request use HELP command for more info");

			}

		}

	}

	public void printHelp() {

		System.out.println("HERE IS A LIST OF ALLA OPERATION AVAILABLE ON WORTH:");
		System.out.println("Operation are shown as:");
		System.out.println("Command Name : [first parameter needed] ... [last parameter needed]");
		System.out.println("* Brief description of what's the meaning of the command *");
		System.out.println(
				"ATTENTION: parameters can't assume null values or empty string if not specified in description");

		System.out.println("_____________________________________________________");

		System.out.println("");
		System.out.println("register: [nickName][password]");
		System.out.println("* command to register a new WORTH user identified by nickName *");
		System.out.println("ACTION YOU CAN PERFORM AS WORTH USER: (after registration) ");
		System.out.println("login: [nickName][password]");
		System.out.println("* command to login in WORTH as the WORTH user identified by nickName *");
		System.out.println("list_users: ");
		System.out.println("* command to ask for the list of all the WORTH users *");
		System.out.println("list_online_users:");
		System.out.println("* command to ask for the list of all the WORTH users currently logged in (online) *");
		System.out.println("list_projects:");
		System.out.println(
				"* command to ask for the list of all the WORTH projects that have the current user as a member *");
		System.out.println("create_project: [projectName]");
		System.out.println("* command to create a new WORTH project identified by projectName *");
		System.out.println("logout: ");
		System.out.println("* command to logout the current user from WORTH *");
		System.out.println("HELP");
		System.out.println("");

		System.out.println("ACTION YOU CAN PERFORM ON A PROJECT AS MEMBER OF THE SPECIFIED PROJECT: ");

		System.out.println("cancel_project: [projectName]");
		System.out.println("* command to delete the project identified by projectName *");
		System.out.println("add_card: [projectName][cardName][description]");
		System.out.println(
				"* command to add a card identified by cardName in the project identified by parojectName, the descripion parameter is not mandatory *");
		System.out.println("add_member: [projectName][nickName] ");
		System.out.println(
				"* command to add the WORTH user identified by nickName to the project identified by projectName *");
		// System.out.println("list_my_projects:");
		System.out.println("show_members: [projectName]");
		System.out.println(
				"* command to ask for the list of all members in the WORTH project identified by projectName *");
		System.out.println("move_card: [projectName][cardName][listFrom][listTo]");
		System.out.println(
				"* command to move the card identified by CardName in the project identified by projectName from the listFrom to the listTo *");
		System.out.println("the lists in each project are: TODO, INPROGRESS, TOBEREVISED, DONE");
		System.out.println("show_card: [projectName][cardName]");
		System.out.println(
				"* command to ask for the card identified by cardName in the project identified by projectName* ");
		System.out.println("show_cards: [projectName]");
		System.out.println("* command to ask for the list of all cards in the project identified by projectName *");
		System.out.println("get_card_history: [projectName][cardName]");
		System.out.println(
				"* command to ask for the history of all the list's transition of the card identified by cardName in the specified project *");

		System.out.println("read_chat: [projectName]");
		System.out.println("* command to ask for the messages in the chat associated to the specified project *");
		System.out.println("send_chat_message: [projectName][msg]");
		System.out.println("* command to send the specified message in the chat associated to the specified project *");

		System.out.println("");

	}

	

	@Override
	public void notifyNewChat(String projectName, InetAddress address) {
		/*if (!(chatDatas.containsKey(projectName)))
			chatDatas.put(projectName, new ChatData(address));*/
		if(chatData.addChat(projectName, address))

		System.out.println("You have joined the " + projectName + "'s chat!");

	}

	@Override
	public void notifyUserState(String nikName, UserState state) throws RemoteException {
		stateOfUsers.put(nickName, state);
		
	}

}
