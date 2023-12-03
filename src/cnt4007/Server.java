package cnt4007;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.Vector;


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
    public static void serverMain(int peerID, Vector<peerProcess.PeerInfo> peerInfoVector, peerProcess config) {
        try {
            int port = peerInfoVector.get(0).peerPortNumber;
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println(peerID + " server is running and listening on port " + port);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                System.out.println();
                ClientHandler clientHandler = new ClientHandler(clientSocket, peerID, peerInfoVector, config);
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
    private Vector<peerProcess.PeerInfo> peerInfoVector;
    private peerProcess config;
    public ClientHandler(Socket socket, int peerID, Vector<peerProcess.PeerInfo> peerInfoVector, peerProcess config) {
        this.clientSocket = socket;
        this.peerID = peerID;
        this.peerInfoVector = peerInfoVector;
        this.config = config;
    }

    @Override
    public void run() {
        try {
            boolean clientInterested = false;
            boolean serverInterested = false;
            boolean getFirstInterested = false;
            boolean continueLoop = true;
            boolean clientChoked = false;
            boolean serverChoked = false;
            byte[] receivedBytes = null;
            int [] threadBitfield;
            long startTime = System.currentTimeMillis();

            ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
            PrintWriter writer = new PrintWriter(output, true);
            ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            String clientMessage;

            // Get simple handshake from client
            clientMessage = reader.readLine();
            String clientID = clientMessage.substring(28);

            Message msgObj = new Message();

            // Handshake going to client. Sent by server
            writer.println("P2PFILESHARINGPROJ0000000000" + peerID);

            Date time = new Date();
            System.out.println("[" + time + "] Peer " + peerID + " is connected from Peer " + clientID);

            peerProcess.PeerInfo clientInfo = getSpecificPeer(peerInfoVector, Integer.parseInt(clientID));
            peerProcess.PeerInfo serverInfo = getSpecificPeer(peerInfoVector, peerID);

            //Send bitfield, go into while loop

            threadBitfield = Arrays.copyOf(serverInfo.getBitfield(), serverInfo.getBitfield().length);

            byte [] bitFieldMSG = msgObj.createBitfieldMessage(serverInfo.getBitfield());
            System.out.println("BitFieldMSG Binary Values from server side:");
            for (byte b : bitFieldMSG) {
                String binaryString = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
                System.out.print(binaryString + " ");
            }
            System.out.println();
            try{
                receivedBytes = (byte[]) input.readObject();
            }
            catch (ClassNotFoundException e) {
                System.out.println("Error: " + e.getMessage());
            }


            // Send a byte array as an object

            ByteBuffer buffer = ByteBuffer.wrap(receivedBytes);

            // Extract the first four bytes as an integer
            int length = buffer.getInt();
            System.out.println("Length: " +length);

            // Extract the next byte
            byte messageType = buffer.get();
            int messageTypeInt = messageType & 0xFF;

            // Extract the remaining bytes as specified by 'length'
            byte[] content = new byte[length];
            buffer.get(content);
            msgObj.receiveMessage(length, messageTypeInt, content);
            if (messageTypeInt==5) {
                System.out.println("bitfield message");
                output.writeObject(bitFieldMSG);
                if (isInterested(serverInfo.getBitfield(), clientInfo.getBitfield())) {
                    serverInterested = true;
                    output.writeObject(msgObj.getInterestedMessage());
                } else {
                    serverInterested = false;
                    output.writeObject(msgObj.getNotInterestedMessage());
                }
            }
            System.out.println("Loop entered on server side:");
            while(continueLoop){

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

                try{
                    receivedBytes = (byte[]) input.readObject();
                }
                catch (ClassNotFoundException e) {
                    clientSocket.close();
                    Thread.currentThread().interrupt();
                    System.out.println("Error: " + e.getMessage());
                }


                buffer = ByteBuffer.wrap(receivedBytes);
                // Extract the first four bytes as an integer
                length = buffer.getInt();

                // Extract the next byte
                messageType = buffer.get();
                messageTypeInt = messageType & 0xFF;

                // Extract the remaining bytes as specified by 'length'
                content = new byte[length];
                buffer.get(content);
                msgObj.receiveMessage(length, messageTypeInt, content);

                int option = msgObj.messageType;
                switch (option) {
                    case 0: // CHOKE
                        time = new Date();
                        System.out.println("[" + time + "] Server Peer " + peerID + " is choked by Client " + clientID);
                        serverChoked = true;
                        break;
                    case 1: // UNCHOKE
                        time = new Date();
                        System.out.println("[" + time + "] Server Peer " + peerID + " is unchoked by Client " + clientID);
                        serverChoked = false;

                        if(Arrays.stream(serverInfo.getBitfield()).anyMatch(bit -> bit == 0)) {
                            int randIndex = serverInfo.getRandomIndexWith1(clientInfo.getBitfield());
                            if (randIndex != -1) {
                                output.writeObject(msgObj.createRequestMessage(randIndex));
                                msgObj.index = randIndex;
                            }
                        }

                        break;
                    case 2: // Interested
                        time = new Date();
                        System.out.println("[" + time + "] Server Peer " + peerID + " received the ‘interested’ message from Client " + clientID);

                        clientInterested = true;
                        serverInfo.setInterestedNeighbors(Integer.parseInt(clientID), clientInterested);

                        if(!getFirstInterested){
                            clientInfo.setLastTimePreferredNeighborsChanged(System.currentTimeMillis());
                            serverInfo.selectPreferredNeighbors(config.getNeighborsVector());
                            if(serverInfo.getLastTimeOptimisticallyUnchokedChanged() == 0) {
                                serverInfo.setOptimisticallyUnchoked(config.getNeighborsVector());
                            }
                            if(serverInfo.getPreferredNeighbors().containsKey(Integer.parseInt(clientID))){

                                output.writeObject(msgObj.unchokeMessage);

                                clientChoked = false;
                            }else{

                                output.writeObject(msgObj.chokeMessage);

                                clientChoked = true;
                            }
                            getFirstInterested = true;
                        }
                        break;
                    case 3: // NOT INTERESTED
                        time = new Date();
                        System.out.println("[" + time + "] Server Peer " + peerID + " received the ‘not interested’ message from Client " + clientID);
                        clientInterested = false;
                        serverInfo.setInterestedNeighbors(Integer.parseInt(clientID), clientInterested);
                        break;
                    case 4: // HAVE
                        time = new Date();
                        System.out.println("[" + time + "] Server Peer " + peerID + " received the ‘have’ message from Client " + clientID);

                        clientInfo.setBitfield(msgObj.payloadInt);
                        if (isInterestedAtIndex(serverInfo.getBitfield(), clientInfo.getBitfield(), msgObj.payloadInt) && !serverInterested) {
                            serverInterested = true;
                            output.writeObject(msgObj.getInterestedMessage());
                        } else if(!isInterestedAtIndex(serverInfo.getBitfield(), clientInfo.getBitfield(), msgObj.payloadInt) && serverInterested){
                            serverInterested = false;
                            output.writeObject(msgObj.getNotInterestedMessage());
                        }
                        break;
                    case 5: // BITFIELD
                        time = new Date();
                        System.out.println("[" + time + "] Server Peer " + peerID + " received the ‘bitfield’ from Client " + clientID);
                        if (isInterested(serverInfo.getBitfield(), clientInfo.getBitfield())) {
                            serverInterested = true;
                            output.writeObject(msgObj.getInterestedMessage());
                        } else {
                            serverInterested = false;
                            output.writeObject(msgObj.getNotInterestedMessage());
                        }

                        if(serverInfo.getPreferredNeighbors().containsKey(Integer.parseInt(clientID))){
                            output.writeObject(msgObj.unchokeMessage);
                            clientChoked = false;
                        }else if(!serverInfo.getPreferredNeighbors().containsKey(Integer.parseInt(clientID))){
                            output.writeObject(msgObj.chokeMessage);
                            clientChoked = true;
                        }
                        break;
                    case 6: // REQUEST
                        time = new Date();
                        System.out.println("[" + time + "] Server Peer " + peerID + " received the ‘request’ message from Client " + clientID);
                        if(!clientChoked) {
                            output.writeObject(msgObj.createPieceMessage(msgObj.payloadInt, FileCreator.readFile(msgObj.payloadInt, config.getPieceSize(), config.getOutputFilePath())));
                        }
                        break;
                    case 7: // PIECE
                        time = new Date();
                        System.out.println("[" + time + "] Server Peer " + peerID + " received the ‘piece' from Client " + clientID);
                        FileCreator.writeToFile(config.getPieceSize(), msgObj.payloadInt, msgObj.payloadBytes, config.getOutputFilePath());
                        serverInfo.setBitfield(msgObj.payloadInt);

                        clientInfo.getPreferredNeighbors().put(peerID,clientInfo.getPreferredNeighbors().get(Integer.parseInt(clientID) + 1));

                        if(peerID == clientInfo.getOptimisticallyUnchoked()){
                            clientInfo.setoUcdownloadrate(true);
                        }

                        if(isInterested(serverInfo.getBitfield(), clientInfo.getBitfield())){
                            if(!serverInterested){
                                output.writeObject(msgObj.interestedMessage);
                            }
                            int randIndex = serverInfo.getRandomIndexWith1(clientInfo.bitfield);
                            if (randIndex != -1) {
                                output.writeObject(msgObj.createRequestMessage(randIndex));
                                msgObj.index = randIndex;
                            }
                        }else{
                            if(serverInterested){
                                output.writeObject(msgObj.notInterestedMessage);
                            }
                        }
                        break;
                    default:
                        System.out.println("Invalid option. Please select a number between 0 and 7.");
                        break;
                }

                if (!Arrays.equals(threadBitfield, serverInfo.getBitfield())) {
                    int index = newPiece(threadBitfield, serverInfo.getBitfield());
                    if (index != -1) {
                        threadBitfield[index] = 1;
                        output.writeObject(msgObj.createHaveMessage(index));
                    }

                }

                if(System.currentTimeMillis() - serverInfo.getLastTimePreferredNeighborsChanged() >= config.unchokingInterval * 1000L){
                    serverInfo.setLastTimePreferredNeighborsChanged(System.currentTimeMillis());
                    serverInfo.selectPreferredNeighbors(config.getNeighborsVector());

                    if(serverInfo.getPreferredNeighbors().containsKey(Integer.parseInt(clientID))){
                        if (clientChoked) {
                            output.writeObject(msgObj.unchokeMessage);
                        }
                        clientChoked = false;
                    }else {
                        if (!clientChoked) {
                            output.writeObject(msgObj.chokeMessage);
                        }
                        clientChoked = true;
                    }
                }
                if(System.currentTimeMillis() - serverInfo.getLastTimePreferredNeighborsChanged() >= config.optimisticUnchokingInterval * 1000L){
                    serverInfo.setOptimisticallyUnchoked(config.getNeighborsVector());
                }
                if(Integer.parseInt(clientID) == serverInfo.getOptimisticallyUnchoked()){
                    if(clientChoked){
                        output.writeObject(msgObj.unchokeMessage);
                    }
                    clientChoked = false;
                }

            }
            System.out.println("All files are downloaded");
            clientSocket.close();
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
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

    public boolean isInterested(int [] serverBitfield, int[] clientBitfield){
        for(int i = 0; i < serverBitfield.length; i++){
            if(serverBitfield[i] != clientBitfield[i] && serverBitfield[i] == 0){
                return true;
            }
        }
        return false;
    }
    public boolean isInterestedAtIndex(int [] serverBitfield, int[] clientBitfield, int index){
        return serverBitfield[index] != clientBitfield[index] && serverBitfield[index] == 0;
    }
    public int newPiece(int[] threadBitfield, int[] serverBitfield){
        for(int i = 0; i < serverBitfield.length; i++){
            if(threadBitfield[i] != serverBitfield[i] && serverBitfield[i] == 1){
                return i;
            }
        }
        return -1;
    }
}
