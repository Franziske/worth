package client;

import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;

import server.UserState;


public interface ClientInterfaceRMI extends Remote {

	public void notifyUserState(String nikName, UserState state) throws RemoteException;

	public String getnickName() throws RemoteException;

	public void notifyNewChat(String projectName, InetAddress address) throws RemoteException;

}
