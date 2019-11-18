import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import com.alibaba.fastjson.JSON;

/*test example
curl http://localhost:8080/wm/flowdownstream/Trans -X POST -d 
'0rtsp://admin:admin@192.168.3.253:554/cam/realmonitor?channel=1&subtype=0&unicast=true' 

curl http://localhost:8080/wm/flowdownstream/Trans -X POST -d 
'0rtsp://admin:admin123@192.168.3.254:554/Streaming/Channels/101?transportmode=unicast&profile=profile_1'

*/
//主线程，根据packet_out消息进行拉流操作
public class Main {
    private Logger log = Logger.getLogger(Main.class.getName());
    private byte[] inBuff = new byte[Const.DATA_LEN];
    private DatagramPacket inPacket = new DatagramPacket(inBuff, inBuff.length);
    public void init() throws Exception{
        log.info("init...");
        int rtspPort = Const.SOCKET_PORT;
        try {
            DatagramSocket socket = new DatagramSocket(Const.STREAM_SERVER_PORT);
            while (true) {
                log.info("wait receive...");
                socket.receive(inPacket);
                String recStr = new String(inBuff,0, inPacket.getLength());
                System.out.println("Recive string: " + recStr);
                char rtspFlag = recStr.charAt(0);
                if (rtspFlag == '0') {
                    String cameraRTSP = recStr.substring(1);

                    String[] IPStr = Utils.getIPandPortFromRTSP(cameraRTSP);
                    String cameraIP = IPStr[0];
                    int cameraPort = Integer.parseInt(IPStr[1]);
                    while(Utils.isLoclePortUsing(rtspPort)) {
                        rtspPort++;
                    }
                    new RTSPClient(new InetSocketAddress(cameraIP, cameraPort),
                            new InetSocketAddress(Const.STREAM_SERVER_IP, rtspPort++), cameraRTSP).start();
                    Thread.sleep(500);
                } else {

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
