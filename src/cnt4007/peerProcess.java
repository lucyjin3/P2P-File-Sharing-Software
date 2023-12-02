package cnt4007;

import java.io.*;
import java.util.Arrays;
import java.util.Vector;
import java.util.Random;

// Will be called by startRemotePeers for each peer
public class peerProcess {
    public int numberOfPreferredNeighbors;
    public int unchokingInterval;
    public int optimisticUnchokingInterval;
    public String fileName;
    public String outputFilePath;
    public int fileSize;
    public int pieceSize;
    Vector<PeerInfo> peerInfoVector = new Vector<>();
    public int whoAmIIDNumber;
    public int numPieces;
    public static int numberOfPieces;
    public boolean iHaveFile;

    public int getWhoAmIIDNumber() {
        return whoAmIIDNumber;
    }
    Vector<PeerInfo> getPeerInfoVector(){
        return peerInfoVector;
    }


    // Each peer will know who it is
    public static class PeerInfo {
        public int peerID;
        public String peerHostName;
        public int peerPortNumber;
        public int hasFile;
        public int[] bitfield;

        public PeerInfo(int peerID, String peerHostName, int peerPortNumber, int hasFile) {
            this.peerID = peerID;
            this.peerHostName = peerHostName;
            this.peerPortNumber = peerPortNumber;
            this.hasFile = hasFile;
            this.bitfield = new int[numberOfPieces];
            if (hasFile == 1) {
                Arrays.fill(bitfield, 1);
            }

        }
        public synchronized int getPeerID(){
            return this.peerID;
        }
        public synchronized void setBitfield(int index) {
            this.bitfield[index] = 1;
        }
        public synchronized int readBitfield(int index) {
            return this.bitfield[index];
        }


        public synchronized int[] getBitfield(){
            return this.bitfield;
        }
        public int getRandomIndexWith1(int [] otherBitfield){
            for (int i = 0; i < bitfield.length; i++){
                if (bitfield[i]==1){
                    break;
                }
                if (i == bitfield.length -1){
                    return -1;
                }
            }
            Random random = new Random();

            if(Arrays.equals(bitfield, otherBitfield)){
                return -1;
            }


            while (true) {
                int randomIndex = random.nextInt(bitfield.length);
                if (bitfield[randomIndex] == 1 && otherBitfield[randomIndex]==0) {
                    return randomIndex;
                }
            }
        }

    }

    public synchronized int getPieceSize(){
        return pieceSize;
    }
    public synchronized String getOutputFilePath(){
        return outputFilePath;
    }

    public synchronized PeerInfo getCertainPeer(int peerID){
        for (PeerInfo peer : peerInfoVector){
            if (peer.peerID == peerID){
                return peer;
            }
        }
        return null;
    }
    // printConfigInfo() outputs the information in the configuration file
    // Used for testing purposes
    public void printConfigInfo() {
        System.out.println("WHOAMI: " + whoAmIIDNumber);
        System.out.println("Number Of Preferred Neighbors: " + numberOfPreferredNeighbors);
        System.out.println("Unchoking Interval: " + unchokingInterval);
        System.out.println("Optimistic Unchoking Interval: " + optimisticUnchokingInterval);
        System.out.println("File Name: " + fileName);
        System.out.println("File Size: " + fileSize);
        System.out.println("Piece Size: " + pieceSize);
        System.out.println("Number of pieces: " + numberOfPieces);
        System.out.println("----------------------------");
    }

    // Used for testing to print out peerInfo
    public void printPeerInfo() {
        for (PeerInfo peer : peerInfoVector) {
            System.out.println("Peer ID: " + peer.peerID);
            System.out.println("Host Name: " + peer.peerHostName);
            System.out.println("Port Number: " + peer.peerPortNumber);
            System.out.println("Has File: " + peer.hasFile);
            System.out.println("----------------------------");
        }
    }

    // Constructor to initialize from the config files
    public peerProcess(String configFilePath, String peerInfoFilePath) throws IOException {

        BufferedReader reader = null;
        try {

            // This will read the info from the Common.cfg file
            reader = new BufferedReader(new FileReader(configFilePath));
            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts[0].equals("NumberOfPreferredNeighbors")) {
                    numberOfPreferredNeighbors = Integer.parseInt(parts[1]);
                } else if (parts[0].equals("UnchokingInterval")) {
                    unchokingInterval = Integer.parseInt(parts[1]);
                } else if (parts[0].equals("OptimisticUnchokingInterval")) {
                    optimisticUnchokingInterval = Integer.parseInt(parts[1]);
                } else if (parts[0].equals("FileName")) {
                    fileName = parts[1];
                    outputFilePath = fileName;
                } else if (parts[0].equals("FileSize")) {
                    fileSize = Integer.parseInt(parts[1]);
                } else if (parts[0].equals("PieceSize")) {
                    pieceSize = Integer.parseInt(parts[1]);
                }
            }

