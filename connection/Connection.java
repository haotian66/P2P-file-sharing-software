/*
* Yuzhuang Chen: UF# 9194-9461
* Haotian Jiang: UF# 6736-6421
* Qiao Xue: UF# 1517-5652
* */

package connection;


import exception.message.MessageException;
import logger.Logger;
import msg.*;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.*;

public class Connection extends Thread {
    private Socket socket;
    private Peer peer;
    private int idofme;
    private BufferedOutputStream outputStream;
    private BufferedInputStream inputStream;
    private boolean npreneighbors;
    private boolean optimisticpre;
    private boolean interested;
    private boolean broadcastHave;
    private Queue<Integer> formerreceive;
    //Access connection process
    private peerProcess process;
    private int thebytesget;
    private boolean isRunning;
    private boolean unchoke;


    public Connection(Socket socket, peerProcess process, Peer peer, int myPeerID) {
        this.socket = socket;
        this.peer = peer;
        this.process = process;
        this.idofme = myPeerID;
        this.npreneighbors = false;
        this.optimisticpre = false;
        this.thebytesget = 0;
        this.isRunning = true;
        formerreceive = new LinkedList<>();
        unchoke = false;
        try {
            this.outputStream = new BufferedOutputStream(this.socket.getOutputStream());
            this.inputStream = new BufferedInputStream(this.socket.getInputStream());
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void setPreferN(boolean preferN){
        if (preferN){unchoke = true; }
        this.npreneighbors = preferN;
    }

    private synchronized boolean transmit(Message message) {
        try{
            this.outputStream.write(message.getMessageByteArray());
            this.outputStream.flush();
        }catch(IOException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private synchronized boolean handshake(){
        try{
            //Send handshakeMsg
            this.outputStream.write(new Message(idofme).getHandshake());
            this.outputStream.flush();
            //Wait for reply
            byte[] response = new byte[Message.HANDSHAKE_LENGTH];
            int result = inputStream.read(response);
            System.out.println("Result : " + result);
            while (result <= 0){

                result = this.inputStream.read(response);
            }
            Message handshakeMsg = new Message(response);
            System.out.println(peer.getPeerId());
            if (handshakeMsg.getPeerID() != peer.getPeerId()){
                return false;
            }
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private synchronized boolean sendBitfield(){
        try{
            Message field = process.getBitField(this.idofme);
            outputStream.write(field.getBitFieldByteArray());
            outputStream.flush();
            System.out.println("Sent bitfield successfully");
            //wait for bit field
            byte[] response = new byte[field.getBitFieldByteArray().length];
            inputStream.read(response);
            Message bitfldmessage = new Message();
            bitfldmessage.setBitField(response);
            process.addBitField(bitfldmessage, peer.getPeerId());
            System.out.println("Receive bitfield successfully");
            if(isInterested(peer.getPeerId())) {
                System.out.println("InterestedMsg!");
                sendInterested();
            } else {
                System.out.println("Not InterestedMsg!");
                sendNotInterested();
            }
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private synchronized boolean sendRequest(){
        byte[] randomIndex = getRandomPieceIndex();
        if (randomIndex == null){
            return true;
        }
        Message requestMessage = new Message(Message.REQUEST_LENGTH, Message.REQUEST_TYPE,
                randomIndex);
        System.out.println("send request to Peer: " + peer.getPeerId());
        return transmit(requestMessage);

    }

    private synchronized boolean sendPiece(int pieceID){
        byte[] pieceContent = process.getFilePart(pieceID);
        int numallo = 4;
        byte[] pieceNum = ByteBuffer.allocate(numallo).putInt(pieceID).array();
        byte[] content = new byte[pieceContent.length + pieceNum.length];
        int i = 0;
        while(i < 4){
            content[i] = pieceNum[i];
            i = i+1;
        }
        i = 0;
        while(i<pieceContent.length)
        {
            content[i+4] = pieceContent[i];
            i = i+1;
        }
        //4 bytes length + 1 byte type + 4 byte index + length
        Message pieceMessage = new Message(pieceContent.length + 9, Message.PIECE_TYPE,
                content);
        System.out.println("Send piece index: " + pieceID + ", length : " + (process.getPieceSize() + 5));
        return transmit(pieceMessage);
    }

    private synchronized boolean sendInterested(){
        Message interestedMsg = new Message(Message.INTERESTED_LENGTH, Message.INTERESTED_TYPE, null);
        System.out.println("Sent InterestedMsg!");
        return transmit(interestedMsg);
    }

    private synchronized boolean sendNotInterested(){
        Message notInterestedMsg = new Message(Message.NOT_INTERESTED_LENGTH, Message.NOT_INTERESTED_TYPE, null);
        System.out.println("Sent Not InterestedMsg!");
        return transmit(notInterestedMsg);
    }

    private synchronized boolean sendChocked() {
        Message chockedMsg = new Message(Message.CHOKE_LENGTH, Message.CHOKE_TYPE, null);
        try {
            Logger.commonLog(peer.getPeerId(), 4);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return transmit(chockedMsg);
    }

    private synchronized boolean sendUnchocked(){
        Message unchockedMsg = new Message(Message.UNCHOKE_LENGTH, Message.UNCHOKE_TYPE, null);
        System.out.println("Unchocked connection " + peer.getPeerId());
        try {
            Logger.commonLog(peer.getPeerId(), 3);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return transmit(unchockedMsg);
    }
    private synchronized boolean sendHave(){
        int numallo = 4;
        Message sendMsg = new Message(Message.HAVE_LENGTH, Message.HAVE_TYPE,
                ByteBuffer.allocate(numallo).putInt(formerreceive.peek()).array());
        System.out.println("Sent have piece " + formerreceive.poll() + " sent to " + peer.getPeerId());
        return transmit(sendMsg);
    }
    public synchronized void broadcastHave(int received){
        broadcastHave = true;
        formerreceive.offer(received);
    }

    private synchronized boolean isInterested(int peerId) {
        Boolean isInterested = false;
        byte[] myBitfieldArray = process.getBitField(idofme).getBitFieldByteArray();
        byte[] neighborBitfieldArray = process.getBitField(peerId).getBitFieldByteArray();
        int i = 0;
        while(i < myBitfieldArray.length) {
            byte myByte = myBitfieldArray[i];
            byte neighborByte = neighborBitfieldArray[i];

            int j = 7;
            while(j>=0) {
                if ((((1&0xFF) << j) & myByte) == 0 && (((1&0xFF) << j) & neighborByte) != 0) {
                    isInterested = true;
                    //Not exist.
                    if (process.getInterestedPieces().indexOf((i-5) * 8 + 7 - j) == -1){
                        synchronized (this){
                            process.getInterestedPieces().add((i-5) * 8 + 7 - j);
                        }
                    }
                } else if(process.getNotInterestedPieces().contains(new Integer((i-5) * 8 + 7 - j))) {
                    synchronized (this){
                        process.getNotInterestedPieces().remove(new Integer((i-5) * 8 + 7 - j));
                    }
                }
                j = j-1;
            }
            i = i+1;
        }
        this.interested = isInterested;
        return isInterested;
    }



    //Get a random number of piece from peerId have but me don't have
    private synchronized byte[] getRandomPieceIndex(){
        HashSet<Integer> set = process.getRequestingPeices();
        if (set.size() == process.getInterestedPieces().size()){
            return null;
        }
        int requestedPieceIndex;
        do {

            requestedPieceIndex = this.process.getInterestedPieces().get(new Random().
                    nextInt(this.process.getInterestedPieces().size()));
            System.out.println(requestedPieceIndex);
        } while (set.contains(requestedPieceIndex));
        System.out.println("RequestMsg peice index " + requestedPieceIndex);
        synchronized (this){
            set.add(requestedPieceIndex);
        }
        return ByteBuffer.allocate(4).putInt(requestedPieceIndex).array();
    }


    public synchronized void doneCalculating(){
        thebytesget = 0;
    }
    public synchronized boolean getPreferN(){
        return npreneighbors;
    }
    public synchronized boolean getOpPrefer(){
        return optimisticpre;
    }
    public synchronized void setOpPrefer(boolean opPrefer){
        this.optimisticpre = opPrefer;
        if (opPrefer){
            unchoke = true;
        }
    }
    public synchronized int getDownloadBytes(){
        return thebytesget;
    }

    public synchronized boolean getIsRunning(){return isRunning;}
    @Override
    public void run() {
        //HandshakeMsg first
        if (!handshake()){
            System.out.println("HandshakeMsg failed");
            return;
        }
        System.out.println("handshake successfully");

        //Send Bitfield
        if (!sendBitfield()){
            return;
        }
        sendChocked();

        while (true){
            //Send
            if (formerreceive.size() != 0){
                sendHave();
            }
            if (unchoke){
                sendUnchocked();
                unchoke = false;
            }

            byte[] reply = new byte[4];
            try{
                int result = 0;
                if (inputStream.available() > 0)
                    result = inputStream.read(reply);
                else
                    continue;

                //if data arrived
                while(result == 4){
                    int length = 0;

                    length = reply[3] & 0xFF |
                            (reply[2] & 0xFF) << 8 |
                            (reply[1] & 0xFF) << 16 |
                            (reply[0] & 0xFF) << 24;

                    length--;
                    byte[] type = new byte[1];
                    result = inputStream.read(type);
                    if (result != 1){
                        throw new MessageException();
                    }
                    byte[] payload = null;
                    if (length > 0){
                        System.out.println("Receive length " + length);
                        payload = new byte[length];
                        //Wait for data
                        while (inputStream.available() < length);
                        result = inputStream.read(payload);
                        if (result != length){
                            System.out.println("Wrong result");
                            throw new MessageException();
                        }
                    }

                    switch (type[0]) {
                        case Message.UNCHOKE_TYPE:
                            System.out.println("Received unchocked from " + peer.getPeerId());
                            sendRequest();
                            break;
                        case Message.CHOKE_TYPE:
                            System.out.println("Received chocked from " + peer.getPeerId());
                            break;
                        case Message.HAVE_TYPE:
                            //Update here
                            int pieceNum1 = 0;

                            pieceNum1 += payload[3] & 0xFF |
                                    (payload[2] & 0xFF) << 8 |
                                    (payload[1] & 0xFF) << 16 |
                                    (payload[0] & 0xFF) << 24;

                            try {
                                Logger.receiveHave(peer.getPeerId(), pieceNum1);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            System.out.println("Receive have piece " + pieceNum1);
                            process.updateBitField(pieceNum1, peer.getPeerId());
                            if (isInterested(peer.getPeerId())) {
                                sendInterested();
                            } else {
                                sendNotInterested();
                            }
                            break;
                        case Message.REQUEST_TYPE:
                            System.out.println("Receive request from Peer: " + peer.getPeerId());
                            int pieceNum2 = 0;

                            pieceNum2 =pieceNum2+ payload[3] & 0xFF |
                                    (payload[2] & 0xFF) << 8 |
                                    (payload[1] & 0xFF) << 16 |
                                    (payload[0] & 0xFF) << 24;

                            System.out.println("Receive request for piece: " + pieceNum2);
                            sendPiece(pieceNum2);
                            break;
                        case Message.INTERESTED_TYPE:
                            try {
                                Logger.commonLog(peer.getPeerId(), 5);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            System.out.println("Receive InterestedMsg from connection " + peer.getPeerId());
                            process.updateInterestPeer(peer.getPeerId(), true);
                            break;
                        case Message.NOT_INTERESTED_TYPE:
                            try {
                                Logger.commonLog(peer.getPeerId(), 6);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            System.out.println("Receive Not InterestedMsg from connection " + peer.getPeerId());
                            process.updateInterestPeer(peer.getPeerId(), false);
                            break;
                        case Message.PIECE_TYPE:
                            //Get piece index
                            int pieceNum3 = payload[3] & 0xFF |
                                    (payload[2] & 0xFF) << 8 |
                                    (payload[1] & 0xFF) << 16 |
                                    (payload[0] & 0xFF) << 24;
                            try {
                                Logger.downloadPiece(peer.getPeerId(), pieceNum3);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            System.out.println("Receive piece index: " + pieceNum3);
                            //Update my bit field
                            process.updateBitField(pieceNum3, idofme);
                            if (isInterested(peer.getPeerId())) {
                                //after receive a piece, continue to send request
                                sendRequest();
                            } else {
                                sendNotInterested();
                            }
                            thebytesget = thebytesget+length;
                            //Send piece to file
                            process.writeIntoFile(Arrays.copyOfRange(payload, 4, payload.length), pieceNum3);
                            process.sendHave(pieceNum3);
                            break;
                        default:
                            System.out.println("Unexpected msg");
                            break;
                    }
                    if (inputStream.available() >= 4)
                        result = inputStream.read(reply);
                    else
                        break;
                }
            }catch(IOException e){
                e.printStackTrace();
            }catch (MessageException e){
                e.printStackTrace();
            }
        }

    }
}
