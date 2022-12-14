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
    public void setIsBusy (boolean busy) {
        this.busy = busy;
    }

    public boolean isBusy () {
        return busy;
    }

    public int getId () {
        return id;
    }

    public String getIp () {
        return ip;
    }

    public int getPort () {
        return port;
    }

    // overwride toString for printing message type
    @Override
	public String toString() {
        if(busy) {
            return id + ",busy," + ip + "," + port;
        } else {
            return id + ",idle," + ip + "," + port;
        }
	}
}
