import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

/**
 * @program: CvTest
 * @description: delete RTP header
 * @author: sunwb
 * @create: 2019-12-07 14:15
 */
public class RTPToUDP extends Thread{
    private Logger log = Logger.getLogger(RTPToUDP.class.getName());
    private byte[] inBuff = new byte[Const.DATA_LEN];
    private DatagramPacket inPacket = new DatagramPacket(inBuff, inBuff.length);
    public void init() throws Exception{
        log.info("init...");
        int rtspPort = Const.SOCKET_PORT;
        try {
            DatagramSocket socket = new DatagramSocket(Const.RTP_PORT);  //监听16264端口的RTP数据
            log.info("wait receive RTP packets...");
            while (true) {

                socket.receive(inPacket);
                String str = new String(inBuff,0, inPacket.getLength());

                Thread.sleep(300);

            }
        } catch (Exception e) {
            log.info(e.toString());
        }
    }

    public static void main(String[] args) throws Exception{

        new Main().init();
    }
}
