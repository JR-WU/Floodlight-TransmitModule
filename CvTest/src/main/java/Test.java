import sun.misc.BASE64Encoder;

import java.net.InetSocketAddress;

//用来测试一些api
public class Test {
    static String username = "admin:admin";

    public static void main(String[] args) {
        String ss = "rtsp://admin:admin@192.168.3.253:554/Streaming/Channels";
        String[] str = Utils.getIPandPortFromRTSP(ss);
        System.out.println(str[0] + "\n" + str[1]);
    }
}
