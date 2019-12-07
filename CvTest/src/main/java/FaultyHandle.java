import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * @program: CvTest
 * @description: 摄像头发生故障，断开连接后的处理类
 * @author: sunwb
 * @create: 2019-12-07 18:51
 */
public class FaultyHandle extends Thread{

    private DatagramPacket outPacket = null;
    DatagramSocket socket;
    String msg;
    public FaultyHandle(String msg) {
        this.msg = msg;
        try {
            init();
            send();
            shutdown();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("init failed!!!");

        }

    }


    public void init() throws IOException {
        try {
            socket = new DatagramSocket();
            outPacket = new DatagramPacket(new byte[0], 0, InetAddress.getByName(Const.SWITCH_IP), Const.SWITCH_PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public boolean send() {
        outPacket.setData(msg.getBytes());
        try {
            socket.send(outPacket);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void shutdown() {
        socket.disconnect();
        socket.close();
    }

    public static void main(String[] args) {
        String cameraIP = "192.168.3.254";
        new FaultyHandle(cameraIP);
    }
}
