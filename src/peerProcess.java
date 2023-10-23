import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

public class peerProcess {
    public int numberOfPreferredNeighbors;
    public int unchokingInterval;
    public int optimisticUnchokingInterval;
    public String fileName;
    public int fileSize;
    public int pieceSize;
    Vector<PeerInfo> peerInfoVector = new Vector<>();
    public int whoAmIIDNumber;
    public static class PeerInfo {
        public int peerID;
        public String peerHostName;
        public int peerPortNumber;
        public int hasFile;

        public PeerInfo(int peerID, String peerHostName, int peerPortNumber, int hasFile) {
            this.peerID = peerID;
            this.peerHostName = peerHostName;
            this.peerPortNumber = peerPortNumber;
            this.hasFile = hasFile;
        }

    }
    public void printConfigInfo(){
        System.out.println("WHOAMI: "+ whoAmIIDNumber);
        System.out.println("Number Of Preferred Neighbors: " + numberOfPreferredNeighbors);
        System.out.println("Unchoking Interval: " + unchokingInterval);
        System.out.println("Optimistic Unchoking Interval: " + optimisticUnchokingInterval);
        System.out.println("File Name: " + fileName);
        System.out.println("File Size: " + fileSize);
        System.out.println("Piece Size: " + pieceSize);
        System.out.println("----------------------------");
    }
    public void printPeerInfo() {
        for (PeerInfo peer : peerInfoVector) {
            System.out.println("Peer ID: " + peer.peerID);
            System.out.println("Host Name: " + peer.peerHostName);
            System.out.println("Port Number: " + peer.peerPortNumber);
            System.out.println("Has File: " + peer.hasFile);
            System.out.println("----------------------------");
        }
    }

    // Constructor to initialize from the file
    public peerProcess(String configFilePath, String peerInfoFilePath) throws IOException {

        BufferedReader reader = null;
        try {
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
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        try {
            reader = new BufferedReader(new FileReader(peerInfoFilePath));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                int peerID = Integer.parseInt(parts[0]);
                String peerHostName = parts[1];
                int peerPortNumber = Integer.parseInt(parts[2]);
                int hasFile = Integer.parseInt(parts[3]);

                PeerInfo peerInfo = new PeerInfo(peerID, peerHostName, peerPortNumber, hasFile);
                peerInfoVector.add(peerInfo);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }


    public static void main(String[] args) {
        try {
            //peerProcess config = new peerProcess("project_config_file_small\\Common.cfg", "project_config_file_small\\PeerInfo.cfg");
            peerProcess config = new peerProcess("project_config_file_small/Common.cfg", "project_config_file_small/PeerInfo.cfg");//lucy mac
            if(args.length > 0) {
                config.whoAmIIDNumber = Integer.parseInt(args[0]);
            } else {
                System.out.println("Please provide a valid ID number as an argument.");
                config.whoAmIIDNumber = 1001;
            }
            config.printConfigInfo();
            config.printPeerInfo();

            // Start the server and the client for each peer
            config.makeConnections(config);
        } catch (IOException e) {
            System.out.println("Error reading the configuration file: " + e.getMessage());
        }
    }
}
