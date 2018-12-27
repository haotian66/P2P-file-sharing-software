/*
* Yuzhuang Chen: UF# 9194-9461
* Haotian Jiang: UF# 6736-6421
* Qiao Xue: UF# 1517-5652
* */

package commonInfo;


import java.io.*;
import java.util.*;

public class PeerInfoChecker {
    public List<PeerInfo> checker() throws Exception {
        List<PeerInfo> peerInfos = new ArrayList<>();
        BufferedReader reader = null;

        try{
            reader = new BufferedReader(new FileReader("config/PeerInfo.cfg"));
           // reader = new BufferedReader(new FileReader("PeerInfo.cfg"));
        }catch (Exception e){
             System.out.println("PeerInfo.cfg file name incorrect: " );
        }
        String line;
        try{
            line = reader.readLine();
            while (line != null){
                //Divided by space
                /* ID || Host name || Port Number || Completed file */
                String[] components = line.split(" ");

                // if the number of arguments are not 4
                if (components.length != 4){
                    throw new RuntimeException("Argument number incorrect");
                }
                PeerInfo peer = new PeerInfo(components[1], Integer.parseInt(components[0]), Integer.parseInt(components[2]), Integer.parseInt(components[3]));
                peerInfos.add(peer);
                line = reader.readLine();
            }
            reader.close();
        }catch (IOException e){
            throw new Exception("Problems in reading PeerInfo.cfg file");
        }

        return peerInfos;
    }
}