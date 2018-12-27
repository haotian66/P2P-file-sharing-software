/*
* Yuzhuang Chen: UF# 9194-9461
* Haotian Jiang: UF# 6736-6421
* Qiao Xue: UF# 1517-5652
* */

package logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;


public class Logger {

    private static int myPeerID;
    private static FileWriter logFile;
    private static String fileName;
    private static BufferedWriter writer;

    public static void initLogger(int peerID) throws Exception {
        myPeerID = peerID;
        fileName = "log_peer_" + myPeerID + ".log";
        try {
            logFile = new FileWriter(fileName);
            writer = new BufferedWriter(logFile);
        } catch (Exception ex) {
            throw new Exception("Can not open log file");
        }
    }

    synchronized public static void commonLog(int peerID, int args) throws Exception{
        

        System.out.println("Start writing the log file");
        String date = new Date().toString();

        try{
        switch(args){
            // "ConnectTCP"
            case 1:{
            String log = "[" +date+ "] " + ": Peer " + myPeerID + " is connected from Peer " + peerID;
            writer.write(log);
            writer.newLine();

            }
            break;
            //"ConnectedTCP
            case 2:{
            String log = "[" +date+ "] " + ": Peer " + myPeerID + " is connected from Peer " + peerID;
            writer.write(log);
            writer.newLine();

            }
            break;
            // "Unchoke"
            case 3:{
            String log = "[" +date+ "] " + ": Peer " + myPeerID + " is unchoked by " + peerID;
            writer.write(log);
            writer.newLine();

            }
            break;
            // "Choke"
            case 4:{
            String log = "[" +date+ "] " + ": Peer " + myPeerID + " is choked by " + peerID;
            writer.write(log);
            writer.newLine();

            }
            break;
            //"ReceivedInterested"
            case 5:{
            String log = "[" +date+ "] " + ": Peer " + myPeerID + " received the 'interested' msg from " + peerID;
            writer.write(log);
            writer.newLine();

            }
            break;
            //"ReceiveNotInterested"
            case 6:{
            String log = "[" +date+ "] " + ": Peer " + myPeerID + " received the ‘not interested’ msg from " + peerID;
            writer.write(log);
            writer.newLine();

            }
        }

        }catch(Exception e) {
                e.printStackTrace();
        }

    }




    public static void changePreferedNeighbors (ArrayList<String> neighborsPeerID)
            throws Exception {

        StringBuilder stringBuilder = new StringBuilder();
        for (String neigborPeerID : neighborsPeerID) {
            stringBuilder.append(neigborPeerID);
            stringBuilder.append(";");
        }
        String neighborsPeerIDList = stringBuilder.toString().substring(0, (stringBuilder.toString().length() - 1));
        try {
            String date = new Date().toString();
            String log =  "[" +date+ "] " + ": Peer " + myPeerID + " has the prefered neighbors " + neighborsPeerIDList;
            writer.write(log);
            writer.newLine();
        } catch (Exception ex) {
            throw new Exception("Problem with changing the neighbors");
        }
    }



    public static void receiveHave (int peerID, int pieceIndex) throws Exception {
        try {
            String date = new Date().toString();
            String log =  "[" +date+ "] " + ": Peer " + myPeerID + " received the 'have' msg from "+ peerID + " for the piece " + pieceIndex;
            writer.write(log);
            writer.newLine();
        } catch (Exception ex) {
            throw new Exception("Problem with receiving message");
        }
    }


    public static void downloadPiece (int peerID, int pieceIndex) throws Exception {
        try {
            String date = new Date().toString();
            String log =  "[" +date+ "] " + ": Peer " + myPeerID +  " has downloaded the piece " + pieceIndex + " from " + peerID;
            writer.write(log);
            writer.newLine();
        } catch (Exception ex) {
            throw new Exception("Problem with pieces download");
        }
    }

    public static void completeDownload () throws Exception {
        try {
            String date = new Date().toString();
            String log =  "[" +date+ "] " + ": Peer " + myPeerID + " has complete the file";
            writer.write(log);
            writer.newLine();
        } catch (Exception ex) {
            throw new Exception("Problem with complete download");
        }
    }

    public static void closeLogger () throws Exception {
        try {
            writer.close();
        } catch (Exception ex) {

            throw new Exception("Problem with closing the file");
        }
    }

}
