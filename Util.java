import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.interfaces.*;
import javax.crypto.Cipher;

public final class Util {
    private static String metaFile = "metadata.txt";
	private static String hashInKeyedHash = "SHA-256";

	// writes byte array to given filename
	public static void writeBytesToFile(String filename, byte[] data){
		try {
			FileOutputStream out = new FileOutputStream(filename);
			out.write(data);
			out.close();
		} catch (FileNotFoundException e) {
			System.out.println(filename + " file does not exist");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IO error from handler of file: " + filename);
			e.printStackTrace();
		}
	}

	// read all file to byte array for given filename
	public static byte[] readBytesFromFile(String filename){
		byte[] data = new byte[0];
		try {
			data = Files.readAllBytes(Paths.get(filename));
		} catch (IOException e) {
			System.out.println("IO error from handler of file: " + filename);
			e.printStackTrace();
		}
		return data;
	}

	// generate RSA public and private key and write it to file
	public static KeyPair generateRSAKeyPair(int keyLen, int ID){
		KeyPair pair = null;
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(keyLen);
			pair = kpg.generateKeyPair();
		} catch (NoSuchAlgorithmException e) {
			System.out.println("RSA key pair generation failed ");
			e.printStackTrace();
		}
		return pair;
	}

	// gets all the hosts of the current servers from metaFile
	public static InetAddress getServerIp() {
		InetAddress ip = null;
		try {
			// open file and create scanner for file
			File file = new File(metaFile);
			Scanner fileReader = new Scanner(file);

			// read ip and port of server
			String metadata = fileReader.nextLine();
			// get data before comma in metaFile, which is the host
			ip = InetAddress.getByName(metadata.substring(0, metadata.indexOf(",")));
			
			fileReader.close();
		} catch (FileNotFoundException e) {
			System.out.println("Failed to get hosts from " + metaFile);
			e.printStackTrace();
		} catch (UnknownHostException e) {
			System.out.println("Failed to get ip from string ");
			e.printStackTrace();
		}
		return ip;
	}

    // gets all the ports of the current servers from metaFile
	public static int getServerPort() {
		int ports = 0;
		try {
			// open file and create scanner for file
			File file = new File(metaFile);
			Scanner fileReader = new Scanner(file);
			String metadata;
			// while file has more ports, read them
			while (fileReader.hasNextLine()) {
				metadata = fileReader.nextLine();
				// get data after comma in metaFile, which is the port number
				metadata = metadata.substring(metadata.indexOf(",")+1, metadata.length());
				ports = Integer.parseInt(metadata);
			}
			fileReader.close();
		} catch (FileNotFoundException e) {
			System.out.println("Failed to get server port from " + metaFile);
			e.printStackTrace();
		}
		return ports;
	}

    // writes a given host and port to a txt file so other clients can locate this server
	public static void writeServerMetadata(String host, int port) {
		try {
			//System.out.println("Writing to file");
			BufferedWriter writer = new BufferedWriter(new FileWriter(metaFile, false));
			writer.write(host + ",");
			writer.write(String.valueOf(port));
			writer.newLine();
			writer.close();
		} catch (IOException e) {
			System.out.println("Failed to write port to " + metaFile);
			e.printStackTrace();
		}
	}

	public static byte[] computeSHA(byte[] inp){
		byte[] res = null;
		try{
			MessageDigest digest = MessageDigest.getInstance(hashInKeyedHash);
			res = digest.digest(inp);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		return res;
	}

	public static byte[] appendByte(byte[] b1, byte[] b2){
		byte[] b3 = null;
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			outputStream.write(b1);
			outputStream.write(b2);
			b3 = outputStream.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return b3;
	}

	public static boolean checkIntegrity(byte[] str, byte[] hash, byte[] key){
		if(Arrays.equals(hash, Util.computeSHA(Util.appendByte(str, key)))){
			return true;
		}
		else{
			throw new RuntimeException("Message Integrity is violated.");
		}
	}

	// write a message object to an output stream
	public static void sendMsg(ObjectOutputStream objOut, Message msg) {
		try {
			objOut.writeObject(msg);
		} catch (Exception e) {
			System.out.println("Failed to write to output stream");
			System.out.println(e);
		}
	}

	// write a message object to an output stream
	public static Message recieveMsg(ObjectInputStream objIn) {
		try {
			Message msg = (Message) objIn.readObject();
			return msg;
		} catch (Exception e) {
			System.out.println("Failed to get object from server socket");
			System.out.println(e);
		}
		return null;
	}

	// gets a random value between 0 and upperBound, used for auth
	public static int getRandom(int upperBound) {
		Random rand = new Random();

		// get a int between [0 to (upperBound - 1)].
		return rand.nextInt(upperBound);
	}

	// get a public key from a file
	public static RSAPublicKey getPublicKey(int id) {
		File pubKeyFile = new File("keys/public_key_" + id + ".key");
		byte[] pubKeyBytes = null;
		KeyFactory keyFactory = null;
		RSAPublicKey pubKey = null;

		// read public key DER file
		try {
			keyFactory = KeyFactory.getInstance("RSA");
			DataInputStream dis = new DataInputStream(new FileInputStream(pubKeyFile));
			pubKeyBytes = new byte[(int)pubKeyFile.length()];
			dis.readFully(pubKeyBytes);
			dis.close();
		} catch (Exception e) {
			System.out.println("Failed to read public key from file");
			System.out.println(e);
		}
		
		// decode public key
		try{
			X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(pubKeyBytes);
			pubKey = (RSAPublicKey) keyFactory.generatePublic(pubSpec);
		} catch (Exception e) {
			System.out.println("Failed to decode public key");
			System.out.println(e);
		}

		return pubKey;
	}

	public static RSAPrivateKey getPrivatKey(int id) {
		File privKeyFile = new File("keys/private_key_" + id + ".key");
		byte[] privKeyBytes = null;
		KeyFactory keyFactory = null;
		RSAPrivateKey privKey = null;

		// read private key DER file
		try {
			keyFactory = KeyFactory.getInstance("RSA");
			DataInputStream dis = new DataInputStream(new FileInputStream(privKeyFile));
			privKeyBytes = new byte[(int)privKeyFile.length()];
			dis.read(privKeyBytes);
			dis.close();
		} catch (Exception e) {
			System.out.println("Failed to read public key from file");
			System.out.println(e);
		}

		// decode private key
		try{
			PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privKeyBytes);
            privKey = (RSAPrivateKey) keyFactory.generatePrivate(privSpec);
		} catch (Exception e) {
			System.out.println("Failed to decode private key");
			System.out.println(e);
		}

		return privKey;
	}

	public static byte[] encrypt(String message, RSAPublicKey publicKey) {
		try {
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			return cipher.doFinal(message.getBytes());
		} catch (Exception e) {
			System.out.println("Failed to encrypt message");
			System.out.println(e);
		}

		return null;
	}

	public static byte[] encrypt(byte[] message, RSAPublicKey publicKey) {
		try {
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			return cipher.doFinal(message);
		} catch (Exception e) {
			System.out.println("Failed to encrypt message");
			System.out.println(e);
		}

		return null;
	}

	public static String decrypt(byte[] message, RSAPrivateKey privateKey) {
		try{
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			return new String(cipher.doFinal(message));
		} catch (Exception e) {
			System.out.println("Failed to encrypt message");
			System.out.println(e);
		}

		return null;
	}

	public static void writePublicKey(int id, PublicKey key) {
		try (FileOutputStream fos = new FileOutputStream("public_key_" + id + ".key")) {
			fos.write(key.getEncoded());
		} catch (Exception e) {
			System.out.println("Failed to write public key to file");
			System.out.println(e);
		}
	}

	public static void writePrivateKey(int id, PrivateKey key) {
		try (FileOutputStream fos = new FileOutputStream("private_key_" + id + ".key")) {
			fos.write(key.getEncoded());
		} catch (Exception e) {
			System.out.println("Failed to write private key to file");
			System.out.println(e);
		}
	}

	public static void printClientList(ArrayList<ClientObj> clientList) {
		String output = "";

		for(int i = 0; i < clientList.size(); i++){
			ClientObj curClient = clientList.get(i);
			if(curClient.isBusy()) {
				output += "Client ID: " + curClient.getId() + " | Status: busy | IP: " + curClient.getIp() + ":" + curClient.getPort();
			} else {
				output += "Client ID: " + curClient.getId() + " | Status: idle | IP: " + curClient.getIp() + ":" + curClient.getPort();
			}
			output += "\n";
		}

		System.out.println(output);
	}

	public static ArrayList<ClientObj> buildClientList(String input) {
		ArrayList<ClientObj> clientList = new ArrayList<>();
		List<String> clientInfo = Arrays.asList(input.split("\n"));
		// System.out.println(input);

		for(int i = 0; i < clientInfo.size(); i++){
			String[] tokens = clientInfo.get(i).split(",");
			ClientObj curClient = new ClientObj(Integer.parseInt(tokens[0]), tokens[2], Integer.parseInt(tokens[3]));

			if(tokens[1].equals("busy")){
				curClient.setIsBusy(true);
			} else {
				curClient.setIsBusy(false);
			}

			clientList.add(curClient);
		}

		return clientList;
	}

	public static ClientObj getClientObj(ArrayList<ClientObj> clientList, int clientId) throws ArrayIndexOutOfBoundsException {
		for(int i = 0; i < clientList.size(); i++){
			if(clientList.get(i).getId() == clientId) {
				return clientList.get(i);
			}
		}

		throw new ArrayIndexOutOfBoundsException();
	}
}