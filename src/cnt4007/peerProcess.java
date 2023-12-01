package cnt4007;

import java.io.*;
import java.util.Arrays;
import java.util.Vector;

// Will be called by startRemotePeers for each peer
public class peerProcess {
    public int numberOfPreferredNeighbors;
    public int unchokingInterval;
    public int optimisticUnchokingInterval;
    public String fileName;
    public int fileSize;
    public int pieceSize;
    Vector<PeerInfo> peerInfoVector = new Vector<>();
    public int whoAmIIDNumber;
    public static int numberOfPieces;
    public boolean iHaveFile;


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
            if( hasFile == 1){
                Arrays.fill(bitfield,1);
            }

        }
        public int getPeerID(){
            return this.peerID;
        }

    }



    // printConfigInfo() outputs the information in the configuration file
    // Used for testing purposes
    public void printConfigInfo(){
        System.out.println("WHOAMI: "+ whoAmIIDNumber);
        System.out.println("Number Of Preferred Neighbors: " + numberOfPreferredNeighbors);
        System.out.println("Unchoking Interval: " + unchokingInterval);
        System.out.println("Optimistic Unchoking Interval: " + optimisticUnchokingInterval);
        System.out.println("File Name: " + fileName);
        System.out.println("File Size: " + fileSize);
        System.out.println("Piece Size: " + pieceSize);
        System.out.println("Number of pieces: "+ numberOfPieces);
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
                } else if (parts[0].equals("FileSize")) {
                    fileSize = Integer.parseInt(parts[1]);
                } else if (parts[0].equals("PieceSize")) {
                    pieceSize = Integer.parseInt(parts[1]);
                }
            }

            numberOfPieces = (fileSize+pieceSize-1)/pieceSize;
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
                if (peerID == whoAmIIDNumber){
                    if (hasFile == 1){
                        iHaveFile = true;
                    }
                    else {
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

    // This will start the server and client for each peer
    public void makeConnections(peerProcess config) {

        // Start the server and the client
        Thread serverThread = new Thread(() -> {
            startServer(config.whoAmIIDNumber);
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
            startClient(config.whoAmIIDNumber, config.peerInfoVector);
        });
        clientThread.start();
    }

    // writeToFile will allow us to write to fileName at a specific index
    public static void writeToFile(String fileName, int pieceSize, int pieceNumber, byte[] data) throws Exception {

        // Calculate the position in the file where you want to start writing
        long position = (long) pieceNumber * pieceSize;

        // By using synchronized key word it allows for singular access to the file
        // This prevents the possibility of different threads writing to the same file
        // at the same time
        synchronized (fileName.intern()) {
            try (RandomAccessFile raf = new RandomAccessFile(fileName, "rw")) {
                // This finds the starting point for a piece
                raf.seek(position);
                // After finding the starting point it writes the data for that piece
                // to the file
                raf.write(data);
            }
        }
    }

    public void checkIfFileWrittenCorrectly(){
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
    public void startServer() {

        // For testing purposes, working with peer 1001
        //String[] args = {Integer.toString(peerId)};
        Server.serverMain(peerId);

    }

    // Start the Client for the peer
    public void startClient(){

        // For testing purposes, working with peer 1002
        //String[] args = {Integer.toString(peerId)};
        Client.clientMain(peerId, peerInfoVector);
    }

    // Used to create the file for peers that do not have the file
    public void createFile(){
        if (!iHaveFile) {

            // Create a new directory for each peer that does not have the file
            new File(Integer.toString(whoAmIIDNumber)).mkdirs();
            String[] createFileArray = new String[2];
            createFileArray[0] = whoAmIIDNumber + "/" + fileName ;
            createFileArray[1] = Integer.toString(fileSize);
            FileCreator.main(createFileArray);
        }
    }

    public static void main(String[] args) {

        try {
            //cnt4007.peerProcess config = new cnt4007.peerProcess("project_config_file_small\\Common.cfg", "project_config_file_small\\PeerInfo.cfg");
            peerProcess config = new peerProcess(args[1] + "/Common.cfg", args[1] + "/PeerInfo.cfg");//lucy mac


            config.whoAmIIDNumber = Integer.parseInt(args[0]);


            config.createFile();
            config.checkIfFileWrittenCorrectly();
            //config.printConfigInfo();
            //config.printPeerInfo();

            // Start the server and the client for each peer
            config.makeConnections(config);

        } catch (IOException e) {
            System.out.println("Error reading the configuration file: " + e.getMessage());
        }
    }
}
