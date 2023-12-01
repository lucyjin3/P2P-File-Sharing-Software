package cnt4007;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;


public class Message {
    public byte[] chokeMessage;

    public byte[] unchokeMessage;

    public byte[] interestedMessage;

    public byte[] notInterestedMessage;
    public byte[] lengthBytes;
    public byte[] typeByte;

    public byte[] payloadBytes;

    public String messageType;

    public int length;


    public Map<Integer, String> messageTypeMap;

    // Constructor to create a message with a specified message type
    public Message(byte messageType) {
        // Message length is 4 bytes, plus 1 byte for the message type
        messageTypeMap.put(0, "choke");
        messageTypeMap.put(1, "unchoke");
        messageTypeMap.put(2, "interested");
        messageTypeMap.put(3, "not_interested");
        messageTypeMap.put(4, "have");
        messageTypeMap.put(5, "bitfield");
        messageTypeMap.put(6, "request");
        messageTypeMap.put(7, "piece");

        chokeMessage = new byte[5];
        chokeMessage[4] = 0; // 5th byte is 0

        unchokeMessage = new byte[5];
        unchokeMessage[4] = 1; // 5th byte is 1

        interestedMessage = new byte[5];
        interestedMessage[4] = 2; // 5th byte is 2

        notInterestedMessage = new byte[5];
        notInterestedMessage[4] = 3; // 5th byte is 3

    }
    public void setMessage(InputStream input) throws IOException{
        // Read the first 5 bytes (4 for length, 1 for type)
        lengthBytes = input.readNBytes(4);
        typeByte = input.readNBytes(1);

        // Convert length bytes to String and then parse integer
        String lengthStr = new String(lengthBytes, StandardCharsets.US_ASCII);
        length = Integer.parseInt(lengthStr);

        // Convert type byte to char
        messageType = String.valueOf(new String(typeByte, StandardCharsets.US_ASCII).charAt(0));

        // Read the payload
        payloadBytes = input.readNBytes(length);
        String payload = new String(payloadBytes, StandardCharsets.US_ASCII);
        System.out.println("Length: " + length);
        System.out.println("Type: " + messageType);
        System.out.println("Payload: " + payload);
    }

    public byte[] createHaveMessage(int pieceRequested){
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.putInt(4);
        buffer.put((byte)6);
        buffer.putInt(pieceRequested);
        return buffer.array();
    };

    public byte[] createBitfieldMessage(int[] bitfield){
        ByteBuffer buffer = ByteBuffer.allocate(5 + bitfield.length);
        buffer.putInt(bitfield.length);
        buffer.put((byte)5); // Message type for bitfield
        for (int bit : bitfield) {
            buffer.put((byte)bit);
        }
        return buffer.array();
    };

    public byte[] createRequestMessage(int index){
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.putInt(4);
        buffer.put((byte)6);
        buffer.putInt(index);
        return buffer.array();
    };

    public byte[] createPieceMessage(int index, byte[] pieceContent){
        ByteBuffer buffer = ByteBuffer.allocate(5 + 4 + pieceContent.length);
        buffer.putInt(4 + pieceContent.length);
        buffer.put((byte)7);
        buffer.putInt(index);
        buffer.put(pieceContent);
        return buffer.array();
    };

}
