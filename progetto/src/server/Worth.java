package server;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;

import client.ClientInterfaceRMI;
import exceptions.DuplicateException;
import exceptions.NotFoundException;

public class Worth extends RemoteServer implements ServiceRMI {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private DataBase worthdb;
	private ArrayList<Project> worthProjects;
	private ArrayList<User> worthUsers;
	private HashMap<String, UserState> stateOfUsers;
	private List<ClientInterfaceRMI> registeredForCB;

	public Worth() throws IOException {
		this.worthProjects = new ArrayList<Project>();
		this.worthUsers = new ArrayList<User>();
		this.worthdb = new DataBase();
		this.stateOfUsers = new HashMap<String, UserState>();
		this.registeredForCB = new ArrayList<ClientInterfaceRMI>();

		// recupero dal database gli utenti già registrati e i progetti in corso
		worthUsers.addAll(worthdb.getUsers());
		for (User u : worthUsers) {
			stateOfUsers.put(u.getnikName(), UserState.OFFLINE);
		}

		worthProjects.addAll(worthdb.getProjects());
	}

	// metodi ausiliari

	private void checkString(String s) throws IllegalArgumentException {
		if (s == null)
			throw new IllegalArgumentException();
		if (s == "")
			throw new IllegalArgumentException();
	}

	private void checkString(String s, int maxlenght) throws IllegalArgumentException {
		if (s == null)
			throw new IllegalArgumentException();
		if (s == "")
			throw new IllegalArgumentException();
		if (s.length() < maxlenght)
			throw new IllegalArgumentException();
	}

	private Project getProject(String projectName) {
		for (Project p : this.worthProjects)

			if (p.getName().equals(projectName)) {
				return p;
			}
		return null;
	}

	private User getUser(String usrName) {
		for (User u : this.worthUsers)

			if (u.getnikName().equals(usrName)) {
				return u;
			}
		return null;
	}

	private Card getCard(Project p, String cardName) {
		for (Card c : p.getAllCards())

			if (c.getTaskName().equals(cardName)) {
				return c;
			}
		return null;
	}
	
	private ClientInterfaceRMI getClientForCB(String usrName) throws RemoteException {
		for (ClientInterfaceRMI c : this.registeredForCB) {

			if (c.getnickName().equals(usrName)) {
				return c;
			}
			
		}
		return null;
	}

	//////////////

	public boolean register(String nik, String psswd) throws RemoteException {
		try {
			checkString(nik);
		} catch (IllegalArgumentException e) {
			System.out.println("Invalid nikname");
			return false;
		}
		try {
			checkString(psswd, 8);
		} catch (IllegalArgumentException e) {
			System.out.println("A valid Password need at least 8 characters");
			return false;
		}
		for (User u : worthUsers) {
			if (u.getnikName().equals(nik))
				return false;
		}
		User newu = new User(nik, psswd);

		this.worthdb.addUser(newu);
		this.worthUsers.add(newu);
		this.stateOfUsers.put(nik, UserState.OFFLINE);
		sendNotification(nik, UserState.OFFLINE);

		return true;

	}

	public String login(String nik, String psswd) throws RemoteException {
		try {
			checkString(nik);
		} catch (IllegalArgumentException e) {
			System.out.println("Invalid nikname");
			return "login failed inavalid nikname";
		}
		try {
			checkString(psswd);
		} catch (IllegalArgumentException e) {
			System.out.println("invalid password");
			return "login failed invalid password";
		}
		User u = this.getUser(nik);
		if (u == null)
			return "current user does not exists in WORTH, register your account first";
		if (u.getPassword().equals(psswd)) {
			if (this.stateOfUsers.get(nik).equals(UserState.ONLINE))
				return "already logged in";

			this.stateOfUsers.replace(nik, UserState.ONLINE);
			sendNotification(nik, UserState.ONLINE);
			
			return " Successfully logged in";
		}

		return "incorrect password, try again";

	}

	public String logout(String nik) throws RemoteException {
		try {
			checkString(nik);
		} catch (IllegalArgumentException e) {
			System.out.println("Invalid nikname");
			return "login failed inavalid nikname";
		}
		User u = this.getUser(nik);
		if (u == null)
			return "current user does not exists in WORTH, register your account first";
		if (this.stateOfUsers.get(nik).equals(UserState.OFFLINE))
			return "already logged out";

		this.stateOfUsers.replace(nik, UserState.OFFLINE);
		sendNotification(nik, UserState.OFFLINE);
		return " Successfully logged out";
	}

	public ArrayList<String> listUsers() {
		ArrayList<String> aux = new ArrayList<String>();

		for (User u : worthUsers) {
			aux.add(u.getnikName());
		}
		return aux;
	}

