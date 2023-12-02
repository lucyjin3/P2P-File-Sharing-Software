package cnt4007;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.nio.ByteBuffer;

// Creates (or opens) the filename file and sets it to a file a set size
// Set by the fileSize parameter
public class FileCreator {
    public static synchronized void writeToFile(int numPieces, int index, byte[] payload, String filePath) {
        long position = (long) index * numPieces;

        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile(filePath, "rw");

            file.seek(position);

            file.write(payload);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            if (file != null) {
                try {
                    file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public static synchronized byte[] readFile(int numPieces, int index, int pieceSize, String filePath) {
        // Calculate the start position in the file
        long position = (long) index * numPieces;

        RandomAccessFile file = null;
        byte[] data = new byte[pieceSize];

        try {
            // Open the file in read mode
            file = new RandomAccessFile(filePath, "r");

            // Seek to the calculated position
            file.seek(position);

            // Read 'pieceSize' bytes into the byte array
            int bytesRead = file.read(data, 0, pieceSize);

            // In case the actual bytes read are less than 'pieceSize', adjust the array
            if (bytesRead < pieceSize) {
                data = java.util.Arrays.copyOf(data, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            // Close the file
            if (file != null) {
                try {
                    file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return data;
    }
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please provide the desired file size in bytes as an argument.");
            return;
        }

        try {
            long size = Long.parseLong(args[1]);

            // Create (or open if it already exists) a file named "output.dat"
            RandomAccessFile raf = new RandomAccessFile(args[0], "rw");

            // Set the file size based on the passed argument
            raf.setLength(size);

            raf.close();

            System.out.println("File " + args[0] + " created with size: " + size + " bytes");
        }
        catch (NumberFormatException e)
        {
            System.out.println("Invalid size provided. Please provide a valid number.");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
