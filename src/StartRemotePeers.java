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

/**
 * Make sure the below peer hostnames and peerIDs match those in
 * PeerInfo.cfg in the remote CISE machines. Also make sure that the
 * peers which have the file initially have it under the 'peer_[peerID]'
 * folder.
 */

        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter your username: ");
        String username = scanner.nextLine();

        System.out.print("Enter your password: ");
        String password = scanner.nextLine();

        System.out.println("Pick a file: \n[1] project_config_file_large \n[2] project_config_file_small\n[3] project_config_test_file");
        String userInput = scanner.nextLine();

        if(userInput.equals("1")){
            dir = "project_config_file_large";
            file = new File("project_config_file_large/PeerInfo.cfg");
        }else if (userInput.equals("2")){
            dir = "project_config_file_small";
            file = new File("project_config_file_small/PeerInfo.cfg");
        }else if (userInput.equals("3")){
            dir = "project_config_test_file";
            file = new File("project_config_test_file/PeerInfo.cfg");
        }else{
            System.out.println("Invalid option");
            return;
        }

        try {
            Scanner sc = new Scanner(file);
            System.out.println("Reading from PeerInfo.cfg:");
            while (sc.hasNextLine()){
                String peerInfo = sc.nextLine();
                System.out.println(peerInfo);
                String[] line = peerInfo.split(" ");
                peerList.add(new PeerInfo(line[0], line[1]));
            }
        }catch(java.io.FileNotFoundException e){
            System.out.println("Error");
            return;
        }
        try {
            Scanner sc = new Scanner(new File(dir+"/Common.cfg"));
            System.out.println("Reading from Common.cfg:");
            while (sc.hasNextLine()){
                String info = sc.nextLine();
                System.out.println(info);
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
                Session session = jsch.getSession(username, remotePeer.getHostName(), 22);
                session.setPassword(new String(password));
                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);

                session.connect();


                Channel channel = session.openChannel("exec");

                ((ChannelExec) channel).setCommand("cd CNT4007Project \n" + scriptPrefix + remotePeer.getPeerID() + " " + dir);

                channel.setInputStream(null);
                ((ChannelExec) channel).setErrStream(System.err);

                InputStream input = channel.getInputStream();
                channel.connect();

                System.out.println("Channel Connected to peer # " + remotePeer.getPeerID() + " at "
                        + remotePeer.getHostName() + " server with commands");

                (new Thread(() -> {

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
                System.out.println(remotePeer.getPeerID() + " JSchException >:");
                e.printStackTrace();
            } catch (IOException ex) {

                System.out.println(remotePeer.getPeerID() + " Exception >:");
                ex.printStackTrace();
            }

        }
    }

}