	public List<String> listOnlineUsers() {

		ArrayList<String> onlineUsers = new ArrayList<String>();

		for (var entry : stateOfUsers.entrySet()) {

			if (entry.getValue().equals(UserState.ONLINE))
				onlineUsers.add(entry.getKey());
		}
		return onlineUsers;

	}

	/// ????????????

	public boolean offline(String currentUsr) {
		if (stateOfUsers.replace(currentUsr, UserState.OFFLINE) == UserState.ONLINE)
			return true;
		else
			return false;
	}

	public ArrayList<String> listProjects() {
		ArrayList<String> aux = new ArrayList<String>();

		for (Project p : this.worthProjects) {
			aux.add(p.getName());
		}
		return aux;
	}

	public ArrayList<String> listMyProjects(String currentUsr) {
		ArrayList<String> aux = new ArrayList<String>();

		for (Project p : this.worthProjects) {
			if (p.getMembers().contains(currentUsr)) {
				aux.add(p.getName());
			}
		}
		return aux;
	}

	public String createProject(String currentUsr, String projectName, InetAddress chatAddress) throws IOException {
		checkString(projectName);
		checkString(currentUsr);

		for (Project p : worthProjects) {
			if (p.getName() == projectName)
				return "Project " + projectName + "already exists in WORTH";
		}

		Project newp = new Project(projectName, getUser(currentUsr), chatAddress);

		this.worthdb.addProject(newp);
		this.worthProjects.add(newp);
		sendNewChatAddress(currentUsr, projectName, chatAddress);
		return "Project " + projectName + "successfully created in WORTH";

	}

	public String addMember(String currentUsr, String projectName, String nik, InetAddress chatAddress) {
		Project p = this.getProject(projectName);
		User u = this.getUser(nik);

		if ((p == null))
			return " Project" + projectName + "doesn't exists";
		if (p.getMembers().contains(nik))
			return "User" + nik + "is alaready member of project " + projectName;
		if (!(p.getMembers().contains(currentUsr)))
			return "User " + currentUsr + "is not member of project " + projectName;

		p.addMember(u);
		// u.addProject(projectName);
		// aggiorna db
		try {
			sendNewChatAddress(nik, projectName, chatAddress);
		} catch (RemoteException e) {
			return "Remote exception occurred, can't add " + nik + " to project " + projectName;
		} //??????
		this.worthdb.refreshProject(p);
		this.worthdb.refreshUser(u);

		return nik + " successfully added to " + projectName;
	}

	public String showMembers(String currentUsr, String projectName) {
		Project p = this.getProject(projectName);

		if ((p == null))
			return " Project" + projectName + "doesn't exists";
		if (!(p.getMembers().contains(currentUsr)))
			return "User " + currentUsr + "is not member of project " + projectName;

		return Arrays.toString(p.getMembers().toArray());

	}

	public String addCard(String currentUsr, String projectName, String cardName, String description) {
		Project p = this.getProject(projectName);

		if ((p == null))
			return " Project" + projectName + "doesn't exists";
		if (!(p.getMembers().contains(currentUsr)))
			return "User " + currentUsr + "is not member of project " + projectName;

		try {
			if (description == null) {
				p.addCard(cardName);
				Card c = new Card(cardName, description);
				this.worthdb.addCardToProject(c, p);
				this.worthdb.refreshProject(p);
				return cardName + " successfully added to " + projectName;
			}
			p.addCard(cardName, description);
			Card c = new Card(cardName, description);
			this.worthdb.addCardToProject(c, p);
			this.worthdb.refreshProject(p);
			return cardName + " successfully added to " + projectName;

		} catch (DuplicateException e) {
			return "Card " + cardName + "already exists in project " + projectName;
		}
	}

	public String showCards(String currentUsr, String projectName) {
		Project p = this.getProject(projectName);

		if ((p == null))
			return " Project" + projectName + "doesn't exists";
		if (!(p.getMembers().contains(currentUsr)))
			return "User " + currentUsr + "is not member of project " + projectName;

		String s = "";
		for (Card c : p.getAllCards()) {
			s = s + "Card" + c.getTaskName() + " description :" + c.getDescription() + "Card's history: "
					+ c.getHistory().toString();
		}
		if (s.equals(""))
			return "No cards found in " + projectName;
		return s;

	}

