package cnt4007;

import java.io.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;



public class StartRemotePeers {

    //p2pFileSharing/cnt4007.
    private static final String scriptPrefix = "java cnt4007/peerProcess ";


    public static class PeerInfo {

        private String peerID;
        private String hostName;

        public PeerInfo(String peerID, String hostName) {
            super();
            this.peerID = peerID;
            this.hostName = hostName;
        }

        public String getPeerID() {
            return peerID;
        }

        public void setPeerID(String peerID) {
            this.peerID = peerID;
        }

        public String getHostName() {
            return hostName;
        }

        public void setHostName(String hostName) {
            this.hostName = hostName;
        }

    }

    public static void main(String[] args) {

        File file;
        String dir;

        ArrayList<PeerInfo> peerList = new ArrayList<>();
// Alter your code to have ur uf user name
        String ciseUser = "carson.schmidt"; // carson
        //String ciseUser = "jinh"; // lucy
        //String ciseUser = " "; // sydney

/**
 * Make sure the below peer hostnames and peerIDs match those in
 * PeerInfo.cfg in the remote CISE machines. Also make sure that the
 * peers which have the file initially have it under the 'peer_[peerID]'
 * folder.
 */
      /*  Console console = System.console();
        if (console == null) {
            System.out.println("No console available");
            return;
        }
        String username = console.readLine("Enter your username: ");
        char[] password = console.readPassword("Enter your password: ");
        */
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter your username: ");
        String username = scanner.nextLine();

        System.out.print("Enter your password: ");
        String password = scanner.nextLine(); // Note: This will not hide the password input

        System.out.println("Pick a file: \n[1] project_config_file_large \n[2] project_config_file_small");
        String userInput = scanner.nextLine();

        if(userInput.equals("1")){
            dir = "project_config_file_large";
            file = new File("project_config_file_large/PeerInfo.cfg");
        }else if (userInput.equals("2")){
            dir = "project_config_file_small";
            file = new File("project_config_file_small/PeerInfo.cfg");
        }else{
            System.out.println("Invalid option");
            return;
        }

        try {
            Scanner sc = new Scanner(file);
            //int id = 1;
            while (sc.hasNextLine()){
                String peerInfo = sc.nextLine();
                String[] line = peerInfo.split(" ");
                peerList.add(new PeerInfo(line[0], line[1]));
            }
        }catch(java.io.FileNotFoundException e){
            System.out.println("Error");
            return;
        }

        for (PeerInfo remotePeer : peerList) {
            try {
                JSch jsch = new JSch();
                /*
                 * Give the path to your private key. Make sure your public key
                 * is already within your remote CISE machine to ssh into it
                 * without a password. Or you can use the corressponding method
                 * of JSch which accepts a password.
                 */
                // Make sure this code is also your local file path to .ssh
                //  jsch.addIdentity("C:\\Users\\csesc_snhoakq\\.ssh\\id_rsa", ""); //carson
                //jsch.addIdentity("/Users/lucyjin3/.ssh/id_rsa", ""); //lucy
                //jsch.addIdentity("C:\\Users\\sydmc\\.ssh\\id_rsa", ""); //sydney
                Session session = jsch.getSession(username, remotePeer.getHostName(), 22);
                session.setPassword(new String(password));
                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);

                session.connect();

                System.out.println("Session to peer# " + remotePeer.getPeerID() + " at " + remotePeer.getHostName());

                Channel channel = session.openChannel("exec");
                System.out.println("remotePeerID"+remotePeer.getPeerID());
                //((ChannelExec) channel).setCommand("lsof -i:6001");
                //((ChannelExec) channel).setCommand("kill 1859806 \nkill 1568769 \nkill 1010464 \nkill 195361 \nkill 183416 \n");
                ((ChannelExec) channel).setCommand("cd CNT4007Project \n" + scriptPrefix + remotePeer.getPeerID() + " " + dir);

                channel.setInputStream(null);
                ((ChannelExec) channel).setErrStream(System.err);

                InputStream input = channel.getInputStream();
                channel.connect();

                System.out.println("Channel Connected to peer# " + remotePeer.getPeerID() + " at "
                        + remotePeer.getHostName() + " server with commands");

                (new Thread(() -> {
                    // TODO: Call cnt4007.peerProcess(peerID);
                    //startPeerProcess(remotePeer.getPeerID(), dir);
//                    peerProcess peerProcess = null;
//                    try {
//                        peerProcess = new peerProcess(dir + "/Common.cfg", dir + "/PeerInfo.cfg");
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                    startPeerProcess(remotePeer.getPeerID());
                    InputStreamReader inputReader = new InputStreamReader(input);
                    BufferedReader bufferedReader = new BufferedReader(inputReader);
                    String line = null;

                    try {

                        while ((line = bufferedReader.readLine()) != null) {
                            System.out.println(remotePeer.getPeerID() + ">:" + line);
                        }
                        bufferedReader.close();
                        inputReader.close();
                    } catch (Exception ex) {
                        System.out.println(remotePeer.getPeerID() + " Exception >:");
                        ex.printStackTrace();
                    }

                    channel.disconnect();
                    session.disconnect();
                })).start();

            } catch (JSchException e) {
// TODO Auto-generated catch block
                System.out.println(remotePeer.getPeerID() + " JSchException >:");
                e.printStackTrace();
            } catch (IOException ex) {
                System.out.println(remotePeer.getPeerID() + " Exception >:");
                ex.printStackTrace();
            }

        }
    }

}