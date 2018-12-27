/*
* Yuzhuang Chen: UF# 9194-9461
* Haotian Jiang: UF# 6736-6421
* Qiao Xue: UF# 1517-5652
* */

package connection;

import commonInfo.PeerInfo;
import msg.Message;

public class Peer {
    private String host;
    private int port;
    private int peerId;
    private Message bitFieldMsg;
    private boolean hasCompleteFile;

    public Peer(PeerInfo peerInfo, int piecesNumber) {
        this.host = peerInfo.getHostName();
        this.port = peerInfo.getPort();
        this.peerId = peerInfo.getHostID();
        this.hasCompleteFile = peerInfo.getCompleted() == 1;
        this.bitFieldMsg = new Message();
        if (peerInfo.getCompleted() == 1)
            this.bitFieldMsg.setBitField(true, piecesNumber);
        else
            this.bitFieldMsg.setBitField(false, piecesNumber);
    }

    public boolean getHasCompleteFile(){
        return hasCompleteFile;
    }

    public void setHasCompleteFile(boolean hasCompleteFile){
        this.hasCompleteFile = hasCompleteFile;

    }
    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getPeerId() {
        return peerId;
    }



}
