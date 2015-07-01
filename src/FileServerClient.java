import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileServerClient {
    final String endMessage = ".";
    final String argumentDivider = "~~~";
    final String DEFAULT_SAVE_DIRECTORY = "C:/fsdownloads/";
    String connectedUsername;
    boolean connected = false;

    public static void main(String[] args) {
        FileServerClient fsClient = new FileServerClient();
        InputStreamReader is = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(is);

        try {
            System.out.println("FileServer Client is starting up...\nWhat is the name of the server host?");

            String hostName = br.readLine();
            if (hostName.length() == 0) // if user did not enter a name
                hostName = "localhost";  //   use the default host name

            System.out.println("What is the port number of the server host?");
            String portNum = br.readLine();
            if (portNum.length() == 0)
                portNum = "30000";          // default port number

            FileServerClientHelper helper =  new FileServerClientHelper(hostName, portNum);
            boolean done = false;
            String message, response;

            System.out.println("\nWelcome to the FileServer client. \nType 'help' to view a list of commands. \n\n" +
                    "Enter 'login' followed by your username to connect to the server.\n");

            while (!done) {
                if (fsClient.connected)
                    System.out.print("FileServer:" + fsClient.connectedUsername + ">");
                else
                    System.out.print("FileServer>");

                message = br.readLine();
                
                if ((message.trim()).equals(fsClient.endMessage)){
                    done = true;
                    helper.done();
                }
                else if (message.equals(("help"))){
                    fsClient.showHelpContent();
                }
                else if (message.trim().length() != 0){
                    try {
                        response = helper.getResponse(fsClient.prepareRequest(message));
                        fsClient.parseResponse(response);
                    }catch(Exception e){
                        System.out.println(e.getMessage());
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String prepareRequest(String message) throws Exception{
        String[] arguments = message.split((" "));
        String command = arguments[0].toLowerCase();
        String requestArgument = "";

        if(arguments.length > 1)
            requestArgument = arguments[1].trim();
        else if (!command.equals("disconnect")) //the user entered no arguments - this is invalid
            throw new Exception("Invalid input detected. Type 'help' for a list of commands.");

        String request;

        if (command.equals("login")){
            if (!connected) {
                connectedUsername = requestArgument; //TEMP SOLUTION
                request = ServerCode.CONNECT_REQUEST + argumentDivider + arguments[1];
            }
            else{
                throw new Exception("You are already connected. Disconnect before opening a new session");
            }
        }
        else if (command.equals("upload")){
            String filePath = requestArgument;
            String fileName = new File(filePath).getName();
            request = ServerCode.UPLOAD_REQUEST + argumentDivider + fileName + argumentDivider + readFileData(filePath);
        }
        else if (command.equals("download")){
            String fileName = requestArgument;
            request = ServerCode.DOWNLOAD_REQUEST + argumentDivider + fileName;
        }
        else if (command.equals("disconnect")){
            if(connected)
                request = ServerCode.DISCONNECT_REQUEST + argumentDivider + connectedUsername;
            else
                throw new Exception("There is no connection open to FileServer!");
        }
        else{
            throw new Exception("Command not recognised. Type 'help' for a list of commands.");
        }

        //System.out.println("Request: " + request);
        return request;
    }

    public void parseResponse(String response){
        String[] arguments = response.split((argumentDivider));
        String responseCode = arguments[0];

        if(responseCode.equals(Integer.toString(ServerCode.CONNECT_RESPONSE_OK))) {
            connected = true;
            System.out.println(arguments[1]);
        }
        else if(responseCode.equals(Integer.toString(ServerCode.DOWNLOAD_RESPONSE_OK))){
            saveFileData(arguments[1], arguments[2]);
        }
        else if(responseCode.equals(Integer.toString(ServerCode.DISCONNECT_RESPONSE_OK))){
            connected = false;
            connectedUsername = null;
            System.out.println(arguments[1]);
        }
        else{
            System.out.println(arguments[1]);
        }
    }

    public String readFileData(String filePath) throws Exception{
        if (!Files.exists(Paths.get(filePath)))
            throw new Exception("File not found, please check the path.");

        BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(filePath), StandardCharsets.UTF_8));

        String sData;
        StringBuilder dataString = new StringBuilder();

        while ((sData = br.readLine()) != null){
            dataString.append(sData);
        }

        return dataString.toString();
    }

    public void saveFileData(String fileName, String fileData){
        try {
            byte[] data = fileData.trim().getBytes();

            File saveDirectory = new File(DEFAULT_SAVE_DIRECTORY + connectedUsername); //SORT USERNAME HERE

            if(!saveDirectory.exists()) {
                //System.out.println("Attempting save directory creation...");
                Files.createDirectories(saveDirectory.toPath());
            }

            File fileToWrite = new File(saveDirectory + "/" + fileName);
            FileOutputStream output = new FileOutputStream(fileToWrite);
            output.write(data);
            output.flush();
            output.close();
            System.out.println("File successfully saved to disk at: " + fileToWrite.getPath());
        }catch(Exception ex){
            ex.printStackTrace();
            System.out.println("Exception: " + ex.getMessage() + "\nFailed to save file to disk.");
        }
    }

    public void showHelpContent()
    {
        System.out.println("\n-----FileServer Help-----\nThe following commands are available:\n" +
            "\tlogin <username>\n" +
            "\tupload <filepath.extension>\n" +
            "\tdownload <filename.extension>\n" +
            "\tdisconnect\n"
        );
    }
}