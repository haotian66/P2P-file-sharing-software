/*
* Yuzhuang Chen: UF# 9194-9461
* Haotian Jiang: UF# 6736-6421
* Qiao Xue: UF# 1517-5652
* */

package commonInfo;

import java.io.*;
import java.util.HashMap;


//reading the Common.cfg file
public class CommonManager {

    private HashMap<String, Object> hashMap = new HashMap<>();

    public HashMap<String, Object> analyze() throws Exception {
        BufferedReader reader = null;
        String line;
        try{
            reader = new BufferedReader(new FileReader("config/Common.cfg"));
          //  reader = new BufferedReader(new FileReader("Common.cfg"));

        }catch (Exception e){
            System.out.println("Common.cfg file name incorrect");
        }

        try{
            while ((line = reader.readLine()) != null) {
                String[] components = line.split(" ");

                String key = components[0];
                String value = components[1];

                if (key.equals("FileName")) {
                    hashMap.put(key, value);
                } else {
                    hashMap.put(key, Integer.parseInt(value));
                }
            }
           // System.out.println(hashMap.size()+" file size");
            reader.close();
        }catch (IOException e){
            throw new Exception("Problems in reading Common.cfg file");
        }
        if(hashMap.size() != 6) {
            throw new RuntimeException("File argument number is wrong");
        }

        return hashMap;
    }
}
