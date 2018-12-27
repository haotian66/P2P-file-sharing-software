/*
* Yuzhuang Chen: UF# 9194-9461
* Haotian Jiang: UF# 6736-6421
* Qiao Xue: UF# 1517-5652
* */

package exception.logger;


public class LogException extends Exception {
    public LogException (String msg, Throwable cause){
        super(msg, cause);
    }

}