	public String showCard(String currentUsr, String projectName, String cardName) {
		Project p = this.getProject(projectName);

		if ((p == null))
			return " Project" + projectName + "doesn't exists";
		if (!(p.getMembers().contains(currentUsr)))
			return "User " + currentUsr + "is not member of project " + projectName;

		try {
			Card c = p.getCard(cardName);
			return "Card" + c.getTaskName() + " description :" + c.getDescription() + "Card's history: "
					+ c.getHistory().toString();

		} catch (NotFoundException e) {
			return "Card " + cardName + "doesn't exists in project " + projectName;
		}

	}

	/// metti user come primo parametro
	public String getCardHistory(String currentUsr, String projectName, String cardName) {
		Project p = this.getProject(projectName);

		if ((p == null))
			return " Project" + projectName + "doesn't exists";
		if (!(p.getMembers().contains(currentUsr)))
			return "User " + currentUsr + "is not member of project " + projectName;

		try {
			Card c = p.getCard(cardName);
			return "Card's history: " + c.getHistory().toString();

		} catch (NotFoundException e) {
			return "Card " + cardName + "doesn't exists in project " + projectName;
		}

	}

	public String moveCard(String currentUsr, String projectName, String cardName, CardState from, CardState to)
			throws StreamWriteException, DatabindException, IOException {
		Project p = this.getProject(projectName);

		if ((p == null))
			return " Project" + projectName + "doesn't exists";
		if (!(p.getMembers().contains(currentUsr)))
			return "User " + currentUsr + "is not member of project " + projectName;

		try {
			Card c = p.getCard(cardName);
			if (!(c.getCurrentState().equals(from)))
				return "Can't move card " + c.getTaskName() + ": is not in " + from.name() + " state";

			if (c.changeState(to)) {

				this.worthdb.uploadCardState(p, c);
				this.worthdb.refreshProject(p);

				return "Card" + c.getTaskName() + "correctly moved to " + to.name();
			}
			return "Card" + c.getTaskName() + "can't be moved from " + from.name() + " to " + to.name();

		} catch (NotFoundException e) {
			return "Card " + cardName + "doesn't exists in project " + projectName;
		}
	}

	public String cancelProject(String currentUsr, String projectName) throws IOException {
		Project p = this.getProject(projectName);

		if ((p == null))
			return " Project" + projectName + "doesn't exists";
		if (!(p.getMembers().contains(currentUsr)))
			return "User " + currentUsr + "is not member of project " + projectName;
		for (Card c : p.getAllCards()) {
			if (!(c.getCurrentState() == CardState.DONE))
				return "Can't delete project " + projectName + " some task still need to be DONE";
		}
		this.worthdb.deleteProject(p);
		this.worthProjects.remove(p);

		return "Project " + projectName + " deleted ";

	}

	public void sendChatMsg(String currentUsr, String projectName, String msg) {

	}

	public void readChat(String currentUsr, String projectName) {

	}

	@Override
	public synchronized void registerForCallback(ClientInterfaceRMI callbackClient) throws RemoteException {
		if (!(registeredForCB.contains(callbackClient))) {
			registeredForCB.add(callbackClient);
			System.out.println("User registered for callback notification");

		} else
			System.out.println("User already registered for call back");

	}

	@Override
	public synchronized void unregisterForCallback(ClientInterfaceRMI callbackClient) throws RemoteException {
		if (registeredForCB.contains(callbackClient)) {
			registeredForCB.remove(callbackClient);
			System.out.println("User registered for callback notification");
		} else
			System.out.println("Can't remove User: not registered for callback notification");

	}

	@Override
	public void sendNotification(String user, UserState us) throws RemoteException {
		System.out.println("Callbacks---");
		Iterator<ClientInterfaceRMI> i = registeredForCB.iterator();
		while (i.hasNext()) {
			ClientInterfaceRMI client = i.next();
			client.notifyUserState(user, us);
		}
		System.out.println("Callbacks done");

	}
	
	/*public static String getNextIPV4Address(String ip) {
	    String[] nums = ip.split("\\.");
	    int i = (Integer.parseInt(nums[0]) << 24 | Integer.parseInt(nums[2]) << 8
	          |  Integer.parseInt(nums[1]) << 16 | Integer.parseInt(nums[3])) + 1;

	    // If you wish to skip over .255 addresses.
	    if ((byte) i == -1) i++;

	    return String.format("%d.%d.%d.%d", i >>> 24 & 0xFF, i >> 16 & 0xFF,
	                                        i >>   8 & 0xFF, i >>  0 & 0xFF);*/
	

	@Override
	public void sendNewChatAddress(String dest, String projectName, InetAddress address) throws RemoteException {
		
		ClientInterfaceRMI client = getClientForCB(dest);
		if(!(client == null)) client.notifyNewChat(projectName, address);
		
	}

}
