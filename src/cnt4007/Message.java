package cnt4007;

import java.nio.ByteBuffer;
import java.util.*;
import java.io.IOException;
import java.util.Arrays;



public class Message {
    public byte[] chokeMessage;

    public byte[] unchokeMessage;

    public byte[] interestedMessage;

    public byte[] notInterestedMessage;
    public int[] bitFieldFromMsg;

    public byte[] payloadBytes;
    public int payloadInt;

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
    public void receiveMessage(int length, int type, byte[] payloadBytes) throws IOException {

        this.length = length;

        this.messageType = type;

        // Process the payload...
        this.payloadBytes = payloadBytes;
        if (messageType == 4){ //HAVE MESSAGE
            payloadInt = ((payloadBytes[0] & 0xFF) << 24) | ((payloadBytes[1] & 0xFF) << 16) |
                    ((payloadBytes[2] & 0xFF) << 8) | (payloadBytes[3] & 0xFF);
        }
        else if (messageType == 5){ //BITFIELD MESSAGE
            // Process bitfield message type
            bitFieldFromMsg = unpackBitfield(this.payloadBytes, length * 8);
        }
        else if (messageType == 6){ //REQUEST MESSAGE
            // Process bitfield message type
            // This will be the index of the piece
            payloadInt = ((payloadBytes[0] & 0xFF) << 24) | ((payloadBytes[1] & 0xFF) << 16) |
                    ((payloadBytes[2] & 0xFF) << 8) | (payloadBytes[3] & 0xFF);
        }
        else if (messageType == 7){ //PIECE
            payloadInt = ((payloadBytes[0] & 0xFF) << 24) | ((payloadBytes[1] & 0xFF) << 16) |
                    ((payloadBytes[2] & 0xFF) << 8) | (payloadBytes[3] & 0xFF);
            this.payloadBytes = Arrays.copyOfRange(payloadBytes, 4, payloadBytes.length);
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
        buffer.put((byte)4);
        buffer.putInt(pieceRequested);
        return buffer.array();
    }

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
        this.payloadInt = index;
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.putInt(4);
        buffer.put((byte)6);
        buffer.putInt(index);
        return buffer.array();
    }

    public byte[] createPieceMessage(int index, byte[] pieceContent){
        ByteBuffer buffer = ByteBuffer.allocate(5 + 4 + pieceContent.length);
        buffer.putInt(4 + pieceContent.length);
        buffer.put((byte)7);
        buffer.putInt(index);
        buffer.put(pieceContent);
        return buffer.array();
    }
}
