import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.security.*;
import java.security.interfaces.*;

public class Server {
	private static int serverID;
	private static int RSAkeyLen = 1024;
	private static int DHkeyLen = 512;
	private static RSAPrivateKey RSAprivateKey;
	private static RSAPublicKey RSApublicKey;
	private static int port;
	private static String ip = null;
	private static ServerSocket serverSock;
	private static List<ClientObj> clientObjs =
        Collections.synchronizedList(new ArrayList<ClientObj>()); // thread safe tracking of clients
	private static AtomicInteger nextId = new AtomicInteger(1); // incrementor for clientObj IDs

	// displays the list of client objects, for testing
	private static void printClients () {
		System.out.println(clientObjs);
	}

	// spawn a new thread for every new client connect so they can be handled simultaneously
	private static class clientListener extends Thread {
		Socket socket = null;
		OutputStream out = null;
		ObjectOutputStream objOut = null;
		InputStream in = null;
		ObjectInputStream objIn = null;

		// initialize thread with the socket from client that the server accepted
        public clientListener(Socket socket){
            this.socket = socket;
        }

		// set outputs streams from socket connection
		private void setOutputStream() {
			// accept connection from client and create streams
			try {
				out = socket.getOutputStream();
				objOut = new ObjectOutputStream(out);
				in = socket.getInputStream();
				objIn = new ObjectInputStream(in);
			} catch (Exception e) {
				System.out.println("Failed to accept client connection and start streams");
				e.printStackTrace();
			}
		}

		// close output streams
		private void closeOutputStreams() {
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
		}
		
		@Override
        public void run() {
			// set outputs streams from socket connection
			setOutputStream();

			// get message from socket, holds ip and port client is listening
			// for other client connections on
			Message clientMsg = Util.recieveMsg(objIn);
			System.out.println(clientMsg);
			System.out.println("Received message");

			// add client to list of clients
			clientObjs.add(new ClientObj(clientMsg.getId(), clientMsg.getIp(), clientMsg.getPort()));

			// get public key of client
			RSAPublicKey clientRSAPubKey = Util.getPublicKey(clientMsg.getId());

			// respond to client with authentication challenge
			int challenge = Util.getRandom(100000);	// get rand number as challenge
			Message msg = new Message("");	// create message with encrypted challenge
			msg.setChallenge(Util.encrypt(String.valueOf(challenge), clientRSAPubKey));
			Util.sendMsg(objOut, msg);
			System.out.println("Sent challenge: " + challenge + ", to client " + clientMsg.getId());

			// read challenge response from client
			Message reply = Util.recieveMsg(objIn);
			System.out.println("Received challenge reply " + reply.getMsg() + ", from client " + clientMsg.getId());
			
			// check if client is authenticated, if not then abort
			String challengeReply = reply.getMsg();
			if (challengeReply.equals(String.valueOf(challenge))) {
				System.out.println("Client successfully authenticated");
			} else {
				System.out.println("Client failed authenticated");
				Thread.currentThread().interrupt();
				return;
			}

			// display clients the servers tracked
			printClients();

			// establish session key with client
			byte[] sessionKey = doDHKeyExchange();
			if (sessionKey == null) {
				System.out.println("Error establishing D-H key exchange");
				closeOutputStreams();
			}

			closeOutputStreams();
		} // end run

		public byte[] doDHKeyExchange() {
			byte [] sessionKey = null;
			try {
				DHServer dh;
				// create DH keys and send public key to client
				dh = new DHServer(DHkeyLen);
				byte[] pubKey = dh.getKeyToSend();
				objOut.writeObject(pubKey);
				objOut.flush();
				System.out.println("Sent client DH public key");

				// receive client DH public key
				byte[] clientPubKey = (byte[])objIn.readObject();
				System.out.println("Received client DH public key");

				// generate session key
				sessionKey = dh.computeSharedSecret(clientPubKey);


			} catch (Exception e) {
				e.printStackTrace();
			}
			return sessionKey;
		}

	} // end clientListener

	// create server socket and set my ip and port
	private static void createServerSocket() {
		// create a server socket and let it pick any open port
		try {
			serverSock = new ServerSocket(0);
		} catch (Exception e) {
			System.out.println("Failed to start server");
			e.printStackTrace();
		}

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
	}

	public static void main(String[] args) {
		serverID = Integer.parseInt(args[0]);

		//generate key pair and store it to file, only used once
		// KeyPair pair = Util.generateRSAKeyPair(RSAkeyLen, serverID);
		// PrivateKey RSAprivateKey = pair.getPrivate();
		// PublicKey RSApublicKey = pair.getPublic();
		// Util.writePublicKey(serverID, RSApublicKey);
		// Util.writePrivateKey(serverID, RSAprivateKey);

		// get server keys from file
		RSApublicKey = Util.getPublicKey(serverID);
		RSAprivateKey = Util.getPrivatKey(serverID);

		// create server socket and set my ip and port
		createServerSocket();

		// write chosen port to file so other processes can retrieve it
		Util.writeServerMetadata(ip, port);

		// for every new client connection create a new server thread
        while(true) {
            try {
                Socket socket = serverSock.accept();
                System.out.println("Client has successfully connected");
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
