import java.net.*;
import java.io.*;

public class Server {
	private static int port;
	private static ServerSocket serverSock;

	// spawn a new thread for every new client connect so they can be handled simultaneously
	private static class clientListener extends Thread {
		@Override
        public void run() {
			String ip = null;
			Socket socket = null;
			OutputStream out = null;
			ObjectOutputStream objOut = null;
			InputStream in = null;
			ObjectInputStream objIn = null;
			
			// get the servers hostName and port
			try {
				port = serverSock.getLocalPort(); // get an open port on the system
				InetAddress fullIP = InetAddress.getLocalHost(); // get servers ip
				ip = fullIP.getHostAddress();
				System.out.println("Server started at " + ip + ":" + port);
			} catch (Exception e) {
				System.out.println("Failed to get server host and port");
				e.printStackTrace();
			}

			// write chosen port to file so other processes can retrieve it
			Util.writeServerMetadata(ip, port);

			// repeatidly perform server duties until terminated
			while (true) {
				// accept connection from client and create streams
				try {
					socket = serverSock.accept();
					System.out.println("Client has successfully connected");
					out = socket.getOutputStream();
					objOut = new ObjectOutputStream(out);
					in = socket.getInputStream();
					objIn = new ObjectInputStream(in);
				} catch (Exception e) {
					System.out.println("Failed to accept client connection and start streams");
					e.printStackTrace();
				}
				
				// get message from socket
				try {
					Message clientMsg = (Message) objIn.readObject();
					System.out.println("Received message");
					// TODO - process client message
				} catch (Exception e) {
					System.out.println("Failed to get object from server socket");
					e.printStackTrace();
				}

				// respond to client
				try {
					Message msg = new Message();
					objOut.writeObject(msg);
					System.out.println("Sent message");
				} catch (Exception e) {
					System.out.println("Failed to write to output stream");
					System.out.println(e);
				}

				// close streams and socket
				try {
					objOut.close();
					out.flush();
					out.close();
					objIn.close();
					in.close();
					socket.close();
				} catch (Exception e) {
					System.out.println("Failed to close streams and socket");
					System.out.println(e);
				}
			} // end while loop server duties
		} // end run
	} // end clientListener

	public static void main(String[] args) {
		// create a server socket and let it pick any open port
		try {
			serverSock = new ServerSocket(0);
		} catch (Exception e) {
			System.out.println("Failed to start server");
			e.printStackTrace();
		}

		// client thread controls communication from the client
		Thread client = new Thread(new clientListener(), "Thread - client");

		client.start();
	}
}