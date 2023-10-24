package cnt4007;
import java.io.RandomAccessFile;

// Creates (or opens) the filename file and sets it to a file a set size
// Set by the fileSize parameter
public class FileCreator {
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
