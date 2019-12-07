import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class Main {
    private Logger log;
    private byte[] inBuff;
    private DatagramPacket inPacket;
    public Main() {
        log = Logger.getLogger(Main.class.getName());
        inBuff = new byte[Const.DATA_LEN];
        inPacket = new DatagramPacket(inBuff, inBuff.length);
    }
    public void init() throws Exception{
        log.info("init...");
        int rtspPort = Const.SOCKET_PORT;
        try {
            DatagramSocket socket = new DatagramSocket(Const.STREAM_SERVER_PORT);
            while (true) {
                log.info("wait receive...");
                socket.receive(inPacket);
                String strJson = new String(inBuff,0, inPacket.getLength());
                System.out.println("Recive Json: " + strJson);
                JSONObject jsonObject = JSON.parseObject(strJson);
                String cameraRTSP = jsonObject.getString("rtspAddr");
                String[] IPStr = Utils.getIPandPortFromRTSP(cameraRTSP);
                String cameraIP = IPStr[0];
                int cameraPort = Integer.parseInt(IPStr[1]);
                while(Utils.isLoclePortUsing(rtspPort)) {
                    rtspPort++;
                }
                new RTSPClient(new InetSocketAddress(cameraIP, cameraPort),
                        new InetSocketAddress(Const.STREAM_SERVER_IP, rtspPort++), cameraRTSP).start();
                Thread.sleep(1000);

            }
        } catch (Exception e) {
            log.info(e.toString());
        }
    }

    public static void main(String[] args) throws Exception{

        new Main().init();
    }
}
