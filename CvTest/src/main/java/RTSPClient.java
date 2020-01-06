
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

//RTSP拉流客户端
public class RTSPClient extends Thread implements IEvent {

    private static final String VERSION = " RTSP/1.0\r\n";
    private static final String RTSP_OK = "RTSP/1.0 200 OK";
    private Logger log;
    /** *//** 远程地址 */
    private final InetSocketAddress remoteAddress;

    /** *//** * 本地地址 */
    private final InetSocketAddress localAddress;

    // 本线程所连接的摄像头IP
    String cameraIP;

    /** *//** * 用户名：密码 base64加密 */
    private final String base64Str;

    /** *//** * 连接通道 */
    private SocketChannel socketChannel;

    /** *//** 发送缓冲区 */
    private final ByteBuffer sendBuf;

    /** *//** 接收缓冲区 */
    private final ByteBuffer receiveBuf;


    private static final int BUFFER_SIZE = 8192;

    /** *//** 端口选择器 */
    private Selector selector;

    private String address;

    private Status sysStatus;

    private String sessionid;

    /** *//** 线程是否结束的标志 */
    private AtomicBoolean shutdown;

    private int seq=1;

    private boolean isSended;

    private String trackInfo;

    private int serverPort;  // 摄像头RTCP端口
    private boolean sendRtcpFlag = false;
    private boolean receiveError = false;  //接收信息错误
    private boolean connectionError = false;  //连接发生故障

    private enum Status {
        init, options, describe, setup, play, pause, teardown
    }

    public RTSPClient(InetSocketAddress remoteAddress,
                      InetSocketAddress localAddress, String address) {
        this.remoteAddress = remoteAddress;
        this.localAddress = localAddress;
        this.address = address;
        log = Logger.getLogger(RTSPClient.class.getName());
        this.cameraIP = remoteAddress.getAddress().toString().substring(1);
        String userPass = address.substring(7, address.indexOf("@"));
        this.base64Str = Utils.base64Encode(userPass);
//        log.info("userpass:" + userPass + "  base64:" + base64Str);
        // 初始化缓冲区  
        sendBuf = ByteBuffer.allocateDirect(BUFFER_SIZE);
        receiveBuf = ByteBuffer.allocateDirect(BUFFER_SIZE);
        init();
    }

