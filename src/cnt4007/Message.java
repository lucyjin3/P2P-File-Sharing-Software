package cnt4007;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;



public class Message {
    public byte[] chokeMessage;

    public byte[] unchokeMessage;

    public byte[] interestedMessage;

    public byte[] notInterestedMessage;
    public byte[] lengthBytes;
    public byte[] typeByte;
    public int[] bitFieldFromMsg;

    public byte[] payloadBytes;
    public String payload;

    public int messageType;
    public int index;

    public int length;


    public Map<Integer, String> messageTypeMap;

    // Constructor to create a message with a specified message type
    public Message() {
        messageTypeMap = new HashMap<>();
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
    public void receiveMessage(ObjectInputStream input) throws IOException {

        System.out.println("Please");
        byte[] lengthBytes = input.readNBytes(4);
        ByteBuffer lengthBuffer = ByteBuffer.wrap(lengthBytes);
        length = lengthBuffer.getInt();
        System.out.println("Length: " + length);

        byte[] typeBytes = input.readNBytes(1);
        messageType = typeBytes[0] & 0xFF;

        System.out.println("Message type: " + messageType);

        // Process the payload...
        payloadBytes = input.readNBytes(length);
        if (messageType == 5) {
            // Process bitfield message type
            bitFieldFromMsg = unpackBitfield(payloadBytes, length * 8);
        }
    }
    public int[] unpackBitfield(byte[] payloadBytes, int bitfieldSize) {
        int[] bitfield = new int[bitfieldSize];

        for (int i = 0; i < payloadBytes.length; i++) {
            for (int j = 0; j < 8; j++) {
                int bitIndex = i * 8 + j;
                if (bitIndex < bitfield.length) {
                    bitfield[bitIndex] = (payloadBytes[i] >> (7 - j)) & 1;
                }
            }
        }
        return bitfield;
    }


    public byte[] getInterestedMessage(){
        return this.interestedMessage;
    }
    public byte[] getNotInterestedMessage(){
        return this.notInterestedMessage;
    }
    public byte[] createHaveMessage(int pieceRequested){
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.putInt(4);
        buffer.put((byte)6);
        buffer.putInt(pieceRequested);
        return buffer.array();
    };

    public byte[] createBitfieldMessage(int[] bitfield) {
        // Calculate the number of bytes needed to represent the bitfield (8 bits per byte)
        int numBytes = (bitfield.length + 7) / 8;

        ByteBuffer buffer = ByteBuffer.allocate(5 + numBytes);
        buffer.putInt(numBytes); // Note that we're putting numBytes here, not bitfield.length
        buffer.put((byte)5); // Message type for bitfield

        byte currentByte = 0;
        for (int i = 0; i < bitfield.length; i++) {
            // Set the bit in the current byte
            currentByte |= (bitfield[i] << (7 - (i % 8)));

            // If this is the last bit for the current byte, or the last bit overall
            if (i % 8 == 7 || i == bitfield.length - 1) {
                buffer.put(currentByte);
                currentByte = 0; // Reset for the next byte
            }
        }

        return buffer.array();
    }


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
