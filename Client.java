import java.net.*;
import java.io.*;

public class Client {

	public static void main(String[] args) {
		Socket socket = null;
		OutputStream out = null;
		ObjectOutputStream objOut = null;
		InputStream in = null;
		ObjectInputStream objIn = null;
		
		// get servers ports so connections can be made
		int serverPort = Util.getServerPort();
		InetAddress serverIp = Util.getServerIp();

		System.out.println("Server is at " + serverIp + ":" + serverPort);

		// try to connect to server
		try {
			socket = new Socket(serverIp, serverPort);
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

		// try to write to output
		try {
			Message msg = new Message();
			objOut.writeObject(msg);
			System.out.println("Sent message");
		} catch (Exception e) {
			System.out.println("Failed to write to output stream");
			System.out.println(e);
		}

		// read response
		try {
			Message msg = (Message) objIn.readObject();
			System.out.println("Received message");
		} catch (Exception e) {
			System.out.println("Failed to get object from server socket");
			e.printStackTrace();
		}

		// close the connection and streams
		try
		{
			objOut.close();
			out.flush();
			out.close();
			objIn.close();
			in.close();
			socket.close();
		}
		catch(IOException i)
		{
			System.out.println(i);
		}
		
	}
}