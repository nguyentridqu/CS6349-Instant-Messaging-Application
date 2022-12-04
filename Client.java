import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Scanner;

public class Client {
	private static int clientID;
	private static int RSAkeyLen = 1024;
	private static int DHkeyLen = 512;
	private static PrivateKey RSAprivateKey;
	private static PublicKey RSApublicKey;
	private static ServerSocket clientSock;
	private static String ip;
	private static int port;
	// server connections
	private static Socket socket = null;
	private static OutputStream out = null;
	private static ObjectOutputStream objOut = null;
	private static InputStream in = null;
	private static ObjectInputStream objIn = null;
	private static String delimiter = "|";

	// listens for incoming messages from other clients
	private static class ClientListener extends Thread {
		Socket clientSocket = null;
		OutputStream clientOut = null;
		ObjectOutputStream clientObjOut = null;
		InputStream clientIn = null;
		ObjectInputStream clientObjIn = null;

		// accept client connection and create in/out streams
		private void createClientSocket() {
			try {
				clientSocket = clientSock.accept();
				System.out.println("Client has successfully connected");
				clientOut = clientSocket.getOutputStream();
				clientObjOut = new ObjectOutputStream(clientOut);
				clientIn = clientSocket.getInputStream();
				clientObjIn = new ObjectInputStream(clientIn);
			}
			catch(Exception e) {
				System.out.println("Failed to accept client connection in client");
				e.printStackTrace();
			}
		}

		@Override
        public void run() {
			// accept client connection and create in/out streams
			createClientSocket();

			// get message from socket
			// TODO Handle messages sent
			Message clientMsg = Util.recieveMsg(clientObjIn);
			System.out.println("Received message inside run");

			// respond to server
			Message msg = new Message("");
			Util.sendMsg(clientObjOut, msg);
			System.out.println("Sent message");



		} // end run
	} // end clientListener

	// creates a server socket for listening for client connections and return a message
	// for the server that contains what port and ip im listening on
	private static Message createClientListener() {
		// create a server socket for clients to connect to and let it pick any open port
		try {
			clientSock = new ServerSocket(0);
		} catch (Exception e) {
			System.out.println("Failed to start server");
			e.printStackTrace();
		}

		// get port and ip the client is listening to
		try {
			port = clientSock.getLocalPort(); // get an open port on the system
			InetAddress fullIP = InetAddress.getLocalHost(); // get servers ip
			ip = fullIP.getHostAddress(); // get just the ip portion
		} catch (Exception e) {
			System.out.println("Failed to get server host and port");
			e.printStackTrace();
		}

		// form message that will be sent to the server with the ip and port im
		// listening for client connections on
		Message msg = new Message("");
		msg.setIp(ip);
		msg.setPort(port);

		return msg;
	}

	// make in/out streams for server
	private static void connectToServer() {
		// get servers ports so connections can be made
		int serverPort = Util.getServerPort();
		InetAddress serverIp = Util.getServerIp();
		String ip = serverIp.getHostAddress();

		System.out.println("Server is at " + ip + ":" + serverPort);

		// try to connect to server
		try {
			socket = new Socket(ip, serverPort);
			System.out.println("Successfully connected to server ");
		} catch (Exception e) {
			System.out.println("Failed to connect to server ");
			System.out.println(e);
		}

		// create in and out streams
		try {
			out = socket.getOutputStream();
			objOut = new ObjectOutputStream(out);
			in = socket.getInputStream();
			objIn = new ObjectInputStream(in);
		} catch (Exception e) {
			System.out.println("Failed to create client input and output streams");
			System.out.println(e);
		}
	}

