import java.io.*;
import java.net.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;


public class Client extends Thread{

    byte[] inBuff = new byte[Const.DATA_LEN];
    private DatagramPacket outPacket = null;
    DatagramSocket socket;
    String msg;
    public Client(String msg) {
        this.msg = msg;
        try {
            init();
            send();
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
        int i = 20;
        try {
            socket.send(outPacket);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        String cameraIP = "192.168.3.254";
        System.out.println("send " + new Client(cameraIP));
    }
}  