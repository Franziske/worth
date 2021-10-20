package server;

//import java.io.File;
import java.io.IOException;

public class ServerMain {

	public static void main(String[] args) throws IOException {

		ChannelMultiplexingServer s = new ChannelMultiplexingServer();

		s.start();
	}

}
