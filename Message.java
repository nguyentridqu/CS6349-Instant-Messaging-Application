import java.io.Serializable;

public class Message implements Serializable {
	private String ip;
	private int port;
	private String msg;

	public Message(String msg) {
		this.msg = msg;
	}

	// getter and setter functions
	public void setIp(String ip) {
		this.ip = ip; 
	}

	public String getIp() {
		return ip;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getPort() {
		return port;
	}

	public String getMsg() {
		return msg;
	}

	public void appendMsg(String m) {
		msg += m;
	}

	public static void main(String[] args) {

	}

	@Override
	public String toString() {
		return msg;
	}
}