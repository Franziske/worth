package server;

//import java.io.File;
import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
//import com.fasterxml.jackson.databind.*;
import java.rmi.server.UnicastRemoteObject;

import exceptions.DuplicateException;
import exceptions.SuchDBAlreadyExistsException;

public class ServerMain {

	public static void main(String[] args) throws IOException {

		ChannelMultiplexingServer s = new ChannelMultiplexingServer();

		s.start();
	}

}