    public void init() {
        if (selector == null) {
            // 创建新的Selector
            try {
                selector = Selector.open();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

        startup();
        sysStatus = Status.init;
        shutdown=new AtomicBoolean(false);
        isSended=false;

    }

    public void startup() {
        try {
            // 打开通道  
            socketChannel = SocketChannel.open();
            // 绑定到本地端口  
            socketChannel.socket().setSoTimeout(30000);
            socketChannel.configureBlocking(false);
            socketChannel.socket().bind(localAddress);
            if (socketChannel.connect(remoteAddress)) {
                log.info("开始建立连接:" + remoteAddress);
            }
            socketChannel.register(selector, SelectionKey.OP_CONNECT
                    | SelectionKey.OP_READ | SelectionKey.OP_WRITE, this);
            log.info("端口打开成功");


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
            log.info("通道为空或者没有连接上");
        }
    }

    public byte[] recieve() {
        if (isConnected()) {
            try {
                int len = 0;
                int readBytes = 0;

                synchronized (receiveBuf) {
                    receiveBuf.clear();
                    try {
                        while ((len = socketChannel.read(receiveBuf)) > 0) {
                            readBytes += len;
                        }
                    } finally {
                        receiveBuf.flip();
                    }
                    if (readBytes > 0) {
                        final byte[] tmp = new byte[readBytes];
                        receiveBuf.get(tmp);
                        return tmp;
                    } else {
                        log.info("接收到数据为空,重新启动连接");
                        return null;
                    }
                }
            } catch (final IOException e) {
                log.info("接收消息错误:");
            }
        } else {
            log.info("端口没有连接");
        }
        return null;
    }

    public boolean isConnected() {
        return socketChannel != null && socketChannel.isConnected();
    }

    private void select() {
        int n = 0;
        try {
            if (selector == null) {
                return;
            }
            n = selector.select(1000);

        } catch (final Exception e) {
            e.printStackTrace();
        }

        // 如果select返回大于0，处理事件  
        if (n > 0) {
            for (final Iterator<SelectionKey> i = selector.selectedKeys()
                    .iterator(); i.hasNext();) {
                // 得到下一个Key  
                final SelectionKey sk = i.next();
                i.remove();
                // 检查其是否还有效  
                if (!sk.isValid()) {
                    continue;
                }

                // 处理事件  
                final IEvent handler = (IEvent) sk.attachment();
                try {
                    if (sk.isConnectable()) {
                        handler.connect(sk);
                    } else if (sk.isReadable()) {
                        handler.read(sk);
                    } else {
                        // System.err.println("Ooops");  
                    }
                } catch (final Exception e) {
                    handler.error(e);
                    sk.cancel();
                }
            }
        }
    }

    public void shutdown() {
        log.info("shutdown...");
        if (isConnected()) {
            try {
                socketChannel.close();
                log.info("端口关闭成功");
            } catch (final IOException e) {
                log.info("端口关闭错误:");
            } finally {
                socketChannel = null;
            }
        } else {
            log.info("通道为空或者没有连接");
        }
    }

    @Override
    public void run() {
        // 启动主循环流程  
        while (!shutdown.get()) {

            try {
                if (isConnected()&&(!isSended)) {
                    switch (sysStatus) {
                        case init:
                            doOption();
                            break;
                        case options:
                            doDescribe();
                            break;
                        case describe:
                            doSetup();
                            break;
                        case setup:
                            if(sessionid==null&&sessionid.length()>0){
                                log.info("setup还没有正常返回");
                            }else{
                                doPlay();

                            }
                            break;
                        case play:
                            if (!sendRtcpFlag) {
                                sendRTCP();
                                sendRtcpFlag = true;
                            }
                            shutdown.set(true);
                            doPause();
                            break;

                        case pause:

                            doTeardown();
                            break;
                        default:
                            break;
                    }
                }
                // do select  
                select();
                try {
                    Thread.sleep(1000);
                } catch (final Exception e) {
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        log.info("--交互完毕，接收视频流");
        while (!connectionError) {
            try {
                Thread.sleep(1000);
                socketChannel.socket().sendUrgentData(0xFF);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
                connectionError = true;
                log.info("摄像头" + cameraIP + "连接中断。。。");
                new FaultyHandle(cameraIP); //故障上报给控制器
                 shutdown();
//                init();


            }

        }

    }

    public void connect(SelectionKey key) throws IOException {
        if (isConnected()) {
            return;
        }
        // 完成SocketChannel的连接  
        socketChannel.finishConnect();
        while (!socketChannel.isConnected()) {
            try {
                Thread.sleep(300);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
            socketChannel.finishConnect();
        }

    }

    public void error(Exception e) {
        e.printStackTrace();
    }

    public void read(SelectionKey key) throws IOException {
        // 接收消息  
        final byte[] msg = recieve();
        if (msg != null) {
            handle(msg);
        } else {
            key.cancel();
        }
    }

    private void handle(byte[] msg) {
        String tmp = new String(msg);
//        log.info("返回内容：");
//        log.info(tmp);
        if (tmp.startsWith(RTSP_OK)) {
            switch (sysStatus) {
                case init:
                    sysStatus = Status.options;
                    break;
                case options:
                    sysStatus = Status.describe;
                    trackInfo=tmp.substring(tmp.indexOf("trackID"));
                    break;
                case describe:
                    sessionid = tmp.substring(tmp.indexOf("Session: ") + 9, tmp
                            .indexOf("timeout")-1);
                    if(sessionid!=null&&sessionid.length()>0){
                        sysStatus = Status.setup;
                    }
                    this.serverPort = getServerPort(tmp);
                    break;
                case setup:
                    sysStatus = Status.play;
                    break;
                case play:
                    sysStatus = Status.pause;
                    shutdown.set(true);
                    break;
                case pause:
                    sysStatus = Status.teardown;

                    break;
                case teardown:
                    sysStatus = Status.init;
                    break;
                default:
                    break;
            }
            isSended=false;
        } else {
            receiveError = true;
            log.info("返回错误：" + tmp);
        }

    }

    private int getServerPort(String tmp) {
        String str = tmp.substring(tmp.indexOf("server_port")+12, tmp.indexOf("ssrc"));
        str = str.substring(str.indexOf("-") + 1, str.length() - 1);
//        log.info(str);
        return Integer.parseInt(str);
    }

    private void doTeardown() {
        StringBuilder sb = new StringBuilder();
        sb.append("TEARDOWN ");
        sb.append(this.address);
        sb.append("/");
        sb.append(VERSION);
        sb.append("Cseq: ");
        sb.append(seq++);
        sb.append("\r\n");
        sb.append("User-Agent: RealMedia Player HelixDNAClient/10.0.0.11279 (win32)\r\n");
        sb.append("Session: ");
        sb.append(sessionid);
        sb.append("\r\n");
        send(sb.toString().getBytes());
//        log.info(sb.toString());
    }

    private void doPlay() {
        StringBuilder sb = new StringBuilder();
        sb.append("PLAY ");
        sb.append(this.address);
        sb.append(VERSION);
        sb.append("Session: ");
        sb.append(sessionid);
        sb.append("Cseq: ");
        sb.append(seq++);
        sb.append("\r\n");
        sb.append("\r\n");
//        log.info(sb.toString());
        send(sb.toString().getBytes());

    }

    private void doSetup() {
        StringBuilder sb = new StringBuilder();
        sb.append("SETUP ");
        sb.append(this.address);
        sb.append("/");
        sb.append(trackInfo.substring(0,10));
        sb.append(VERSION);
        sb.append("Cseq: ");
        sb.append(seq++);
        sb.append("\r\n");
//        sb.append("Transport: RTP/AVP/TCP;UNICAST;mode=play\r\n");  //TCP.测试有问题
        sb.append("Transport: RTP/AVP;UNICAST;client_port=16264-16265\r\n");    // UDP 传输
        sb.append("\r\n");
//        log.info(sb.toString());
        send(sb.toString().getBytes());
    }

    private void doOption() {
        StringBuilder sb = new StringBuilder();
        sb.append("OPTIONS ");
        sb.append(this.address);
        sb.append(VERSION);
        sb.append("Cseq: ");
        sb.append(seq++);
        sb.append("\r\n");
        sb.append("Authorization: Basic " + base64Str + "\r\n");
        sb.append("\r\n");
//        log.info(sb.toString());
        send(sb.toString().getBytes());
    }

    private void doDescribe() {
        StringBuilder sb = new StringBuilder();
        sb.append("DESCRIBE ");
        sb.append(this.address);
        sb.append(VERSION);
        sb.append("Cseq: ");
        sb.append(seq++);
        sb.append("\r\n");
        sb.append("\r\n");
//        log.info(sb.toString());
        send(sb.toString().getBytes());
    }

    private void doPause() {
        StringBuilder sb = new StringBuilder();
        sb.append("PAUSE ");
        sb.append(this.address);
        sb.append("/");
        sb.append(VERSION);
        sb.append("Cseq: ");
        sb.append(seq++);
        sb.append("\r\n");
        sb.append("Session: ");
        sb.append(sessionid);
        sb.append("\r\n");
        send(sb.toString().getBytes());
//        log.info(sb.toString());
    }
    /**
    * @Description send RTCP packet to DH_camera;
    * @param
    * @Return void
    * @Author sunwb
    * @Date 2019/11/14 20:10
    **/
  private void sendRTCP() {
        new RTCPThread(cameraIP, serverPort).start();
  }

public static void main(String[] args) {
    try {
        // RTSPClient(InetSocketAddress remoteAddress,
        // InetSocketAddress localAddress, String address)
        RTSPClient client = new RTSPClient(
                new InetSocketAddress("113.0.0.102", 554),
                new InetSocketAddress("10.0.1.117", 12346),
                Const.HK_RTSP);
        client.start();
    } catch (Exception e) {
        e.printStackTrace();
    }
}
}  