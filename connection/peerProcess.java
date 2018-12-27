/*
* Yuzhuang Chen: UF# 9194-9461
* Haotian Jiang: UF# 6736-6421
* Qiao Xue: UF# 1517-5652
* */

package connection;

import commonInfo.CommonManager;
import commonInfo.Constant;
import commonInfo.PeerInfo;
import commonInfo.PeerInfoChecker;
import logger.Logger;
import msg.Message;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class peerProcess {
    private int peerId;
    private byte[] file;
    private int fileSize;
    private String fileName;
    private int pieceSize;
    private int pieceNum;
    private HashMap<Integer, Peer> peers;
    private int optimisticlyUnchokedInterval;
    private HashMap<Integer, Peer> preferredNeighbors;
    private int numberOfPreferredNeighbors;
    private HashMap<Integer , Message> bitFields;
    private int unchokingInterval;
    private HashMap<Integer, Connection> HashMap_connection;
    private ArrayList<Integer> interestedPieces;
    private HashMap<Integer, Integer> interestedPeer;
    private ArrayList<Integer> notInterestedPieces;
    private HashSet<Integer> requestingPeices;
    private PeerInfo Thispeer;
    private Timer timer;
    
 public static void main(String[] args) {
        int peerId = Integer.parseInt(args[0]);

        //analyse config file
        peerProcess peerProcess = new peerProcess(peerId);
        peerProcess.run();
    }

    private peerProcess(int peerId) {
        this.timer = new Timer();
        this.peerId = peerId;
        this.notInterestedPieces = new ArrayList<>();
        this.preferredNeighbors = new HashMap<>();
        this.requestingPeices = new HashSet<>();
        this.interestedPieces = new ArrayList<>();
    }
   

    private void init() {
        CommonManager commonManager = new CommonManager();
        PeerInfoChecker peerInfoChecker = new PeerInfoChecker();
        try {
            HashMap<String, Object> configs = commonManager.analyze();
            this.numberOfPreferredNeighbors = (int) configs.get(Constant.STRING_NUMBER_OF_PREFER_NEIGHBOURS);
            this.unchokingInterval = (int) configs.get(Constant.STRING_UNCHOKING_INTERVAL);
            this.optimisticlyUnchokedInterval = (int) configs.get(Constant.STRING_OPTIMISTIC_UNCHOKING_INTERVAL);
            this.fileName = (String) configs.get(Constant.STRING_FILE_NAME);
            this.fileSize = (int) configs.get(Constant.STRING_FILE_SIZE);
            this.pieceSize = (int) configs.get(Constant.STRING_PIECE_SIZE);
            this.file = new byte[this.fileSize];
            this.interestedPeer = new HashMap<>();
            try {
                Logger.initLogger(peerId);
            } catch (Exception e) {
                e.printStackTrace();
            }

            List<PeerInfo> peerInfos = peerInfoChecker.checker();
            
            int piecesNumber = (int) Math.ceil((double) this.fileSize / this.pieceSize);
            pieceNum = piecesNumber;
            this.bitFields = new HashMap<>();
            this.peers = new HashMap<>();
            for (PeerInfo peerInfo : peerInfos) {
                if (peerInfo.getHostID() == peerId) {
                    Thispeer = peerInfo;
                }
                this.peers.put(peerInfo.getHostID(), new Peer(peerInfo, piecesNumber));
            }
            Message bitFieldMsg = new Message();
            if (Thispeer.getCompleted() > 0) {
                bitFieldMsg.setBitField(true, piecesNumber);
                this.bitFields.put(this.peerId, bitFieldMsg);
            } else {
                bitFieldMsg.setBitField(false, piecesNumber);
                this.bitFields.put(this.peerId, bitFieldMsg);
            }
            for (Peer p : peers.values()){
                System.out.println(p.getPeerId());
            }
            if (Thispeer.getCompleted() == 1){
                setTheFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void run() {
        init();

        //build connections
        TimerTask updatePreferredNeighbors = new UpdatePreferredNeighbors();
        TimerTask updateOptimisticNeighbor = new UpdateOptimisticNeighbor();;
        TimerTask AllFinished = new CheckAllThreadRunning();

        this.HashMap_connection = new HashMap<>();

        timer.schedule(updatePreferredNeighbors, 3000, this.unchokingInterval);
        timer.schedule(updateOptimisticNeighbor, 3000, this.optimisticlyUnchokedInterval);
        timer.schedule(AllFinished, 7000, 5000);
        ServerSocket serverSocket = null;
        try{
            serverSocket = new ServerSocket(Thispeer.getPort());
        }catch(IOException e){
            e.printStackTrace();
        }
        for (Peer peer : peers.values()) {
            if (peer.getPeerId() < this.peerId){
                try {
                    System.out.println(peer.getHost());
                    Socket socket = new Socket(peer.getHost(), peer.getPort());
                    Connection connection = new Connection(socket, this, peer, this.peerId);
                    try {
                        Logger.commonLog(peer.getPeerId(),1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println("Connected to " + peer.getPeerId());
                    HashMap_connection.put(peer.getPeerId(), connection);
                    connection.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        while (true) {
            try {
                System.out.println("Waiting for connecting");
                Socket receivedSocket = serverSocket.accept();
                String ip = receivedSocket.getRemoteSocketAddress().toString().split(":")[0].substring(1);
                System.out.println("Connected to " + ip);
                try {
                    Logger.commonLog(peerId,2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Connection connection;
                for (Peer p : peers.values()) {
                   if (p.getHost().equals(ip) && p.getPeerId() != peerId) {
                        connection = new Connection(receivedSocket, this, p, peerId);
                        this.HashMap_connection.put(p.getPeerId(), connection);
                        connection.start();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private class UpdateOptimisticNeighbor extends TimerTask {
        @Override
        public void run() {
            List<Connection> connectors = new ArrayList<>();
            for (Map.Entry e : interestedPeer.entrySet()){
                HashMap_connection.get(e.getKey()).setOpPrefer(false);
                if (!HashMap_connection.get(e.getKey()).getPreferN()){
                    if (!HashMap_connection.get(e.getKey()).getOpPrefer()){
                        connectors.add(HashMap_connection.get(e.getKey()));
                    }
                }
            }
            if (connectors.size() > 0){
                Collections.shuffle(connectors);
                connectors.get(0).setOpPrefer(true);
            }
        }
    }
    public HashSet<Integer> getRequestingPeices() {
        return requestingPeices;
    }


    private class UpdatePreferredNeighbors extends TimerTask {
        @Override
        public void run() {
            HashMap<Integer, Double> speed = new HashMap<>();
            for (Map.Entry e : HashMap_connection.entrySet()){
                Connection c = (Connection) e.getValue();
                speed.put((Integer)e.getKey(), (double)c.getDownloadBytes() / this.scheduledExecutionTime());
                c.doneCalculating();
            }

            preferredNeighbors.clear();
            int j = 0;
            while (j < numberOfPreferredNeighbors ){
                double max = -1;
                int id = -1;
                for (Map.Entry e : speed.entrySet()){

                    if ((Double)e.getValue() > max && interestedPeer.containsKey(e.getKey())){
                        max = (Double)e.getValue();
                        id = (Integer)e.getKey();
                    }
                }
                if (max == -1 || id == -1){
                    return;
                }
                if (!HashMap_connection.get(id).getPreferN() && HashMap_connection.get(id).getIsRunning()){
                    HashMap_connection.get(id).setPreferN(true);
                }
                preferredNeighbors.put(id, peers.get(id));
                speed.remove(id);
                j++;
            }
            ArrayList<String> preferedNeighborList = new ArrayList<>();
            for(Integer i : preferredNeighbors.keySet()){
                preferedNeighborList.add(String.valueOf(i));
            }
            try {
                Logger.changePreferedNeighbors(preferedNeighborList);
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (Map.Entry e : speed.entrySet()){
                HashMap_connection.get(e.getKey()).setPreferN(false);
            }
        }
    }

    public synchronized void addBitField(Message bitFieldMsg, int peerID){
        if (!bitFields.containsKey(peerID)){
            bitFields.put(peerID, bitFieldMsg);
            byte[] bytes = bitFieldMsg.getBitFieldByteArray();
            int length = bytes.length-1;
            while(length >=0){
                if ((bytes[length] & 0xFF) != 0xFF) {
                    return;
                }else
                    length--;
            }


            peers.get(peerID).setHasCompleteFile(true);
        }
    }

    public synchronized void updateInterestPeer(int peerId, boolean isInterest){
        if (interestedPeer.containsKey(peerId)){
            if (!isInterest){
                interestedPeer.remove(peerId);
            }
        }else {
            if (isInterest){
                interestedPeer.put(peerId, peerId);
            }
        }
        System.out.println("Interest connection : ");

    }
    private class CheckAllThreadRunning extends TimerTask{
        @Override
        public void run(){
            for (Peer peer : peers.values()){
                if (!peer.getHasCompleteFile()){
                    return;
                }
            }
            try {
                Logger.closeLogger();
                System.out.println("All finished!");
                System.exit(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public ArrayList<Integer> getNotInterestedPieces() {
        return notInterestedPieces;
    }

    public synchronized void writeIntoFile(byte[] partOfFile, int index) throws IOException{
        boolean finish = true;
        int i =0;
        while ( i < partOfFile.length){
            file[i+index*pieceSize] = partOfFile[i];
            i++;
        }
        Message bitFieldMsg = this.getBitField(peerId);
        byte[] bitFieldByteArray = bitFieldMsg.getBitFieldByteArray();
        i=0;
        while ( i < bitFieldByteArray.length - 5){
            if((bitFieldByteArray[i+5]) != (byte)255){
                finish = false;
            }
            i++;
        }
        if(finish){
            try {
                Logger.completeDownload();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("The file complete!");
            File dir = new File("peer_" + peerId);
            dir.mkdir();
            FileOutputStream fileOutputStream = new FileOutputStream("peer_" + peerId +"/" + fileName);
            fileOutputStream.write(file, 0, fileSize);
            fileOutputStream.close();
        }
    }
    public synchronized Message getBitField(int peerID){
        if (bitFields.containsKey(peerID)){
            return bitFields.get(peerID);
        }
        return null;
    }
    public synchronized byte[] getFilePart(int fileIndex){
        int start = fileIndex * pieceSize;
        if (fileIndex + 1== pieceNum){
            return Arrays.copyOfRange(file, start, fileSize);
        }
        return Arrays.copyOfRange(file, start, start + pieceSize);
    }
    public synchronized boolean setTheFile(){
        File f = new File(fileName);
        try{
            FileInputStream fs = new FileInputStream(f);
            int result = fs.read(file);
            System.out.println("Bytes of file : " + result);
            fs.close();
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }
    public synchronized void sendHave(int index){
        for (HashMap.Entry e : HashMap_connection.entrySet()){
            Connection c =(Connection)e.getValue();
            c.broadcastHave(index);
        }
    }
    public int getPieceSize(){
        return pieceSize;
    }
    public static byte[] getBooleanArray(byte b) {
        byte[] array = new byte[8];
        int i =7;
        while ( i >= 0) {
            array[i] = (byte)(b & 1);
            b = (byte) (b >> 1);
            i--;
        }
        return array;
    }
    public synchronized void updateBitField(int pieceIndex, int peerId){
        Message bitFieldMsg = bitFields.get(peerId);
        bitFieldMsg.updateBitField(pieceIndex);
        byte[] bitFieldByteArray = bitFieldMsg.getBitFieldByteArray();
        System.out.println("bitfield length: " + bitFieldByteArray.length);
        System.out.println("payload: " + Arrays.toString(getBooleanArray(bitFieldByteArray[5])));
        int i = 0;
        while ( i < bitFieldByteArray.length-5){
            if(bitFieldByteArray[i+5] != (byte)255){
                return;
            }
            i++;
        }
        System.out.println("connection: " + peerId + "become completed");
        peers.get(peerId).setHasCompleteFile(true);
    }
    public ArrayList<Integer> getInterestedPieces() {
        return interestedPieces;
    }

}
