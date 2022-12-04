import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.security.*;

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
	public static KeyPair generateRSAKeyPairAndSaveToFile(int keyLen, int ID){
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
}