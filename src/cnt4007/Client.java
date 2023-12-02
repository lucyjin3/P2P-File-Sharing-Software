package cnt4007;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
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
        private final peerProcess config;
        private final peerProcess.PeerInfo server;
        private Vector<peerProcess.PeerInfo> peerInfoVector;


        // Constructor
        public ClientConnection(String clientId, peerProcess.PeerInfo server, Vector<peerProcess.PeerInfo> peerInfoVector, peerProcess config, AtomicReference<Exception> exceptionRef) {
            this.config = config;
            this.host = server.peerHostName;
            this.port = server.peerPortNumber;
            this.clientId = clientId;
            this.server = server;
            this.exceptionRef = exceptionRef;
            this.peerInfoVector = peerInfoVector;
        }

        @Override
        public void run() {
            try {
                boolean serverInterested = false;
                boolean continueLoop = true;
                byte[] receivedBytes = null;
                int [] threadBitfield;
                // Establish connectin with server
                Socket socket = new Socket(host, port);
                System.out.println(clientId + " Connected to the server" + this.server.getPeerID());

                ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                PrintWriter writer = new PrintWriter(output, true);
                ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));


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
                System.out.println("[" + time + "] Peer " + clientId + " makes a connection to Peer " + lastFourServerID);

                //Setting up message object
                Message msgObj = new Message();
                //msgObj.receiveMessage(input);
                //String msgType = msgObj.messageType;

                peerProcess.PeerInfo clientInfo = getSpecificPeer(peerInfoVector, Integer.parseInt(clientId));
                peerProcess.PeerInfo serverInfo = getSpecificPeer(peerInfoVector, Integer.parseInt(lastFourServerID));

                threadBitfield = clientInfo.getBitfield();
                byte [] bitFieldMSG = msgObj.createBitfieldMessage(clientInfo.getBitfield());
                System.out.println("BitFieldMSG Binary Values from client:");
                for (byte b : bitFieldMSG) {
                    String binaryString = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
                    System.out.print(binaryString + " ");
                }

                output.writeObject(bitFieldMSG);
                output.flush();


                while(continueLoop) {
                    System.out.println("Loop entered on client side:");
                    try {
                        receivedBytes = (byte[]) input.readObject();
                    } catch (ClassNotFoundException e) {
                        System.out.println("Error: " + e.getMessage());
                        // Sets the custom "ServerNotReadyException"
                        // Client is connecting to server that has not been started yet
                        exceptionRef.set(new ServerNotReadyException("Server is not ready yet."));
                    }


                    // Send a byte array as an object
                    if (receivedBytes.length != 0) {

                    }
                    ByteBuffer buffer = ByteBuffer.wrap(receivedBytes);

                    // Extract the first four bytes as an integer
                    int length = buffer.getInt();
                    System.out.println("Length: " + length);

                    // Extract the next byte
                    byte messageType = buffer.get();
                    int messageTypeInt = messageType & 0xFF;
                    System.out.println("Message Type: " + messageTypeInt);

                    // Extract the remaining bytes as specified by 'length'
                    byte[] content = new byte[length];
                    buffer.get(content);
                    if (messageTypeInt == 5) {
                        int[] bitfieldTEST = msgObj.unpackBitfield(content, content.length * 8);


                        int option = msgObj.messageType;
                        switch (option) {
                            case 0: // CHOKE
                                time = new Date();
                                System.out.println("[" + time + "] Peer " + clientId + " is choked by " + lastFourServerID);
                                break;
                            case 1: // UNCHOKE
                                time = new Date();
                                System.out.println("[" + time + "] Peer " + clientId + " is unchoked by " + lastFourServerID);
                                break;
                            case 2: // Interested
                                time = new Date();
                                System.out.println("[" + time + "] Peer " + clientId + " received the ‘interested’ message from " + lastFourServerID);

                                //Used for testing purposes. Will be moved to unchocked
                                int randIndex = clientInfo.getRandomIndexWith1(serverInfo.bitfield);
                                if (randIndex != -1) {
                                    output.writeObject(msgObj.createHaveMessage(randIndex));
                                }
                                serverInterested = true;
                                break;
                            case 3: // NOT INTERESTED
                                time = new Date();
                                System.out.println("[" + time + "] Peer " + clientId + " received the ‘not interested’ message from " + lastFourServerID);
                                serverInterested = false;
                                break;
                            case 4: // HAVE
                                time = new Date();
                                System.out.println("[" + time + "] Peer " + clientId + " received the ‘have’ message from " + lastFourServerID);

                                // Only used for testing purposes. Will be moved to unchoked
                                if (clientInfo.bitfield[Integer.parseInt(msgObj.payload)] == 0) {
                                    output.write(msgObj.createRequestMessage(Integer.parseInt(msgObj.payload)));
                                    msgObj.index = Integer.parseInt(msgObj.payload);
                                } else if (isInterestedAtIndex(clientInfo.getBitfield(), serverInfo.getBitfield(), Integer.parseInt(msgObj.payload))) {
                                    //Needed to be updated later during choke and unchoke
                                    //Don't always send interested message
                                    output.write(msgObj.getInterestedMessage());
                                } else {
                                    output.write(msgObj.getNotInterestedMessage());
                                }
                                break;
                            case 5: // BITFIELD
                                time = new Date();
                                System.out.println("[" + time + "] Peer " + clientId + " received the ‘bitfield’ from " + lastFourServerID);
                                threadBitfield = msgObj.bitFieldFromMsg;
                                if (isInterested(clientInfo.getBitfield(), serverInfo.getBitfield())) {
                                    output.write(msgObj.getInterestedMessage());
                                } else {
                                    output.write(msgObj.getNotInterestedMessage());
                                }

                                break;
                            case 6: // REQUEST
                                time = new Date();
                                System.out.println("[" + time + "] Peer " + clientId + " received the ‘request’ message from " + lastFourServerID);
                                output.write(msgObj.createPieceMessage(Integer.parseInt(msgObj.payload), FileCreator.readFile(config.numPieces, Integer.parseInt(msgObj.payload), config.pieceSize, config.outputFilePath)));
                                serverInfo.bitfield[Integer.parseInt(msgObj.payload)] = 1;
                                break;
                            case 7: // PIECE
                                time = new Date();
                                System.out.println("[" + time + "] Peer " + clientId + " received the ‘piece' from " + lastFourServerID);
                                FileCreator.writeToFile(config.numPieces, msgObj.index, msgObj.payloadBytes, config.outputFilePath);
                                clientInfo.setBitfield(msgObj.index);
                                threadBitfield[msgObj.index] = 1;
                                break;
                            default:
                                System.out.println("Invalid option. Please select a number between 0 and 7.");
                                break;
                        }

                        // Used for testing to keep moving
                        for (int i = 0; i < serverInfo.bitfield.length; i++) {
                            if (serverInfo.bitfield[i] == 1 && clientInfo.bitfield[i] == 0) {
                                output.write(msgObj.interestedMessage);
                            }
                        }

                        if (!Arrays.equals(threadBitfield, clientInfo.bitfield)) {
                            int index = newPiece(threadBitfield, clientInfo.getBitfield());
                            if (index != -1) {
                                threadBitfield[index] = 1;
                            }
                            output.write(msgObj.createHaveMessage(index));
                        }
                        // Check if all the files are completed
                        for (peerProcess.PeerInfo peer : peerInfoVector) {
                            continueLoop = false;
                            if (Arrays.stream(peer.getBitfield()).anyMatch(bit -> bit == 0)) {
                                continueLoop = true;
                                break;
                            }
                        }
                    }else{
                        System.out.println("Nothing received from server");
                    }
                }


                socket.close();

            }catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
                // Sets the custom "ServerNotReadyException"
                // Client is connecting to server that has not been started yet
                exceptionRef.set(new ServerNotReadyException("Server is not ready yet."));
            }
        }
        public peerProcess.PeerInfo getSpecificPeer(Vector<peerProcess.PeerInfo> peerInfoVector, int peerID){
            for (peerProcess.PeerInfo peer : peerInfoVector){
                if (peer.peerID == peerID){
                    return peer;
                }
            }
            return null;
        };

        public boolean isInterested(int [] clientBitfield, int[] serverBitfield){
            for(int i = 0; i < clientBitfield.length; i++){
                if(clientBitfield[i] != serverBitfield[i] && clientBitfield[i] == 0){
                    return true;
                }
            }
            return false;
        }
        public boolean isInterestedAtIndex(int [] clientBitfield, int[] serverBitfield, int index){
            return clientBitfield[index] != serverBitfield[index] && clientBitfield[index] == 0;
        }
        public int newPiece(int[] threadBitfield, int[] clientBitfield){
            for(int i = 0; i < clientBitfield.length; i++){
                if(threadBitfield[i] != clientBitfield[i] && clientBitfield[i] == 1){
                    return i;
                }
            }
            return -1;
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

    public static void clientMain(int peerID, Vector<peerProcess.PeerInfo> peerInfoVector, peerProcess config) {

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
            int retryCount = 0;
            AtomicReference<Exception> exceptionRef = new AtomicReference<>(null);
            // Handle possible delays in previous peer servers starting up
            while (retryCount < MAX_RETRIES) {

                // Starts thread connection
                // Will start ClientConnection for each peer client as they connect to servers
                Thread connectionThread = new Thread(new ClientConnection(Integer.toString(peerID), peerInfoVector.get(i),peerInfoVector, config, exceptionRef));
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
