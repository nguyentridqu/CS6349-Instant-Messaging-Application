# CS6349-Instant-Messaging-Application

### Compilation

use ```make```

or manually compile with ```javac Server.java``` and ```javac Client.java```

### Run Server and Clients

1. Run the server first with ID of 0 via the command ```java Server 0```
2. Run each client with their ID as an argument starting from 1 (i.e. ```java Client 1```, ```java Client 2```,... )

### Chatting functionality

A command line interface will be shown for each client process with options (1-4):

1 - Obtain a list of available clients from the server and their statuses.

2 - Connect to another client. After choosing this option, specify the ID of the other client to connect to.

3 - Disconnect from the server

4 - This option is for the second client in each pair to stop receiving inputs to send to the server and start accepting
inputs to send to the first client.

### Steps to connect between clients

Once a connection to the server is established

1. Enter option '1' to obtain a list of other clients and their ID
2. Enter option '2' to initiate a session to another client
    * Enter the ID of the other client to connect to
3. A connection will be established between the current client (client 1) and the other client (client 2)
4. Client 2 must enter option 4 to stop receiving inputs to send to server and sstart accepting inputs to send to the
   client 1.
5. The two clients will take turn sending messages
    * Client 2 must be the first to send a message
    * Then client 1 will send a message
    * Then client 2 will send a message
    * ...