import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

//使用UDP发送RTCP包的线程类
public class RTCPThread extends Thread{
    private String cameraIP;
    private int cameraPort;
    private DatagramPacket outPacket = null;
    DatagramSocket socket;

    // rtcp packet，来自于wireshark抓包
    String str = "80c9000140981deb81ca000640981deb010f4445534b544f502d354f324d425532000000";

    byte[] data = Utils.hexStr2Byte(str);
    public RTCPThread(String ip, int port){
        this.cameraIP = ip;
        this.cameraPort = port;
        System.out.println(Thread.currentThread().getName() + "-----" + cameraIP + ":" + cameraPort);
        init();
    }

    public void init(){
        try {
            socket = new DatagramSocket();   //本地端口号
            outPacket = new DatagramPacket(data, data.length,InetAddress.getByName(cameraIP), cameraPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println("start RTCP thread！");
        while (true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                socket.send(outPacket);
            } catch (IOException e) {
                System.out.println("outPacket 发送失败！");
                e.printStackTrace();
            }

        }
    }

    public static void main(String[] args) {
        new RTCPThread
                (Const.STREAM_SERVER_IP, 12345).start();
    }
}
