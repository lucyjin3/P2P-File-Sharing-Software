package p2pFileSharing;

public class Peer {

    //file
    Bitfield bitfield;
    Neighbor[] currentNeighbors;

    public Peer(Bitfield bitfield){
        this.bitfield = bitfield;
    }
}
