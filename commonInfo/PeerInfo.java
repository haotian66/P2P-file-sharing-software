/*
* Yuzhuang Chen: UF# 9194-9461
* Haotian Jiang: UF# 6736-6421
* Qiao Xue: UF# 1517-5652
* */

package commonInfo;


public class PeerInfo {
    private final String hostName;
    private final int hostID;
    private final int port;
    private final int completed;

    public PeerInfo(String hostName, int hostID, int port, int Completed) {
        this.hostName = hostName;
        this.hostID = hostID;
        this.port = port;
        this.completed = Completed;
    }

    public String getHostName() {
        return hostName;
    }

    public int getHostID() {
        return hostID;
    }

    public int getPort() {
        return port;
    }


    public int getCompleted() {
        return completed;
    }
}
