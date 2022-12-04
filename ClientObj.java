
// client objects used to keep track of clients within the server
public class ClientObj {
    private int id;                 // client id
    private boolean busy = false;   // is client already talking to another client
    private String ip = null;       // ip address of client
    private int port;               // port of client

    public ClientObj (int id, String ip, int port) {
        this.id = id;
        this.ip = ip;
        this.port = port;
    }

    // getter and setter functions
    public void setStatus (boolean busy) {
        this.busy = busy;
    }

    public boolean getStatus () {
        return busy;
    }

    public int getId () {
        return id;
    }

    public String getIp () {
        return ip;
    }

    public boolean getPort () {
        return busy;
    }

    // overwride toString for printing message type
    @Override
	public String toString() {
        if(busy) {
            return "Client ID: " + id + " | Status: busy | IP: " + ip + ":" + port;
        } else {
            return "Client ID: " + id + " | Status: available | IP: " + ip + ":" + port;
        }
	}
}
