package client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class ClientMain {

	public static void main(String[] args) {

		Client c = new Client();
		try {
			c.start();
		} catch (RemoteException | NotBoundException e) {

			e.printStackTrace();
		}

	}

}
