package cnt4007;

import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.Vector;
import java.nio.charset.StandardCharsets;

import static java.lang.Integer.parseInt;

// Server for each peer
// Peers' clients that start after will connect to the server
public class Server {
    public void readMessage(Socket clientSocket) throws IOException {
        InputStream input = clientSocket.getInputStream();

        // Read the first 5 bytes (4 for length, 1 for type)
        byte[] lengthBytes = input.readNBytes(4);
        byte[] typeByte = input.readNBytes(1);

        // Convert length bytes to String and then parse integer
        String lengthStr = new String(lengthBytes, StandardCharsets.US_ASCII);
        int length = Integer.parseInt(lengthStr);

        // Convert type byte to char
        char type = new String(typeByte, StandardCharsets.US_ASCII).charAt(0);

        // Read the payload
        byte[] payloadBytes = input.readNBytes(length);
        String payload = new String(payloadBytes, StandardCharsets.US_ASCII);

        System.out.println("Length: " + length);
        System.out.println("Type: " + type);
        System.out.println("Payload: " + payload);
    }
    public static void serverMain(int peerID) {
        try {
            int port = 6001;
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println(peerID + " server is running and listening on port " + port);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                System.out.println();
                ClientHandler clientHandler = new ClientHandler(clientSocket, peerID);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final int peerID;
    public ClientHandler(Socket socket, int peerID) {
        this.clientSocket = socket;
        this.peerID = peerID;
    }

    @Override
    public void run() {
        try {
            InputStream input = clientSocket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            OutputStream output = clientSocket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            String clientMessage;

            // Send simple handshake
            clientMessage = reader.readLine();

            // Checking if this was the expected server
            // If it is not the correct server, connection will terminate
//            if (!clientMessage.equals("P2PFILESHARINGPROJ0000000000" + )){
//                System.out.println("Received: " + clientMessage + "\n Expecting: P2PFILESHARINGPROJ0000000000" + peerID);
//                clientSocket.close();
//            }

            String clientID = clientMessage.substring(28);
            System.out.println("Received: " + clientMessage);

            // Handshake sent by client
            writer.println("P2PFILESHARINGPROJ0000000000" + peerID);

            // [Time]: Peer [peer_ID 1] is connected from Peer [peer_ID 2].
            Date time = new Date();
            System.out.println("[" + time + "] Peer " + peerID + " is connected from Peer " + clientID);

            // TODO: Get a clientMessage
            // Currently clientMessage= NULL , and Handshake working!
            while ((clientMessage = reader.readLine()) != null) {
                System.out.println("Received: " + clientMessage);
                if (clientMessage.equals("exit")) break;
                writer.println("Server Received: " + clientMessage);
            }

            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
