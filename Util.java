import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.security.*;

public final class Util {
    private static String metaFile = "metadata.txt";
	private static String public_key_filename_base = "public_key_";
	private static String private_key_filename_base = "private_key_";

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

	// read public key from file given server/client ID
	public static PublicKey getPublicKey(int ID){
		byte[] key_bytes = readBytesFromFile(public_key_filename_base + ID);
		X509EncodedKeySpec ks = new X509EncodedKeySpec(key_bytes);
		PublicKey key;
		try {
			KeyFactory kf = KeyFactory.getInstance("RSA");
			key = kf.generatePublic(ks);
		} catch (InvalidKeySpecException e) {
			throw new RuntimeException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		return key;
	}

	// read private key from file given server/client ID
	public static PrivateKey getPrivateKey(int ID){
		byte[] key_bytes = readBytesFromFile(private_key_filename_base + ID);
		X509EncodedKeySpec ks = new X509EncodedKeySpec(key_bytes);
		PrivateKey key;
		try {
			KeyFactory kf = KeyFactory.getInstance("RSA");
			key = kf.generatePrivate(ks);
		} catch (InvalidKeySpecException e) {
			throw new RuntimeException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		return key;
	}

	// generate RSA public and private key and write it to file
	public static void generateRSAKeyPairAndSaveToFile(int keyLen, int ID){
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(keyLen);
			KeyPair pair = kpg.generateKeyPair();
			PublicKey publicKey = pair.getPublic();
			PrivateKey privateKey = pair.getPrivate();

			writeBytesToFile(public_key_filename_base + ID, publicKey.getEncoded());
			writeBytesToFile(private_key_filename_base + ID, privateKey.getEncoded());

		} catch (NoSuchAlgorithmException e) {
			System.out.println("RSA key pair generation failed ");
			e.printStackTrace();
		}
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