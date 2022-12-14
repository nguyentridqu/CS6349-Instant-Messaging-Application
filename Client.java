import javax.crypto.Cipher;
import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.security.interfaces.*;

public class Client {
    private static int clientID;
    private static int RSAkeyLen = 1024;
    private static int DHkeyLen = 512;
    private static int TTL = 2000;
    private static RSAPrivateKey RSAprivateKey;
    private static RSAPublicKey RSApublicKey;
    private static ServerSocket clientSock;
    private static String ip;
    private static int port;
    // server connections
    private static Socket socket = null;
    private static OutputStream out = null;
    private static ObjectOutputStream objOut = null;
    private static InputStream in = null;
    private static ObjectInputStream objIn = null;
    // client connections
    private static Socket clientSocket = null;
    private static OutputStream clientOut = null;
    private static ObjectOutputStream clientObjOut = null;
    private static InputStream clientIn = null;
    private static ObjectInputStream clientObjIn = null;
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
            } catch (Exception e) {
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
                int seq_no = 1;
                // receive ticket from other client
                byte[] ticket = (byte[]) clientObjIn.readObject();
                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.DECRYPT_MODE, RSAprivateKey);
                byte[] CC_sessionKey = cipher.doFinal(ticket);

                byte[] handshake_byte = (byte[]) clientObjIn.readObject();
                byte[] sha_msg = (byte[]) clientObjIn.readObject();
                long challenge_recv = 0;
                if (Util.checkIntegrity(handshake_byte, sha_msg, CC_sessionKey)) {
                    handshake_byte = khObj.decrypt(handshake_byte, CC_sessionKey);
                    String handshake_str = new String(handshake_byte);
                    String[] chunks = handshake_str.split("[" + delimiter + "]");
                    int sq_no_came = Integer.parseInt(chunks[1]);
                    if (sq_no_came != seq_no) {
                        throw new Exception("Sequence number mismatch");


                    }
                    challenge_recv = Long.parseLong(chunks[0]) - 1;
                }

                // sending challenge
                seq_no++;
                long nonce = new Random().nextLong();
                String handshake_str = challenge_recv + delimiter + nonce + delimiter + seq_no + delimiter;
                handshake_byte = handshake_str.getBytes();
                byte[] enc_msg = khObj.encrypt(handshake_byte, CC_sessionKey);
                clientObjOut.writeObject(enc_msg);
                clientObjOut.writeObject(Util.computeSHA(Util.appendByte(enc_msg, CC_sessionKey)));
                clientObjOut.flush();

                // receiving response of challenge
                seq_no++;
                handshake_byte = (byte[]) clientObjIn.readObject();
                sha_msg = (byte[]) clientObjIn.readObject();
                if (Util.checkIntegrity(handshake_byte, sha_msg, CC_sessionKey)) {
                    // validate for seq_no and nonce in handshake_byte
                    byte[] decrypt_msg = khObj.decrypt(handshake_byte, CC_sessionKey);
                    handshake_str = new String(decrypt_msg);
                    String[] chunks = handshake_str.split("[" + delimiter + "]");
                    int sq_no_came = Integer.parseInt(chunks[1]);
                    if (sq_no_came != seq_no) {
                        throw new Exception("Sequence number mismatch");
                    }
                    long nonce_recv = Long.parseLong(chunks[0]);
                    if (nonce_recv != nonce - 1) {
                        throw new Exception("Nonce mismatch");
                    }
                }

                // exchange messages
                Scanner cin = new Scanner(System.in);
                System.out.println("Connection established with other client, start chatting...");
                int skip = 0;
                while (true) {
                    seq_no++;
                    byte[] client_msgs = (byte[]) clientObjIn.readObject();
                    byte[] sha = (byte[]) clientObjIn.readObject();
                    if (Util.checkIntegrity(client_msgs, sha, CC_sessionKey)) {
                        byte[] decrypt_msg = khObj.decrypt(client_msgs, CC_sessionKey);
                        String str = new String(decrypt_msg);
                        String[] chunks = str.split("[" + delimiter + "]");
                        int sq_no_came = Integer.parseInt(chunks[1]);
                        if (sq_no_came != seq_no) {
                            throw new Exception("Sequence number mismatch");
                        }
                        if (skip == 0) {
                            skip++;
                        } else {
                            System.out.println("Other Client:" + chunks[0]);
                        }
                    }

                    System.out.print("You: ");
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
        msg.setId(clientID);

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

    // make in/out streams for server
    private static void connectToClient(String ip, String port) {
        // get servers ports so connections can be made
        int clientPort = Integer.parseInt(port);

        System.out.println("Client is at " + ip + ":" + clientPort);

        // try to connect to server
        try {
            clientSocket = new Socket(ip, clientPort);
            System.out.println("Successfully connected to client ");
        } catch (Exception e) {
            System.out.println("Failed to connect to client ");
            System.out.println(e);
        }

        // create in and out streams
        try {
            clientOut = clientSocket.getOutputStream();
            clientObjOut = new ObjectOutputStream(clientOut);
            clientIn = clientSocket.getInputStream();
            clientObjIn = new ObjectInputStream(clientIn);
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


    public static void main(String[] args) throws Exception {
        clientID = Integer.parseInt(args[0]);

        // get client keys from file
        RSApublicKey = Util.getPublicKey(clientID);
        RSAprivateKey = Util.getPrivatKey(clientID);

        // make in/out streams for server
        connectToServer();

        // start listening for client connections
        Message msg = createClientListener();
        ClientListener clientThread = new ClientListener();
        clientThread.start();

        // send to server what ip and port im listening for clients on
        Util.sendMsg(objOut, msg);
        System.out.println("Sent message");

        // read challenge from server
        Message serverMsg = Util.recieveMsg(objIn);
        System.out.println("Received encrypted challenge: " + Helper.bytesToHexString(serverMsg.getChallenge()));

        // decrypt challenge
        String challenge = Util.decrypt(serverMsg.getChallenge(), RSAprivateKey);
        System.out.println("Decrypted challenge: " + challenge);

        // send decrypted challenge back to server
        serverMsg = new Message(challenge);
        Util.sendMsg(objOut, serverMsg);

        byte[] sessionKey;
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
            sessionKey = dh.computeSharedSecret();

            KeyedHash khObj = new KeyedHash();
            while (true) {

                System.out.println("Choose the option:\n" +
                        "1 - Get list of other clients\n" +
                        "2 - Connect to another client\n" +
                        "3 - Disconnect\n" +
                        "4 - Chill out");
                Scanner cin = new Scanner(System.in);
                int option = cin.nextInt();

                if (option == 1) {
                    // send to server request for list of clients
                    try {
                        Helper.sendEncrypt(objOut, "getClientList", sessionKey);

                        String clientList = Helper.recvDecrypt(objIn, sessionKey);

                        // System.out.println("Client list:\n" + clientList);
                        ArrayList<ClientObj> clients = Util.buildClientList(clientList);
                        Util.printClientList(clients, clientID);

                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }

                } else if (option == 2) {

                    Helper.sendEncrypt(objOut, "talkToAnother", sessionKey);
                    System.out.print("Enter the client ID you want to connect to: ");
                    int other_client_id = cin.nextInt();

                    Helper.sendEncrypt(objOut, Integer.toString(other_client_id), sessionKey);
                    String response = Helper.recvDecrypt(objIn, sessionKey);
                    if (response.equals("failure")) {
                        System.out.println("Invalid ID or other client is busy");
                        continue;
                    }

                    String otherIP = Helper.recvDecrypt(objIn, sessionKey);
                    String otherPort = Helper.recvDecrypt(objIn, sessionKey);
                    byte[] CC_sessionKey = Helper.recvDecryptBytes(objIn, sessionKey);
                    byte[] ticket_byte = Helper.recvDecryptBytes(objIn, sessionKey);
                    byte[] ts = Helper.recvDecryptBytes(objIn, sessionKey);

                    long currentTime = System.currentTimeMillis();
                    long timeStamp = Helper.bytesToLong(ts);
                    if (timeStamp + TTL < currentTime) {
                        throw new Exception("TTL expires");
                    }

                    System.out.println("Other IP: " + otherIP + ":" + otherPort);
                    System.out.println("Session Key: " + Helper.bytesToHexString(CC_sessionKey));
                    System.out.println("Ticket: " + Helper.bytesToHexString(ticket_byte));
                    System.out.println("Time stamp: " + timeStamp);

                    // establish socket connection with other client
                    connectToClient(otherIP, otherPort);

                    int seq_no = 0;
                    // sending ticket to other client
                    seq_no++;
                    long nonce = new Random().nextLong();
                    String handshake_str = nonce + delimiter + seq_no + delimiter;
                    byte[] handshake_byte = handshake_str.getBytes();
                    byte[] enc_msg = khObj.encrypt(handshake_byte, CC_sessionKey);
                    clientObjOut.writeObject(ticket_byte);
                    clientObjOut.writeObject(enc_msg);
                    clientObjOut.writeObject(Util.computeSHA(Util.appendByte(enc_msg, CC_sessionKey)));
                    clientObjOut.flush();

                    // receiving challenge from other client
                    seq_no++;
                    byte[] handshake_msg = (byte[]) clientObjIn.readObject();
                    byte[] sha = (byte[]) clientObjIn.readObject();
                    long challenge_recv = 0;
                    if (Util.checkIntegrity(handshake_msg, sha, CC_sessionKey)) {
                        // validate for seq_no and nonce in handshake_byte
                        byte[] decrypt_msg = khObj.decrypt(handshake_msg, CC_sessionKey);
                        handshake_str = new String(decrypt_msg);
                        String[] chunks = handshake_str.split("[" + delimiter + "]");
                        int sq_no_came = Integer.parseInt(chunks[2]);
                        if (sq_no_came != seq_no) {
                            throw new Exception("Sequence number mismatch");
                        }

                        long nonce_recv = Long.parseLong(chunks[0]);
                        if (nonce_recv != nonce - 1) {
                            throw new Exception("Nonce mismatch");
                        }
                        challenge_recv = Long.parseLong(chunks[1]) - 1;
                    }

                    // sending response to the challenge
                    seq_no++;
                    handshake_str = challenge_recv + delimiter + seq_no + delimiter;
                    handshake_byte = handshake_str.getBytes();
                    enc_msg = khObj.encrypt(handshake_byte, CC_sessionKey);
                    clientObjOut.writeObject(enc_msg);
                    clientObjOut.writeObject(Util.computeSHA(Util.appendByte(enc_msg, CC_sessionKey)));
                    clientObjOut.flush();

                    // sending messages
                    System.out.println("Connection established with other client, waiting for other client to start chatting...");
                    int skip = 0;
                    while (true) {
                        if (skip == 0) {
                            skip++;
                        } else {
                            System.out.print("You: ");
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

                        seq_no++;
                        byte[] client_msgs = (byte[]) clientObjIn.readObject();
                        sha = (byte[]) clientObjIn.readObject();
                        if (Util.checkIntegrity(client_msgs, sha, CC_sessionKey)) {
                            byte[] decrypt_msg = khObj.decrypt(client_msgs, CC_sessionKey);
                            str = new String(decrypt_msg);
                            String[] chunks = str.split("[" + delimiter + "]");
                            int sq_no_came = Integer.parseInt(chunks[1]);
                            if (sq_no_came != seq_no) {
                                throw new Exception("Sequence number mismatch");
                            }
                            System.out.println("Other Client: " + chunks[0]);
                        }
                    }
                } else if (option == 3) {
                    break;
                } else if (option == 4) {
                    break;
                } else {
                    System.out.println("Wrong option value");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            closeOutputStreams();
        }


        // close the connection and streams
        closeOutputStreams();
    }
}
