import sun.misc.BASE64Encoder;

import java.net.InetSocketAddress;
import java.util.Arrays;

//用来测试一些api
public class Test {
    static String username = "admin:admin";

    public static void main(String[] args) {
        String str = "10.0.0";
        String []stt = str.split(".");
        System.out.println(stt[0]);
    }
}
