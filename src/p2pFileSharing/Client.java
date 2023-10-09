package p2pFileSharing;

import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        try {
            String host = "127.0.0.1";
            int port = 4444;
            Socket socket = new Socket(host, port);
            System.out.println("Connected to the server");

            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while ((line = userInput.readLine()) != null) {
                writer.println(line);
                String response = reader.readLine();
                System.out.println("Server says: " + response);
                if (line.equals("exit")) break;
            }

            socket.close();
        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O Error: " + ex.getMessage());
        }
    }
}
