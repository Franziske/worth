package client;

import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;

import server.UserState;

public interface ServiceRMI extends Remote{

		public boolean register(String nick, String psswd) throws RemoteException;

		// registrare per callback
		public void registerForCallback(ClientInterfaceRMI callbackClient) throws RemoteException;

		// unregistrare per callback
		public void unregisterForCallback(ClientInterfaceRMI callbackClient) throws RemoteException;

		// inviare notifica
		public void sendNotification(String user, UserState us) throws RemoteException;

		// invia riferimenti per la chat

		public void sendNewChatAddress(String dest, String projectName, InetAddress address) throws RemoteException;
	

}
