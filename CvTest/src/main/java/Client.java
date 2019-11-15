import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;


// socket通信---客户端测试类
public class Client extends Thread{

    private final InetSocketAddress remoteAddress;

    /** *//** * 本地地址 */
    private final InetSocketAddress localAddress;

    public Client(InetSocketAddress remoteAddress,
                InetSocketAddress localAddress, String address) throws InterruptedException {
            this.remoteAddress = remoteAddress;
            this.localAddress = localAddress;
            System.out.println(remoteAddress);
            System.out.println(localAddress);
        try {
            System.out.println("start connect...");
            Socket socket = new Socket("localhost", 8888);
            OutputStream outputStream = socket.getOutputStream();
            PrintWriter printWriter = new PrintWriter(outputStream);
            printWriter.print("hello, this is client.");
            printWriter.flush();
            socket.shutdownOutput();

            InputStream inputStream = socket.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String temp = null;
            String info = "";
            while ((temp = bufferedReader.readLine()) != null) {
                info += temp;
                System.out.println("server message: " + info);
            }

            bufferedReader.close();
            inputStream.close();
            printWriter.close();
            outputStream.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        System.out.println("thread run..");
    }



    public static void main(String[] args) {
        try {
            // RTSPClient(InetSocketAddress remoteAddress,  
            // InetSocketAddress localAddress, String address)  
            Client client = new Client(
                    new InetSocketAddress("192.168.3.236", 554),
                    new InetSocketAddress("192.168.3.102", 12345),
                    "rtsp://admin:admin123@192.168.3.254:554/Streaming/Channels/101?transportmode=unicast&profile=Profile_1");
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}  