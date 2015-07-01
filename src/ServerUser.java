import java.net.*;

public class ServerUser{
    private String username;
    private InetAddress address;
    private int port;

    public ServerUser(String username, InetAddress address, int port) {
        this.username = username.trim();
        this.port = port;
        this.address = address;
    }

    public String getUsername() {
        return username;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @java.lang.Override
    public String toString() {
        return "ServerUser{" +
                "username='" + username + '\'' +
                ", address='" + address.toString() + '\'' +
                ", port='" + port + '\'' +
                '}';
    }
}