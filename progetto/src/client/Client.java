package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;

import server.ServiceRMI;
import server.UserState;

class ChatData {
	InetAddress address;
	Thread thread;
	public ChatData(InetAddress address) {
		this.address = address;
		thread = new Thread(new UDPReceiver(address));
		thread.start();
	}
	
	public InetAddress getAdress() {
		return this.address;
	}
}

class UDPReceiver implements Runnable {
	InetAddress address;
	
	public UDPReceiver(InetAddress addr) {
		this.address = addr;
	}
	
	@Override
	public void run() {
		System.out.println("trhead partito");
	    int mcPort = 9991;
	    MulticastSocket mcSocket = null;
	   /* try {
			mcIPAddress = InetAddress.getByName(address);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	    
	    while(true) {
	    try {
			mcSocket = new MulticastSocket(mcPort);
			System.out.println("Multicast Receiver running at:"
					+ mcSocket.getLocalSocketAddress());
			mcSocket.joinGroup(address);
			
			DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
			
			System.out.println("Waiting for a  multicast message...");
			mcSocket.receive(packet);
			String msg = new String(packet.getData(), packet.getOffset(),
					packet.getLength());
			System.out.println("[Multicast  Receiver] Received:" + msg);
			
			mcSocket.leaveGroup(address);
			mcSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    }
	}
	
}

public class Client implements ClientInterfaceRMI {

	private String nickName; // username dell'utente loggato nel client
	private UserState clientState;
	private Map<String, Integer> functionParams;// flag settato a false di default, viene impostato a true quando
												// l'utente effettua il login
	private Map<String, UserState> stateOfUsers;// mappa nickname-stato
	private Map<String, ChatData> chatDatas;
	
	private Scanner scanner;
	// private StringTokenizer tokenizer;
	private Connections connections; // o lo creo dopo???
	private int RMIport = 8888;

	public Client() {

		this.nickName = null;
		this.clientState = UserState.OFFLINE;
		this.stateOfUsers = new HashMap<String, UserState>();
		this.chatDatas = new HashMap<String,ChatData>();
		this.scanner = new Scanner(System.in);
		// this.tokenizer = new StringTokenizer(null);
		this.connections = new Connections();
		this.functionParams = new HashMap<String, Integer>();
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

		ArrayList<String> onlineUsers = new ArrayList<String>();

		for (var entry : stateOfUsers.entrySet()) {

			if (entry.getValue().equals(UserState.ONLINE))
				onlineUsers.add(entry.getKey());
		}
		return onlineUsers;

	}

	// switch(function) {

	/*
	 * case("login") : if(clientState.equals(UserState.ONLINE)) {
	 * System.out.println("An user is already logged in"); return false; }
	 * 
	 * } }
	 */

	public void start() throws RemoteException, NotBoundException {

		int p = (int) ((Math.random() * (65636 - 1024)) + 1024);

		Registry r = LocateRegistry.getRegistry(RMIport);
		// creo stub di oggetto remoto
		ServiceRMI stubS = (ServiceRMI) r.lookup("WORTH-SERVER");
		ClientInterfaceRMI stubC = (ClientInterfaceRMI) UnicastRemoteObject.exportObject(this, p);

		boolean done = false;

		String request = new String();
		String response = new String();

		while (!done) {

			String toProcess = scanner.nextLine();

			ArrayList<String> parameters = new ArrayList<String>();
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

					if (clientState.equals(UserState.OFFLINE)) {
						this.nickName = parameters.get(0);
						stubS.registerForCallback(stubC);
						response = connections.sendRequest(request);
						if (response.contains("Successfully logged in")) {
							this.clientState = UserState.ONLINE;
							//this.nickName = parameters.get(0);
							

							String allUsers = connections.sendRequest("list_users");
							// String name = new String();

							allUsers = allUsers.replace("[", "").replace("]", "");

							// String listString = allUsers.substring(1, allUsers.length() - 2); // chop off
							// brackets
							StringTokenizer t = new StringTokenizer(allUsers, ",");

							while (t.hasMoreTokens())
								stateOfUsers.put(t.nextToken().trim(), UserState.OFFLINE);
							stateOfUsers.put(nickName, UserState.ONLINE);
						}

						//this.nickName = null;
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

				case ("read_chat"):

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
						System.out.println("You need to logged in before this request");

					break;
				case ("send_chat_message"):
					
					InetAddress chatAddress = chatDatas.get(parameters.get(0)).getAdress();
					if(chatAddress.equals(null)) {
						System.out.println("you are not member of this project");
					}
					
					
					try {
						connections.sendChatMsg(this.nickName + ": " + parameters.get(1), chatAddress);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					/*String addr = "230.1.1.1";
					try {
						InetAddress ia = InetAddress.getByName(addr);
						chatDatas.put("nomeprogetto", new ChatData(ia));
			
						connections.sendChatMsg("ciao", addr);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}*/
					break;
				case ("HELP"):
					printHelp();
					break;

				}

				if (command.equals("logout")) {
					done = true;
					clientState = UserState.OFFLINE;
					stubS.unregisterForCallback(this);
				}
				

			} else {
				System.out.println(" Error in request use HELP command for more info\n");

			}

		}

	}

	public void printHelp() {

		System.out.println("HERE IS A LIST OF ALLA OPERATION AVAILABLE ON WORTH:\n");
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
	public void notifyUserState(String nickName, UserState state) throws RemoteException {
		stateOfUsers.put(nickName, state);

	}

	@Override
	public void notifyNewChat(String projectName, InetAddress address) {
		if(!(chatDatas.containsKey(projectName))) chatDatas.put(projectName, new ChatData(address));
		
		System.out.println("You have joined the " + projectName + "'s chat!");
		
	}

	

}


