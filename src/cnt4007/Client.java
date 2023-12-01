package cnt4007;

import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;
import java.nio.charset.StandardCharsets;

// Client for each peer
// Will connect to the peers that have started previously
public class Client {
    private static class ServerNotReadyException extends Exception {
        public ServerNotReadyException(String message) {
            super(message);
        }
    }

    // This allows each client connection instance to run as its own thread
    // This allows a 'single client' to connect to multiple different servers
    private static class ClientConnection implements Runnable {
        private final String host;
        private final int port;
        private final String clientId;
        // Reference to store exceptions that occur during connection
        private final AtomicReference<Exception> exceptionRef;
        private final peerProcess.PeerInfo server;


        // Constructor
        public ClientConnection(String clientId, peerProcess.PeerInfo server, AtomicReference<Exception> exceptionRef) {
            this.host = server.peerHostName;
            this.port = server.peerPortNumber;
            this.clientId = clientId;
            this.server = server;
            this.exceptionRef = exceptionRef;
        }

        @Override
        public void run() {
            try {

                // Establish connectin with server
                Socket socket = new Socket(host, port);
                System.out.println(clientId + " Connected to the server" + this.server.getPeerID());

                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);
                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
                String serverMessage;

                // Handshake sent by client
                writer.println("P2PFILESHARINGPROJ0000000000" + clientId);
                serverMessage = reader.readLine();

                // Checking if this was the expected server
                // When we do the for loop, the peerId of the one we are trying to connect will be the end
                if (!serverMessage.equals("P2PFILESHARINGPROJ0000000000" + server.peerID)) {
                    System.out.println("Received: " + serverMessage + "\n Expecting: P2PFILESHARINGPROJ0000000000" + server.peerID);
                }
                String lastFourServerID = serverMessage.substring(serverMessage.length()-4);

                Date time = new Date();
               // String serverID = serverMessage.substring(28);
                System.out.println("[" + time + "] Peer " + clientId + " makes a connection to Peer " + lastFourServerID);
                System.out.println("Received: " + serverMessage);

                while ((serverMessage = userInput.readLine()) == null) {

                    // TODO: Connect to Message.java
                    writer.println(serverMessage);
                    String response = reader.readLine();
                    System.out.println("Server says: " + response);
                    if (serverMessage.equals("exit")) break;
                }

                socket.close();

            }catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
                // Sets the custom "ServerNotReadyException"
                // Client is connecting to server that has not been started yet
                exceptionRef.set(new ServerNotReadyException("Server is not ready yet."));
            }
        }
    }

    public static void readMessage(Socket clientSocket) throws IOException {
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

    public static void clientMain(int peerID, Vector<peerProcess.PeerInfo> peerInfoVector) {

        // TODO: Create a for loop to connect to all peers that were started prior
        for (int i = 0; i < peerInfoVector.size(); i++) {
            if (peerInfoVector.get(i).getPeerID() == peerID) {
                break;
            }

            // Define how many times you want to retry
            final int MAX_RETRIES = 5;
            // Wait 5 seconds before retrying
            final int RETRY_DELAY_MS = 5000;

            String host = "localhost";

            // port will need to be configurable
            // will be set by parameters most likely
            int port = 4444;
            int retryCount = 0;
            AtomicReference<Exception> exceptionRef = new AtomicReference<>(null);
            // Handle possible delays in previous peer servers starting up
            while (retryCount < MAX_RETRIES) {

                // Starts thread connection
                // Will start ClientConnection for each peer client as they connect to servers
                Thread connectionThread = new Thread(new ClientConnection(Integer.toString(peerID), peerInfoVector.get(i),exceptionRef));
                connectionThread.start();
//                try {
//                    // Waits for connection to finish
//                    // connectionThread.join();
//                } catch (InterruptedException ex) {
//                    System.out.println("Thread was interrupted: " + ex.getMessage());
//                    return;
//                }

                // Checks if the thread has an exception
                // Will retry is an exception is found after 5 secs
                // until it connects or MAX_RETRIES has been reached
                Exception caughtException = exceptionRef.get();
                if (caughtException instanceof ServerNotReadyException) {
                    System.out.println(caughtException.getMessage());
                    retryCount++;

                    if (retryCount < MAX_RETRIES) {
                        System.out.println("Retrying to connect in " + RETRY_DELAY_MS + "ms...");
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(ie);
                        }
                    } else {
                        System.out.println("Max retries reached. Exiting...");
                    }
                } else if (caughtException != null) {
                    System.out.println("Unexpected error: " + caughtException.getMessage());
                    return;
                } else {
                    break;
                }

            }
        }
    }
}
