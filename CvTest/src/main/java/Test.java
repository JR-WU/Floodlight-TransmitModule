import sun.misc.BASE64Encoder;

import java.net.InetSocketAddress;

//用来测试一些api
public class Test {
    static String username = "admin:admin";

    public static void main(String[] args) {
        InetSocketAddress socketAddress = new InetSocketAddress(Const.STREAM_SERVER_IP, 5);
        System.out.println(socketAddress.getAddress().toString().substring(1));
    }
}
