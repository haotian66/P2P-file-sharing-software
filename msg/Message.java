/*
* Yuzhuang Chen: UF# 9194-9461
* Haotian Jiang: UF# 6736-6421
* Qiao Xue: UF# 1517-5652
* */

package msg;


import java.nio.ByteBuffer;
import java.util.Arrays;

public class Message {
    private int length;
    private byte[] payload;
    private byte type;
    private  byte[] Length_of_message = new byte[4];
    private   byte[] bitField;
    private byte[] shakehand = new byte[32];
    private int peerID;
    public final static int CHOKE_LENGTH = 5;
    public final static byte CHOKE_TYPE = 0;
    public final static int HANDSHAKE_LENGTH = 32;
    public final static int HAVE_LENGTH = 9;
    public final static byte HAVE_TYPE = 4;
    public final static int INTERESTED_LENGTH = 5;
    public final static byte INTERESTED_TYPE = 2;
    public final static int NOT_INTERESTED_LENGTH = 5;
    public final static byte NOT_INTERESTED_TYPE = 3;
    public final static byte PIECE_TYPE = 7;
    public final static int REQUEST_LENGTH = 9;
    public final static byte REQUEST_TYPE = 6;
    public final static int UNCHOKE_LENGTH = 5;
    public final static byte UNCHOKE_TYPE = 1;
    public Message() {
    }
    public Message(int length, byte type, byte[] payload) {
        this.payload = payload;
        // initial
        this.type = type;
        this.length = length - 4;
    }


    private String bytesToHex(byte[] bytes) {
        char[] Array_of_hex = "0123456789ABCDEF".toCharArray();
        int j=0;
        char[] Char_of_hex = new char[bytes.length * 2];
        while ( j < bytes.length ) {
            int m = bytes[j] & 0xFF;
            Char_of_hex[j * 2] = Array_of_hex[m >>> 4];
            Char_of_hex[j * 2 + 1] = Array_of_hex[m & 0x0F];
            j++;
        }
        return new String(Char_of_hex);
    }

    public synchronized void setBitField(byte[] mesaage){
        int i = 0;
        int n = 0;
        while (i < 4){
            Length_of_message[i] = mesaage[i];
            n += (int)mesaage[i] << (3-i);
            i++;
        }

        bitField = new byte[n + 5];
        bitField = mesaage;
        payload = new byte[n];
        System.out.println("debug: " + bytesToHex(bitField));
        System.arraycopy(mesaage,5,payload,0,n);
    }

    private static byte[] getBooleanArray(byte bytes) {
        byte[] series = new byte[8];
        int i=7;
        while (i >= 0) {
            series[i] = (byte)(bytes & 1);
            bytes = (byte) (bytes >> 1);
            i--;
        }
        return series;
    }
    public byte[] getHandshake() {
        return shakehand;
    }

    public void setBitField(boolean if_hasaFile, int Num_of_pieces){
        type=5;
        int Length_of_payload = (int)Math.ceil((double)Num_of_pieces/8);
        //
        int thingleft = Num_of_pieces % 8;
        Length_of_message = ByteBuffer.allocate(4).putInt(Length_of_payload).array();
        int j;
        payload = new byte[Length_of_payload];
        // mssagelenth is improtant;
        bitField = new byte[Length_of_payload + 5];
        int i = 0;
        while ( i < 4) {
            bitField[i] = Length_of_message[i];
            i++;
        }

        bitField[i] = type;
        if(!if_hasaFile) {
            j=0;
            while (j < payload.length - 1) {
                i++;
                bitField[i] = 0;
                j++;
            }
            if (thingleft == 0){
                return;
            }
            byte lastByte = 0;
            j=0;
            while ( j < 8 - thingleft){
                lastByte += ((1&0xFF) << (j));
                j++;
            }
            bitField[++i] = lastByte;
        }else {
            int k =0;
            while ( k < Length_of_payload){
                bitField[++i] = (byte) 0xFF;
                k++;
            }
        }
    }
    public byte[] getMessageByteArray(){
        byte[] result;
        if (payload == null)
            result = new byte[5];
        else
            result = new byte[5 + payload.length];
        byte[] lengthArray = ByteBuffer.allocate(4).putInt(length).array();
        System.arraycopy(lengthArray,0,result,0,4);
        result[4] = type;
        if (payload != null) {
            System.arraycopy(payload,0,result,5,payload.length);
        }
        return result;
    }
    public void updateBitField(int Index_of_piece){
        int j = (Index_of_piece) / 8;
        int i = 0;
        System.out.println("The bitfield before updated is: " + Arrays.toString(getBooleanArray(bitField[j + 5])));
        int n = 7 - ((Index_of_piece) % 8);
        //let bitfield move left n.
        bitField[j + 5] = (byte) (bitField[j + 5] | ((1 & 0xFF) << n));
        System.out.println("The updated bitfield is: " + Arrays.toString(getBooleanArray(bitField[j + 5])));
    }
    public int getPeerID() {
        return peerID;
    }

    public byte[] getBitFieldByteArray(){
        return bitField;
    }
    public Message(int peerID) {
        this.peerID = peerID;
        String header = "P2PFILESHARINGPROJ" + "0000000000";
        byte[] Array_of_header = header.getBytes();
        int i =0;
        while ( i < header.length()) {
            shakehand[i] = Array_of_header[i];
            i++;
        }
        i=0;
        int add = header.length();
        byte[] peerIDArray = ByteBuffer.allocate(4).putInt(peerID).array();
        while (i < 4){
            shakehand[i + add] = peerIDArray[i];
            i++;
        }
    }
    public Message(byte[] message) throws Exception {
        int j = 0;
        if (message.length != 32) {
            throw new RuntimeException("Problem with reading the message");
        }
        System.arraycopy(message,0,shakehand,0,32);
        byte[] Array_of_peerID = new byte[4];
        System.arraycopy(shakehand,28,Array_of_peerID,0,4);
        peerID = java.nio.ByteBuffer.wrap(Array_of_peerID).getInt();
    }



}
