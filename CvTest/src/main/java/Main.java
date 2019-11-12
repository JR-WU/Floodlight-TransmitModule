import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.logging.Logger;
import com.alibaba.fastjson.JSON;

public class Main {
    private Logger log = Logger.getLogger(Main.class.getName());
    private byte[] inBuff = new byte[Const.DATA_LEN];
    private DatagramPacket inPacket = new DatagramPacket(inBuff, inBuff.length);
    public void init() throws Exception{
        log.info("init...");
        try {
            DatagramSocket socket = new DatagramSocket(Const.STREAM_SERVER_PORT);
            while (true) {
                log.info("wait receive...");
                socket.receive(inPacket);
                String[] cameraArr = new String(inBuff,0, inPacket.getLength()).split("#");
//                System.out.println(Arrays.toString(cameraArr));
                int rtspPort = Const.RTSP_PORT;
                for (String cameraInfo : cameraArr) {
                    Camera camera = JSON.parseObject(cameraInfo, Camera.class);
                    System.out.println(camera.toString());

                    while(Utils.isLoclePortUsing(rtspPort)) {
                        rtspPort++;
                    }
                    new RTSPClient(new InetSocketAddress(camera.getIp(), camera.getPort()),
                            new InetSocketAddress(Const.STREAM_SERVER_IP, rtspPort), camera.getRtsp()).start();
                }
            }
        } catch (Exception e) {
            log.info(e.toString());
        }
    }

    public static void main(String[] args) throws Exception{
//        int start = 80;
//        int end = 1024;
//        for(int i=start;i<=end;i++){
//            System.out.println("查看"+i);
//            if(Utils.isLocalPortUsing(i)){
//                System.out.println("端口 "+i+" 已被使用");
//            }
//        }
//        System.out.println(Utils.isLoclePortUsing(29917));
        new Main().init();
    }
}
