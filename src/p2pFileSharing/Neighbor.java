package p2pFileSharing;

public class Neighbor {

    public int peerID;
    Bitfield bitfield;
    public boolean choke;
    public boolean interested;
    public boolean preferred;
    public int downloadRate;

    public Neighbor(int peerID, Bitfield bitfield){
        this.peerID = peerID;
        this.bitfield = bitfield;
        this.choke = false;
        this.interested = false;
        this.preferred = false;
        this.downloadRate = 0;
    }

}
