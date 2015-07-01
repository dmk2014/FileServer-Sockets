import java.io.*;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class FileServer {
    static FileServer fileServer;
    List<ServerUser> connectedUsers = new ArrayList<ServerUser>();
    final String argumentDivider = "~~~";
    final String STORAGE_DIRECTORY = "C:/fileserver/";

    public static void main(String[] args) {
        int serverPort = 30000;    // default port

        if (args.length == 1)
            serverPort = Integer.parseInt(args[0]);

        fileServer = new FileServer();

        try {
            // instantiates a datagram socket for both sending and receiving data
            FileServerDatagramSocket mySocket = new FileServerDatagramSocket(serverPort);
            System.out.println("Server ready.");

            while (true) {  // forever loop
                DatagramMessage request = mySocket.receiveMessageAndSender();
                System.out.println("\nRequest received");

                //read request command
                String[] message = request.getMessage().split("~~~");
                int requestCode = Integer.parseInt(message[0]);
                String messageData = message[1].trim(); //*** PAYLOAD IS 3 ITEMS FOR SOME REQUESTS

                System.out.println("Command received: " + requestCode + "\nMessage Data: " + messageData);

                //FOR UPLOAD/DOWNLOAD/DISCONNECT WE NEED TO KNOW THE USERNAME
                String username = fileServer.getUsername(request.getAddress(), request.getPort());
                int responseCode = fileServer.processRequest(username, request, message);
                String responseMessage = fileServer.generateResponse(username, responseCode, messageData);

                mySocket.sendMessage(request.getAddress(),request.getPort(), responseMessage);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public int processRequest(String username, DatagramMessage request, String[] message)
    {
        String requestData = message[1].trim();
        int requestCode = Integer.parseInt(message[0]);
        int responseCode = 0;

        if (requestCode == ServerCode.CONNECT_REQUEST) {
            responseCode = fileServer.connectUser(requestData.toLowerCase(), request.getAddress(), request.getPort());
        }
        else if (username.equals("")){
            //If the requesting address has no username it means the user hasn't connecting
            //and cannot access further features
            responseCode = ServerCode.CONNECT_RESPONSE_FAIL;
        }
        else if (requestCode == ServerCode.UPLOAD_REQUEST){
            responseCode = fileServer.storeFile(requestData, message[2],username);
        }
        else if (requestCode == ServerCode.DOWNLOAD_REQUEST){
            responseCode = fileServer.checkFileExists(requestData,username);
        }
        else if (requestCode == ServerCode.DISCONNECT_REQUEST){
            responseCode = fileServer.disconnectUser(requestData);
        }

        return responseCode;
    }

    public String generateResponse(String username, int responseCode, String requestData) throws Exception
    {
        String responseMessage = responseCode + fileServer.argumentDivider;

        if (responseCode == ServerCode.CONNECT_RESPONSE_OK)
            responseMessage += "Welcome to FileServer! You are now able to transfer files...";
        else if (responseCode == ServerCode.CONNECT_RESPONSE_FAIL)
            responseMessage += "Unable to connect to FileServer.";
        else if (responseCode == ServerCode.UPLOAD_RESPONSE_OK)
            responseMessage += "File successfully stored.";
        else if (responseCode == ServerCode.UPLOAD_RESPONSE_FAIL)
            responseMessage += "FileServer unable to store file.";
        else if (responseCode == ServerCode.DOWNLOAD_RESPONSE_OK)
            responseMessage += requestData + fileServer.argumentDivider + fileServer.retrieveFile(requestData,username);
        else if (responseCode == ServerCode.DOWNLOAD_RESPONSE_FAIL)
            responseMessage += "Download from server failed.";
        else if (responseCode == ServerCode.DISCONNECT_RESPONSE_OK)
            responseMessage += "Disconnected from FileServer.";

        return responseMessage;
    }

    public int connectUser(final String username, InetAddress address, int port)
    {
        //if the user is already connected we will first end that session
        disconnectUser(username);

        connectedUsers.add(new ServerUser(username, address, port));
        return createUserFolder(username);
    }

    public int disconnectUser(final String username)
    {
        int i=0;
        boolean done = false;

        while(i < connectedUsers.size() && !done){
            if(connectedUsers.get(i).getUsername().equals(username)) {
                connectedUsers.remove(i);
                done = true;
                System.out.println(username + " disconnected successfully...returning 205");
            }

            i++;
        }

        return ServerCode.DISCONNECT_RESPONSE_OK;
    }

    public int createUserFolder(String username)
    {
        File folder = new File(STORAGE_DIRECTORY + username);
        int result;

        if(!folder.exists())
        {
            try {
                System.out.println("Attempting to create folder: " + folder);
                //Files.createDirectory(folder.toPath());
                Files.createDirectories(folder.toPath());
                result = ServerCode.CONNECT_RESPONSE_OK;
                System.out.println("Folder created....returning 201");
            }
            catch(Exception e)
            {
                System.out.println(e.getMessage());
                result = ServerCode.CONNECT_RESPONSE_FAIL;
            }
        }
        else
        {
            System.out.println("Folder already exists....returning 201");
            result = ServerCode.CONNECT_RESPONSE_OK;
        }

        return result;
    }

    public String getUsername(InetAddress address, int port)
    {
        String username = "";

        for(ServerUser user : connectedUsers)
        {
            if(user.getAddress().equals(address) && user.getPort() == port)
                username = user.getUsername();
        }

        return username;
    }

    public int storeFile(String fileName, String fileData, String username)
    {
        System.out.println("Storing file (" + username + "): " + fileName);

        try {
            byte[] data = fileData.trim().getBytes();

            File fileToWrite = new File(STORAGE_DIRECTORY + username + "/" + fileName);
            FileOutputStream output = new FileOutputStream(fileToWrite);
            output.write(data);
            output.flush();
            output.close();
        }catch(Exception ex){
            System.out.println("Failed...returning 302");
            return ServerCode.UPLOAD_RESPONSE_FAIL;
        }

        System.out.println("Success...returning 301");
        return ServerCode.UPLOAD_RESPONSE_OK;
    }

    public int checkFileExists(String fileName, String username){
        try {
            Path filePath = Paths.get(STORAGE_DIRECTORY + username + "/" + fileName);

            if (!Files.exists(filePath)) {
                System.out.println("File not found...returning 402");
                return ServerCode.DOWNLOAD_RESPONSE_FAIL;
            } else
                return ServerCode.DOWNLOAD_RESPONSE_OK;
        }catch(Exception e){
            System.out.println("File not found (exception thrown)...returning 402");
            return ServerCode.DOWNLOAD_RESPONSE_FAIL;
        }
    }

    public String retrieveFile(String fileName, String username) throws Exception{
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(STORAGE_DIRECTORY + username + "/" + fileName), StandardCharsets.UTF_8));
        String sData;
        StringBuilder dataString = new StringBuilder();

        while ((sData = br.readLine()) != null){
            dataString.append(sData);
        }

        System.out.println("File retrieved...");
        return dataString.toString();
    }
}