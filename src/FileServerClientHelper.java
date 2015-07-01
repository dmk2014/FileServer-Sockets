import java.net.*;
import java.io.*;

public class FileServerClientHelper {
    private FileServerClientDatagramSocket mySocket;
    private InetAddress serverHost;
    private int serverPort;

    FileServerClientHelper(String hostName, String portNum)
            throws SocketException, UnknownHostException {
        this.serverHost = InetAddress.getByName(hostName);
        this.serverPort = Integer.parseInt(portNum);
        // instantiates a datagram socket for both sending
        // and receiving data
        this.mySocket = new FileServerClientDatagramSocket();
    }

    public String getResponse(String message)
            throws SocketException, IOException {
        String echo = "";
        mySocket.sendMessage(serverHost, serverPort, message);
        // now receive the echo
        echo = mySocket.receiveMessage();
        return echo;
    } //end getResponse

    public void done() throws SocketException {
        mySocket.close();
    }  //end done

}