            numberOfPieces = (fileSize + pieceSize - 1) / pieceSize;
            numPieces = (fileSize + pieceSize - 1) / pieceSize;
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        try {
            // This will read the peerInfo from the PeerInfo.cfg for each peer
            reader = new BufferedReader(new FileReader(peerInfoFilePath));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                int peerID = Integer.parseInt(parts[0]);
                String peerHostName = parts[1];
                int peerPortNumber = Integer.parseInt(parts[2]);
                int hasFile = Integer.parseInt(parts[3]);
                if (peerID == whoAmIIDNumber) {
                    if (hasFile == 1) {
                        iHaveFile = true;
                    } else {
                        iHaveFile = false;
                    }
                }

                PeerInfo peerInfo = new PeerInfo(peerID, peerHostName, peerPortNumber, hasFile);
                peerInfoVector.add(peerInfo);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

    }

    // This will start the server and client for each peerst
    public void makeConnections(peerProcess config) {

        // Starts thread for the server and the client
        Thread serverThread = new Thread(() -> {
            startServer(config.whoAmIIDNumber, config.getPeerInfoVector(), config);
        });
        serverThread.start();

        // Give the server some time to initialize, if necessary
        try {
            // 1 second delay. Adjust if necessary.
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Start client in the main thread or in another separate thread if desired
        Thread clientThread = new Thread(() -> {
            startClient(config.whoAmIIDNumber, config.peerInfoVector, config);
        });
        clientThread.start();
    }

    public void checkIfFileWrittenCorrectly() {
        // File name for the initially read file
        String file1 = "project_config_file_small/1001/thefile";

        // File name for the outputted file
        String file2 = Integer.toString(whoAmIIDNumber) + "/" + fileName;

        try (
                // Tries opening the initially read file and outputted file
                FileInputStream fis1 = new FileInputStream(file1);
                FileInputStream fis2 = new FileInputStream(file2)
        ) {
            // Checks the number of available bytes
            // If both files don't have same number of bytes cannot be the same
            if (fis1.available() != fis2.available()) {
                System.out.println("Files have different sizes and are not identical.");
                return;
            }

            // Initializes the bytes to check each file
            int byteData1;
            int byteData2;
            // While the byte does not equal the end of file
            while ((byteData1 = fis1.read()) != -1) {
                // Reads in the second file byte
                byteData2 = fis2.read();
                // Checks if the bytes are equal
                if (byteData1 != byteData2) {
                    // If the file bytes are ever not the same they are not equal
                    // This means the code does not write the file correctly
                    System.out.println("Files are not identical.");
                    return;
                }
            }

            System.out.println("Files are identical!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Start the Server for the peer
    public static void startServer(int peerId, Vector<peerProcess.PeerInfo> peerInfoVector, peerProcess config) {

        Server.serverMain(peerId, peerInfoVector, config);

    }

    // Start the Client for the peer
    public static void startClient(int peerId,Vector<PeerInfo> peerInfoVector, peerProcess config) {

        Client.clientMain(peerId, peerInfoVector, config);
    }

    // Used to create the file for peers that do not have the file
    public void createFile(peerProcess config) {
        if (!config.iHaveFile) {

            // Create a new directory for each peer that does not have the file
            new File(Integer.toString(whoAmIIDNumber)).mkdirs();
            String[] createFileArray = new String[2];
            createFileArray[0] = whoAmIIDNumber + "/" + fileName;
            createFileArray[1] = Integer.toString(fileSize);
            outputFilePath = (createFileArray[0]);
            FileCreator.main(createFileArray);
        }
    }

    public static void main(String[] args) {

        try {
           peerProcess config = new peerProcess(args[1] + "/Common.cfg", args[1] + "/PeerInfo.cfg");//lucy mac


            config.whoAmIIDNumber = Integer.parseInt(args[0]);
            System.out.println(config.peerInfoVector.get(0).peerPortNumber);

            config.createFile(config);
            //config.checkIfFileWrittenCorrectly();
            config.makeConnections(config);

        } catch (IOException e) {
            System.out.println("Error reading the configuration file: " + e.getMessage());
        }
    }
}
