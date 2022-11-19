import java.net.*;
import java.io.*;

public class Server {
	private static int port;
	private static ServerSocket serverSock;

	// spawn a new thread for every new client connect so they can be handled simultaneously
	private static class clientListener extends Thread {
		Socket socket = null;

		// initialize thread with the socket from client that the server accepted
        public clientListener(Socket socket){
            this.socket = socket;
        }
		
		@Override
        public void run() {
			String ip = null;
			OutputStream out = null;
			ObjectOutputStream objOut = null;
			InputStream in = null;
			ObjectInputStream objIn = null;
			
			// get the servers hostName and port
			try {
				port = serverSock.getLocalPort(); // get an open port on the system
				InetAddress fullIP = InetAddress.getLocalHost(); // get servers ip
				ip = fullIP.getHostAddress(); // get just the ip portion
				System.out.println("Server started at " + ip + ":" + port);
			} catch (Exception e) {
				System.out.println("Failed to get server host and port");
				e.printStackTrace();
			}

			// write chosen port to file so other processes can retrieve it
			Util.writeServerMetadata(ip, port);

			// repeatedly perform server duties until terminated
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
				// TODO - process client message
				Message clientMsg = Util.recieveMsg(objIn);
				System.out.println("Received message");

				// respond to client
				Message msg = new Message();
				Util.sendMsg(objOut, msg);
				System.out.println("Sent message");

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

		// for every new client connection create a new server thread
        while(true) {
            try {
                Socket socket = serverSock.accept();
                System.out.println("Connection to server successful");
                clientListener serverThread = new clientListener(socket);
                serverThread.start();
            }
            catch(Exception e) {
                e.printStackTrace();
                System.out.println("Failed accept client connection and create server thread");
            }
        }
	}
}