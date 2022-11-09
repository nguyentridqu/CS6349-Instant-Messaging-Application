import java.net.*;
import java.io.*;
import java.util.*;

public final class Util {
    private static String metaFile = "metadata.txt";

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
}