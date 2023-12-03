package cnt4007;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

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
                boolean clientInterested = false;
                boolean getFirstInterested = false;
                boolean continueLoop = true;
                boolean clientChoked = false;
                boolean serverChoked = false;
                byte[] receivedBytes = null;
                int [] threadBitfield;
                long startTime = System.currentTimeMillis();

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

                peerProcess.PeerInfo clientInfo = getSpecificPeer(peerInfoVector, Integer.parseInt(clientId));
                peerProcess.PeerInfo serverInfo = getSpecificPeer(peerInfoVector, Integer.parseInt(lastFourServerID));

                threadBitfield = Arrays.copyOf(clientInfo.getBitfield(), clientInfo.getBitfield().length);
                byte [] bitFieldMSG = msgObj.createBitfieldMessage(clientInfo.getBitfield());
                System.out.println("BitFieldMSG Binary Values from client:");
                for (byte b : bitFieldMSG) {
                    String binaryString = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
                    System.out.print(binaryString + " ");
                }
                Vector<Integer> indexVector = new Vector<>();

                output.writeObject(bitFieldMSG);
                output.flush();

                System.out.println("Loop entered on client side:");
                while(continueLoop) {
                    // Check if all the files are completed
                    for (peerProcess.PeerInfo peer : peerInfoVector) {
                        continueLoop = false;
                        if (Arrays.stream(peer.getBitfield()).anyMatch(bit -> bit == 0)) {
                            continueLoop = true;
                            break;
                        }
                    }
                    if(!continueLoop){
                        break;
                    }

                    try {
                        receivedBytes = (byte[]) input.readObject();
                    } catch (ClassNotFoundException e) {

                        System.out.println("Error: " + e.getMessage());
                        // Sets the custom "ServerNotReadyException"
                        // Client is connecting to server that has not been started yet
                        exceptionRef.set(new ServerNotReadyException("Server is not ready yet."));
                    } catch (EOFException eof){

                        System.out.println("Error: " + eof.getMessage());
                    }

                    // Send a byte array as an object



                    ByteBuffer buffer = ByteBuffer.wrap(receivedBytes);

                    // Extract the first four bytes as an integer
                    int length = buffer.getInt();
                    System.out.println("Length: " + length);

                    // Extract the next byte
                    byte messageType = buffer.get();
                    int messageTypeInt = messageType & 0xFF;

                    // Extract the remaining bytes as specified by 'length'
                    byte[] content = new byte[length];
                    buffer.get(content);
                    msgObj.receiveMessage(length, messageTypeInt, content);

                        int option = msgObj.messageType;
                        switch (option) {
                            case 0: // CHOKE
                                time = new Date();
                                System.out.println("[" + time + "] Peer " + clientId + " is choked by " + lastFourServerID);
                                clientChoked = true;
                                break;
                            case 1: // UNCHOKE
                                time = new Date();
                                System.out.println("[" + time + "] Peer " + clientId + " is unchoked by " + lastFourServerID);
                                clientChoked = false;

                                if(Arrays.stream(clientInfo.getBitfield()).anyMatch(bit -> bit == 0)) {
                                    System.out.println("I need something");
                                    int randIndex = clientInfo.getRandomIndexWith1(serverInfo.getBitfield());
                                    System.out.println("This is what I need " + randIndex);
                                    if (randIndex != -1) {
                                        output.writeObject(msgObj.createRequestMessage(randIndex));
                                        msgObj.index = randIndex;
                                    }
                                }
                                break;
                            case 2: // Interested
                                time = new Date();
                                System.out.println("[" + time + "] Peer " + clientId + " received the ‘interested’ message from " + lastFourServerID);

                                serverInterested = true;
                                clientInfo.setInterestedNeighbors(Integer.parseInt(lastFourServerID), serverInterested);

                                if(!getFirstInterested){
                                    serverInfo.setLastTimePreferredNeighborsChanged(System.currentTimeMillis());
                                    clientInfo.selectPreferredNeighbors(config.getNeighborsVector());
                                    if(clientInfo.getLastTimeOptimisticallyUnchokedChanged() == 0) {
                                        clientInfo.setOptimisticallyUnchoked(config.getNeighborsVector());
                                    }
                                    if(clientInfo.getPreferredNeighbors().containsKey(Integer.parseInt(lastFourServerID))){
                                        output.writeObject(msgObj.unchokeMessage);
                                        serverChoked = false;
                                    }else{
                                        output.writeObject(msgObj.chokeMessage);
                                        serverChoked = true;
                                    }
                                    getFirstInterested = true;
                                }

                                break;
                            case 3: // NOT INTERESTED
                                time = new Date();
                                System.out.println("[" + time + "] Peer " + clientId + " received the ‘not interested’ message from " + lastFourServerID);
                                serverInterested = false;
                                clientInfo.setInterestedNeighbors(Integer.parseInt(lastFourServerID), serverInterested);
                                break;
                            case 4: // HAVE
                                time = new Date();
                                System.out.println("[" + time + "] Peer " + clientId + " received the ‘have’ message from " + lastFourServerID);

                                serverInfo.setBitfield(msgObj.payloadInt);
                                if (isInterestedAtIndex(clientInfo.getBitfield(), serverInfo.getBitfield(), msgObj.payloadInt) && !clientInterested) {

                                    clientInterested = true;
                                    output.writeObject(msgObj.getInterestedMessage());
                                } else if(!isInterestedAtIndex(clientInfo.getBitfield(), serverInfo.getBitfield(), msgObj.payloadInt) && clientInterested){
                                    clientInterested = false;
                                    output.write(msgObj.getNotInterestedMessage());
                                }
                                break;
                            case 5: // BITFIELD
                                time = new Date();
                                System.out.println("[" + time + "] Peer " + clientId + " received the ‘bitfield’ from " + lastFourServerID);

                                if (isInterested(clientInfo.getBitfield(), serverInfo.getBitfield())) {
                                    clientInterested = true;
                                    output.writeObject(msgObj.getInterestedMessage());
                                } else {
                                    clientInterested = false;
                                    output.writeObject(msgObj.getNotInterestedMessage());
                                }

                                break;
                            case 6: // REQUEST


                                time = new Date();
                                System.out.println("[" + time + "] Peer " + clientId + " received the ‘request’ message from " + lastFourServerID);
                                if(!serverChoked) {
                                    output.writeObject(msgObj.createPieceMessage(msgObj.payloadInt, FileCreator.readFile(msgObj.payloadInt, config.getPieceSize(), config.getOutputFilePath())));
                                }

                                break;

                            case 7: // PIECE
                                time = new Date();
                                System.out.println("[" + time + "] Peer " + clientId + " received the ‘piece' from " + lastFourServerID);

                                indexVector.add(msgObj.payloadInt);
                                FileCreator.writeToFile(config.getPieceSize(), msgObj.payloadInt, msgObj.payloadBytes, config.getOutputFilePath());
                                clientInfo.setBitfield(msgObj.payloadInt);

                                serverInfo.getPreferredNeighbors().put(Integer.parseInt(clientId),serverInfo.getPreferredNeighbors().get(Integer.parseInt(lastFourServerID) + 1));

                                if(Integer.parseInt(clientId) == serverInfo.getOptimisticallyUnchoked()){
                                    serverInfo.setoUcdownloadrate(true);
                                }
                                if(isInterested(clientInfo.getBitfield(), serverInfo.getBitfield())){
                                    if(!clientInterested){
                                        output.writeObject(msgObj.interestedMessage);
                                    }
                                    int randIndex = clientInfo.getRandomIndexWith1(serverInfo.bitfield);
                                    if (randIndex != -1) {
                                        output.writeObject(msgObj.createRequestMessage(randIndex));
                                        msgObj.index = randIndex;
                                    }
                                }else{
                                    if(clientInterested){
                                        output.writeObject(msgObj.notInterestedMessage);
                                    }
                                }
                                break;
                            default:
                                System.out.println("Invalid option. Please select a number between 0 and 7.");
                                break;
                        }


                    if (!Arrays.equals(threadBitfield, clientInfo.getBitfield())) {
                        int index = newPiece(threadBitfield, clientInfo.getBitfield());
                        if (index != -1) {
                            threadBitfield[index] = 1;
                            output.writeObject(msgObj.createHaveMessage(index));
                        }
                    }

                    if(System.currentTimeMillis() - clientInfo.getLastTimePreferredNeighborsChanged() >= config.unchokingInterval * 1000L){
                        clientInfo.setLastTimePreferredNeighborsChanged(System.currentTimeMillis());
                        clientInfo.selectPreferredNeighbors(config.getNeighborsVector());

                        if(clientInfo.getPreferredNeighbors().containsKey(Integer.parseInt(lastFourServerID))){
                            if (serverChoked) {
                                output.writeObject(msgObj.unchokeMessage);
                            }
                            serverChoked = false;
                        }else if(!clientInfo.getPreferredNeighbors().containsKey(Integer.parseInt(lastFourServerID))){
                            if (!serverChoked) {
                                output.writeObject(msgObj.chokeMessage);
                            }
                            serverChoked = true;
                        }
                    }

                    if(System.currentTimeMillis() - clientInfo.getLastTimePreferredNeighborsChanged() >= config.optimisticUnchokingInterval * 1000L){
                        clientInfo.setOptimisticallyUnchoked(config.getNeighborsVector());
                    }
                    if(Integer.parseInt(lastFourServerID) == clientInfo.getOptimisticallyUnchoked()){
                        if(serverChoked){
                            output.writeObject(msgObj.unchokeMessage);
                        }
                        serverChoked = false;
                    }

                }
                int countIndexes = 0;
                for (Integer index : indexVector){
                    System.out.println("Index: " + index);
                    countIndexes++;
                }
                System.out.println("Count of indexes: " + countIndexes);

                System.out.println("All files are downloaded");
                socket.close();
                Thread.currentThread().interrupt();

            }catch (IOException e) {
                Thread.currentThread().interrupt();
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

        for (int i = 0; i < peerInfoVector.size(); i++) {
            if (peerInfoVector.get(i).getPeerID() == peerID) {
                break;
            }

            // Define how many times you want to retry
            final int MAX_RETRIES = 5;
            // Wait 5 seconds before retrying
            final int RETRY_DELAY_MS = 5000;

            String host = "localhost";

            int retryCount = 0;
            AtomicReference<Exception> exceptionRef = new AtomicReference<>(null);
            // Handle possible delays in previous peer servers starting up
            while (retryCount < MAX_RETRIES) {

                // Starts thread connection
                // Will start ClientConnection for each peer client as they connect to servers
                Thread connectionThread = new Thread(new ClientConnection(Integer.toString(peerID), peerInfoVector.get(i),peerInfoVector, config, exceptionRef));
                connectionThread.start();

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
