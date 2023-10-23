package cnt4007;

import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        final int MAX_RETRIES = 5;  // Define how many times you want to retry
        final int RETRY_DELAY_MS = 5000;  // Wait 5 seconds before retrying

        String host = "localhost";
        int port = 4444;
        int retryCount = 0;

        // Handle possible delays in previous peer servers starting
        while (retryCount < MAX_RETRIES) {
            try {
                Socket socket = new Socket(host, port);
                System.out.println("Connected to the server");

                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);
                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
                String line;
                // Reading what the server is saying
                while ((line = userInput.readLine()) != null) {
                    writer.println(line);
                    String response = reader.readLine();
                    System.out.println("Server says: " + response);
                    if (line.equals("exit")) break;
                }

                socket.close();
                break;  // Exit the loop once connected and after communication is done
            } catch (UnknownHostException ex) {
                System.out.println("Server not found: " + ex.getMessage());
                return;  // If server is not found, it makes no sense to retry, so return from the method
            } catch (IOException ex) {
                System.out.println("I/O Error: " + ex.getMessage());
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
            }
        }
    }
}
