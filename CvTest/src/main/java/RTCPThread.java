import javax.rmi.CORBA.Util;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

//使用TCP 发送RTCP包的线程类
public class RTCPThread extends Thread{
    /** *//** 远程地址 */
    private final InetSocketAddress remoteAddress;

    /** *//** * 本地地址 */
    private final InetSocketAddress localAddress;
    /** *//** * 连接通道 */
    private SocketChannel socketChannel;
    /** *//** 发送缓冲区 */
    private final ByteBuffer sendBuf;
    private Selector selector;
    private boolean isSended;
    private static final int BUFFER_SIZE = 8192;
    public RTCPThread(InetSocketAddress remoteAddress,
                      InetSocketAddress localAddress) {
        this.remoteAddress = remoteAddress;
        this.localAddress = localAddress;
        // 初始化缓冲区
        sendBuf = ByteBuffer.allocateDirect(BUFFER_SIZE);
        if (selector == null) {
            // 创建新的Selector
            try {
                selector = Selector.open();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

        init();
        isSended=false;
    }

    /**
    * @Description 建立tcp连接
    * @param
    * @Return void
    * @Author sunwb
    * @Date 2019/11/14 20:33
    **/
    private void init() {
        System.out.println("RTCP Thread start...\n connecting camera...");

        try {
            // 打开通道
            socketChannel = SocketChannel.open();
            // 绑定到本地端口
            socketChannel.socket().setSoTimeout(30000);
            socketChannel.configureBlocking(false);
            socketChannel.socket().bind(localAddress);
            if (socketChannel.connect(remoteAddress)) {
                System.out.println("开始建立连接:" + remoteAddress);
            }
            socketChannel.register(selector, SelectionKey.OP_CONNECT
                    | SelectionKey.OP_READ | SelectionKey.OP_WRITE, this);
            System.out.println("端口打开成功");


        } catch (final IOException e1) {
            e1.printStackTrace();
        }
    }

    public void send(byte[] out) {
        if (out == null || out.length < 1) {
            return;
        }
        synchronized (sendBuf) {
            sendBuf.clear();
            sendBuf.put(out);
            sendBuf.flip();
        }

        // 发送出去
        try {
            write();
            isSended=true;
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public void write() throws IOException {
        if (isConnected()) {
            try {
                socketChannel.write(sendBuf);
            } catch (final IOException e) {
            }
        } else {
            System.out.println("通道为空或者没有连接上");
        }
    }


    public boolean isConnected() {
        return socketChannel != null && socketChannel.isConnected();
    }


    @Override
    public void run() {
        try {
            Thread.sleep(300);
            sendRTCP();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void sendRTCP() {
        String str = "80c9000140981deb81ca000640981deb010f4445534b544f502d354f324d425532000000";
        byte[] data = Utils.hexStr2Byte(str);
        send(data);
    }


}
