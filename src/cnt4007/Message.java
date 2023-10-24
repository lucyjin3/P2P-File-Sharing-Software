package cnt4007;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;


public class Message {
    // Define message types
    public static final byte CHOKE = 0;
    public static final byte UNCHOKE = 1;
    public static final byte INTERESTED = 2;
    public static final byte NOT_INTERESTED = 3;
    public static final byte HAVE = 4;
    public static final byte BITFIELD = 5;
    public static final byte REQUEST = 6;
    public static final byte PIECE = 7;

    private byte[] messageBytes;

    // Constructor to create a message with a specified message type
    public Message(byte messageType) {
        // Message length is 4 bytes, plus 1 byte for the message type
        messageBytes = new byte[5];
        setMessageType(messageType);
    }

    //set message type
    public void setMessageType(byte messageType) {
        messageBytes[4] = messageType;
    }

    //get message type
    public byte getMessageType() {
        return messageBytes[4];
    }

    public byte[] getMessageBytes() {
        return messageBytes;
    }

    // Get the message length from the message bytes
    public int getMessageLength() {
        return ((messageBytes[0] & 0xFF) << 24) |
                ((messageBytes[1] & 0xFF) << 16) |
                ((messageBytes[2] & 0xFF) << 8) |
                (messageBytes[3] & 0xFF);
    }

    // Set the message length in the message bytes
    public void setMessageLength(int length) {
        messageBytes[0] = (byte) (length >> 24);
        messageBytes[1] = (byte) (length >> 16);
        messageBytes[2] = (byte) (length >> 8);
        messageBytes[3] = (byte) length;
    }

    // Create a 'bitfield' message with the given bitfield data
    public static Message createBitfieldMessage(byte[] bitfield) {
        Message message = new Message(BITFIELD);
        int length = bitfield.length + 1;
        message.setMessageLength(length);
        message.getMessageBytes()[5] = (byte) 0; // First bit of bitfield message
        System.arraycopy(bitfield, 0, message.getMessageBytes(), 6, bitfield.length);
        return message;
    }

    // Create a 'request' message with the given piece index
    public static Message createRequestMessage(int pieceIndex) {
        Message message = new Message(REQUEST);
        message.setMessageLength(5);
        message.getMessageBytes()[5] = (byte) (pieceIndex >> 24);
        message.getMessageBytes()[6] = (byte) (pieceIndex >> 16);
        message.getMessageBytes()[7] = (byte) (pieceIndex >> 8);
        message.getMessageBytes()[8] = (byte) pieceIndex;
        return message;
    }


    // Send the message to the specified output stream
    public void sendMessage(OutputStream outputStream) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        dataOutputStream.write(messageBytes);
        dataOutputStream.flush();
    }
}
