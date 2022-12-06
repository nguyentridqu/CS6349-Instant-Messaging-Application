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
    private static int CCKeyLen = 64;
    private static int TTL = 2000;
    private static PrivateKey RSAprivateKey;
    private static PublicKey RSApublicKey;
    private static int port;
    private static String ip = null;
    private static ServerSocket serverSock;
    private static List<ClientObj> clientObjs =
            Collections.synchronizedList(new ArrayList<ClientObj>()); // thread safe tracking of clients
    private static AtomicInteger nextId = new AtomicInteger(1); // incrementor for clientObj IDs

    // displays the list of client objects, for testing
    private static void printClients() {
        System.out.println(clientObjs);
    }

    // spawn a new thread for every new client connect so they can be handled simultaneously
    private static class clientListener extends Thread {
        Socket socket = null;
        OutputStream out = null;
        ObjectOutputStream objOut = null;
        InputStream in = null;
        ObjectInputStream objIn = null;
        int thisClientId;

        // initialize thread with the socket from client that the server accepted
        public clientListener(Socket socket) {
            this.socket = socket;
        }

        public boolean validClient(int otherClientId) {
            boolean valid = false;
            for (ClientObj client : clientObjs) {
                if (client.getId() == otherClientId && !client.isBusy()) {
                    valid = true;
                    break;
                }
            }
            return valid && (otherClientId != thisClientId);
        }

        public ClientObj getClientById(int otherClientId) {
            for (ClientObj client : clientObjs) {
                if (client.getId() == otherClientId) {
                    return client;
                }
            }
            return null;
        }

        public String getClientIP(int otherClientId) {
            for (ClientObj client : clientObjs) {
                if (client.getId() == otherClientId) {
                    return client.getIp();
                }
            }
            return "";
        }

        public String getClientPort(int otherClientId) {
            for (ClientObj client : clientObjs) {
                if (client.getId() == otherClientId) {
                    return String.valueOf(client.getPort());
                }
            }
            return "";
        }

        public String getClientList() {
            String clientList = "";
            for (ClientObj client : clientObjs) {
                clientList += client.toString();
                // if (client.getId() == thisClientId) {
                //     clientList += " (self)";
                // }
                clientList += "\n";
            }
            return clientList;
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

        private byte[] doDHKeyExchange() {
            byte[] sessionKey = null;
            try {
                DHServer dh;
                // create DH keys and send public key to client
                dh = new DHServer(DHkeyLen);
                byte[] pubKey = dh.getKeyToSend();
                objOut.writeObject(pubKey);
                objOut.flush();
                System.out.println("Sent client DH public key");

                // receive client DH public key
                byte[] clientPubKey = (byte[]) objIn.readObject();
                System.out.println("Received client DH public key");

                // generate session key
                sessionKey = dh.computeSharedSecret(clientPubKey);


            } catch (Exception e) {
                e.printStackTrace();
            }
            return sessionKey;
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
            thisClientId = clientMsg.getId();

            // get public key of client
            RSAPublicKey clientRSAPubKey = Util.getPublicKey(clientMsg.getId());

            // respond to client with authentication challenge
            int challenge = Util.getRandom(100000);    // get rand number as challenge
            Message msg = new Message("");    // create message with encrypted challenge
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
            // printClients();

            // establish session key with client
            byte[] sessionKey = doDHKeyExchange();
            if (sessionKey == null) {
                System.out.println("Error establishing D-H key exchange");
                closeOutputStreams();
            }

            while (true) {
                try {
                    String clientChoice = Helper.recvDecrypt(objIn, sessionKey);
                    switch (clientChoice) {
                        case "getClientList":
                            Helper.sendEncrypt(objOut, getClientList(), sessionKey);
                            System.out.println("Sent client list to client " + thisClientId);
                            break;
                        case "talkToAnother":
                            String otherClient = Helper.recvDecrypt(objIn, sessionKey);
                            int otherClientID = Integer.parseInt(otherClient);
                            if (validClient(otherClientID)) {
                                Helper.sendEncrypt(objOut, "success", sessionKey);
                                System.out.println("success");

                                // get other client's IP address
                                String otherIP = getClientIP(otherClientID);
                                // get other client's port number
                                String otherPort = getClientPort(otherClientID);

                                // generate client-client session key
                                SecureRandom random = new SecureRandom();
                                byte[] CCkey = new byte[CCKeyLen];
                                random.nextBytes(CCkey);

                                // ticket for second client (encrypted session key with second's public key)
                                RSAPublicKey otherRSAPubKey = Util.getPublicKey(otherClientID);
                                byte[] ticket = Util.encrypt(CCkey, otherRSAPubKey);

                                // timestamp for freshness
                                long timestamp = System.currentTimeMillis();
                                byte[] ts = Helper.longToBytes(timestamp);

                                // send key, ticket, and timestamp
                                Helper.sendEncrypt(objOut, otherIP, sessionKey);
                                Helper.sendEncrypt(objOut, otherPort, sessionKey);
                                Helper.sendEncrypt(objOut, CCkey, sessionKey);
                                Helper.sendEncrypt(objOut, ticket, sessionKey);
                                Helper.sendEncrypt(objOut, ts, sessionKey);

                                // mark both clients as busy
                                getClientById(thisClientId).setIsBusy(true);
                                getClientById(otherClientID).setIsBusy(true);

                            } else {
                                Helper.sendEncrypt(objOut, "failure", sessionKey);
                                System.out.println("failure");
                            }

                            break;
                        default:
                            break;
                    }

                } catch (java.io.EOFException e) {
					break;
				} catch (Exception e) {
                    e.printStackTrace();
                    break;
                }

            }


            closeOutputStreams();
        } // end run


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
        while (true) {
            try {
                Socket socket = serverSock.accept();
                System.out.println("Client has successfully connected");
                clientListener serverThread = new clientListener(socket);
                serverThread.start();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Failed accept client connection and create server thread");
            }
        }
    }
}
