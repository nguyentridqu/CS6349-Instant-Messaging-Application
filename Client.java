import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Random;
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

			KeyedHash khObj = new KeyedHash();
			try {
				int seq_no = 0;
				// receive ticket from other client
				Message ticket = Util.recieveMsg(clientObjIn);
				// TODO extract key from ticket
				byte[] CC_sessionKey = new byte[128];
				Message handshake_msg = Util.recieveMsg(clientObjIn);
				byte[] handshake_byte = khObj.decrypt(handshake_msg.getMsg().getBytes(), CC_sessionKey);
				String handshake_str = new String(handshake_byte);
				Message sha_msg = Util.recieveMsg(clientObjIn);
				long challenge_recv = 0;
				if(Util.checkIntegrity(handshake_byte, sha_msg.getMsg().getBytes(), CC_sessionKey)){
					String[] chunks = handshake_str.split("[" + delimiter + "]");
					int sq_no_came = Integer.parseInt(chunks[1]);
					if(sq_no_came != seq_no){
						throw new Exception("Sequence number mismatch");

					}
					challenge_recv = Long.parseLong(chunks[0]) - 1;
				}

				// sending challenge
				seq_no++;
				long nonce = new Random().nextLong();
				handshake_str = challenge_recv + delimiter + nonce + delimiter + seq_no + delimiter;
				handshake_byte = handshake_str.getBytes();
				byte[] enc_msg = khObj.encrypt(handshake_byte, CC_sessionKey);
				clientObjOut.writeObject(enc_msg);
				clientObjOut.writeObject(Util.computeSHA(Util.appendByte(enc_msg, CC_sessionKey)));
				clientObjOut.flush();

				// receiving response of challenge
				seq_no++;
				handshake_msg = Util.recieveMsg(clientObjIn);
				handshake_byte = khObj.decrypt(handshake_msg.getMsg().getBytes(), CC_sessionKey);
				handshake_msg = Util.recieveMsg(clientObjIn);
				if(Util.checkIntegrity(handshake_byte, handshake_msg.getMsg().getBytes(), CC_sessionKey)){
					// validate for seq_no and nonce in handshake_byte
					handshake_str = new String(handshake_byte);
					String[] chunks = handshake_str.split("[" + delimiter + "]");
					int sq_no_came = Integer.parseInt(chunks[1]);
					if(sq_no_came != seq_no){
						throw new Exception("Sequence number mismatch");
					}
					long nonce_recv = Long.parseLong(chunks[0]);
					if(nonce_recv != nonce - 1){
						throw new Exception("Nonce mismatch");
					}
				}

				// exchange messages
				Scanner cin = new Scanner(System.in);
				while(true) {
					seq_no++;
					Message client_msgs = Util.recieveMsg(clientObjIn);
					byte[] decrypt_msg = khObj.decrypt(client_msgs.getMsg().getBytes(), CC_sessionKey);
					client_msgs = Util.recieveMsg(clientObjIn);
					if (Util.checkIntegrity(decrypt_msg, client_msgs.getMsg().getBytes(), CC_sessionKey)) {
						String str = new String(decrypt_msg);
						String[] chunks = str.split("[" + delimiter + "]");
						int sq_no_came = Integer.parseInt(chunks[1]);
						if (sq_no_came != seq_no) {
							throw new Exception("Sequence number mismatch");
						}

						System.out.println("Other Client:" + chunks[0]);
					}


					String str = cin.nextLine();
					if (str.equals("!quit")) {
						break;
					}
					seq_no++;
					str = str + delimiter + seq_no + delimiter;
					enc_msg = khObj.encrypt(str.getBytes(), CC_sessionKey);
					clientObjOut.writeObject(enc_msg);
					clientObjOut.writeObject(Util.computeSHA(Util.appendByte(enc_msg, CC_sessionKey)));
					clientObjOut.flush();
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
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
					long nonce = new Random().nextLong();
					String handshake_str = nonce + delimiter + seq_no + delimiter;
					byte[] ticket_byte = Ticket.getMsg().getBytes();
					byte[] handshake_byte = handshake_str.getBytes();
					byte[] enc_msg = khObj.encrypt(handshake_byte, CC_sessionKey);
					clientThread.clientObjOut.writeObject(ticket_byte);
					clientThread.clientObjOut.writeObject(enc_msg);
					clientThread.clientObjOut.writeObject(Util.computeSHA(Util.appendByte(enc_msg, CC_sessionKey)));
					clientThread.clientObjOut.flush();

					// receiving challenge from other client
					seq_no++;
					Message handshake_msg = Util.recieveMsg(clientThread.clientObjIn);
					handshake_byte = khObj.decrypt(handshake_msg.getMsg().getBytes(), CC_sessionKey);
					handshake_msg = Util.recieveMsg(clientThread.clientObjIn);
					long challenge_recv = 0;
					if(Util.checkIntegrity(handshake_byte, handshake_msg.getMsg().getBytes(), CC_sessionKey)){
						// validate for seq_no and nonce in handshake_byte
						handshake_str = new String(handshake_byte);
						String[] chunks = handshake_str.split("[" + delimiter + "]");
						int sq_no_came = Integer.parseInt(chunks[2]);
						if(sq_no_came != seq_no){
							throw new Exception("Sequence number mismatch");
						}

						long nonce_recv = Long.parseLong(chunks[0]);
						if(nonce_recv != nonce - 1){
							throw new Exception("Nonce mismatch");
						}

						challenge_recv = Long.parseLong(chunks[1]) - 1;
					}

					// sending response to the challenge
					seq_no++;
					handshake_str = challenge_recv + delimiter + seq_no + delimiter;
					handshake_byte = handshake_str.getBytes();
					enc_msg = khObj.encrypt(handshake_byte, CC_sessionKey);
					clientThread.clientObjOut.writeObject(enc_msg);
					clientThread.clientObjOut.writeObject(Util.computeSHA(Util.appendByte(enc_msg, CC_sessionKey)));
					clientThread.clientObjOut.flush();

					// sending messages
					while(true){
						String str = cin.nextLine();
						if(str.equals("!quit")){
							break;
						}
						seq_no++;
						str = str + delimiter + seq_no + delimiter;
						enc_msg = khObj.encrypt(str.getBytes(), CC_sessionKey);
						clientThread.clientObjOut.writeObject(enc_msg);
						clientThread.clientObjOut.writeObject(Util.computeSHA(Util.appendByte(enc_msg, CC_sessionKey)));
						clientThread.clientObjOut.flush();

						seq_no++;
						Message client_msgs = Util.recieveMsg(clientThread.clientObjIn);
						byte[] decrypt_msg = khObj.decrypt(client_msgs.getMsg().getBytes(), CC_sessionKey);
						client_msgs = Util.recieveMsg(clientThread.clientObjIn);
						if(Util.checkIntegrity(decrypt_msg, client_msgs.getMsg().getBytes(), CC_sessionKey)){
							str = new String(decrypt_msg);
							String[] chunks = str.split("[" + delimiter + "]");
							int sq_no_came = Integer.parseInt(chunks[1]);
							if(sq_no_came != seq_no){
								throw new Exception("Sequence number mismatch");
							}

							System.out.println("Other Client:" + chunks[0]);
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