	// close output streams
	private static void closeOutputStreams() {
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

	public static void main(String[] args) {
		clientID = Integer.parseInt(args[0]);

		//generate key pair and store it to file
		KeyPair pair = Util.generateRSAKeyPairAndSaveToFile(RSAkeyLen, clientID);
		RSAprivateKey = pair.getPrivate();
		RSApublicKey = pair.getPublic();
		Util.writeBytesToFile("public_key_" + clientID, RSApublicKey.getEncoded());

		// make in/out streams for server
		connectToServer();

		// start listening for client connections
		Message msg = createClientListener();
		ClientListener clientThread = new ClientListener();
		clientThread.start();

		// send to server what ip and port im listening for clients on
		Util.sendMsg(objOut, msg);
		System.out.println("Sent message");

		// read response
		Message serverMsg = Util.recieveMsg(objIn);
		System.out.println("Received message");

		while(true) {
			System.out.println("Choose the option:\n1 - Get session key from the server\n2 - Disconnect");
			Scanner cin = new Scanner(System.in);
			int option = cin.nextInt();

			if(option == 1){
				try {
					// get server DH public key
					byte[] serverPubKey = (byte[]) objIn.readObject();
					System.out.println("Received server DH public key");

					// create own DH keys
					DHClient dh = new DHClient(serverPubKey);

					// send own DH public key to server
					byte[] myPubKey = dh.getKeyToSend();
					objOut.writeObject(myPubKey);
					objOut.flush();
					System.out.println("Sent client DH public key");

					// generate session key for client-server
					byte[] sessionKey = dh.computeSharedSecret();

					KeyedHash khObj = new KeyedHash();
					// TODO: List all clients with their IP and client ID
					System.out.print("Enter the client ID you want to connect to: ");
					int other_client_id = cin.nextInt();

					// TODO: get session key, ticket and ip of other client from server
					Message Ticket = new Message("");
					byte[] CC_sessionKey = new byte[128];

					// TODO: establish socket connection with other client

					int seq_no = 0;
					// sending ticket to other client
					seq_no++;
					byte[] nonce = new byte[8];
					new SecureRandom().nextBytes(nonce);
					String handshake_str = Ticket.getMsg() + delimiter;
					byte[] handshake_byte = Util.appendByte(handshake_str.getBytes(), khObj.encrypt(nonce, CC_sessionKey));
					handshake_str = delimiter + seq_no + delimiter;
					handshake_byte = Util.appendByte(handshake_byte, handshake_str.getBytes());
					byte[] enc_msg = khObj.encrypt(handshake_byte, CC_sessionKey);
					// TODO use objOut of the other client
					// objOut.writeObject(enc_msg);
					// objOut.writeObject(Util.computeSHA(Util.appendByte(enc_msg, CC_sessionKey)));
					// objOut.flush();

					// receiving challenge from other client
					seq_no++;
					Message handshake_msg = Util.recieveMsg(objIn);
					handshake_byte = khObj.decrypt(handshake_msg.getMsg().getBytes(), CC_sessionKey);
					handshake_msg = Util.recieveMsg(objIn);
					if(Util.checkIntegrity(handshake_byte, handshake_msg.getMsg().getBytes(), CC_sessionKey)){
						// TODO validate for seq_no in handshake_byte
					}

					// sending response to the challenge
					seq_no++;
					new SecureRandom().nextBytes(nonce);
					handshake_str = delimiter + seq_no + delimiter;
					handshake_byte = Util.appendByte(khObj.encrypt(nonce, CC_sessionKey), handshake_str.getBytes());
					enc_msg = khObj.encrypt(handshake_byte, CC_sessionKey);
					// TODO use objOut of the other client
					// objOut.writeObject(enc_msg);
					// objOut.writeObject(Util.computeSHA(Util.appendByte(enc_msg, CC_sessionKey)));
					// objOut.flush();

					// sending messages
					while(true){
						// TODO add seq_no to messages
						// TODO use objOut of the other client
						String str = cin.nextLine();
						if(str.equals("!quit")){
							break;
						}
						seq_no++;
						enc_msg = khObj.encrypt(str.getBytes(), CC_sessionKey);
						// TODO use objOut of the other client
						// objOut.writeObject(enc_msg);
						// objOut.writeObject(Util.computeSHA(Util.appendByte(enc_msg, CC_sessionKey)));
						// objOut.flush();

						Message client_msgs = Util.recieveMsg(objIn);
						byte[] decrypt_msg = khObj.decrypt(client_msgs.getMsg().getBytes(), CC_sessionKey);
						client_msgs = Util.recieveMsg(objIn);
						if(Util.checkIntegrity(decrypt_msg, client_msgs.getMsg().getBytes(), CC_sessionKey)){
							System.out.println("Other Client:" + decrypt_msg);
						}
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			else if(option == 2){
				break;
			}
			else{
				System.out.println("Wrong option value");
			}
		}

		// close the connection and streams
		closeOutputStreams();

		// TODO: delete, temp testing for concurrent client connections to server
		while(true) {
			try {
				Thread.sleep(4000);
			} catch (Exception e) {
				System.out.println(e);
			}
		}
		
	}

}
