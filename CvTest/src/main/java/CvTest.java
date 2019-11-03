import javax.swing.*;
import java.io.*;
import java.net.Socket;


public class CvTest {
    private String VERSION = "RTSP/1.0";
    private String address;
    private int seq;
    public CvTest(String address) {
        this.address = address;
        this.seq = 1;
    }
    private void doOption() {
        StringBuilder sb = new StringBuilder();
        sb.append("OPTIONS ");
        sb.append(address.substring(0, address.lastIndexOf("/")));
        sb.append(VERSION);
        sb.append("\r\n");
        sb.append("CSeq: ");
        sb.append(seq++);
        sb.append("\r\n");
        sb.append("\r\n");
        System.out.println(sb.toString());
//        send(sb.toString().getBytes());
    }

    private void doDescribe() {
        StringBuilder sb = new StringBuilder();
        sb.append("DESCRIBE ");
        sb.append(this.address);
        sb.append(VERSION);
        sb.append("\r\n");
        sb.append("CSeq: ");
        sb.append(seq++);
        sb.append("\r\n");
        sb.append("\r\n");
        System.out.println(sb.toString());
//        send(sb.toString().getBytes());
    }

    private void doSetup() {
        StringBuilder sb = new StringBuilder();
        sb.append("SETUP ");
        sb.append(this.address);
        sb.append("/");
        sb.append("trackInfo"); //trackInfo
        sb.append(VERSION);
        sb.append("CSeq: ");
        sb.append(seq++);
        sb.append("\r\n");
        sb.append("Transport: RTP/AVP;UNICAST;client_port=16264-16265;mode=play");
        sb.append("\r\n");
        sb.append("\r\n");
        System.out.println(sb.toString());
//        send(sb.toString().getBytes());
    }

    private void doPlay() {
        StringBuilder sb = new StringBuilder();
        sb.append("PLAY ");
        sb.append(this.address);
        sb.append(VERSION);
        sb.append("Session: ");
        sb.append(0); //session id
        sb.append("CSeq: ");
        sb.append(seq++);
        sb.append("\r\n");
        sb.append("\r\n");
        System.out.println(sb.toString());
//        send(sb.toString().getBytes());

    }

    public static void send(String rtspCmd) throws IOException {
        String ip = "10.0.0.103";
        int port = 80;
        try {
            System.out.println("start:");
            Socket socket = new Socket(ip, port);
            System.out.println("socket:" + socket.isConnected());
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF(rtspCmd);
            System.out.println("write finish: " + rtspCmd);
//            Byte recv = input.readByte();
//            System.out.println("recive: " + recv);
            out.close();
            input.close();
            System.out.println("close?" + socket.isConnected());
        } catch (IOException e) {
            System.out.println("客户端异常:" + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException{

        String rtspCmd = "rtsp://admin:admin123@10.0.0.103:554/Streaming/Channels/101?transportmode=unicast&profile=Profile_1";
        send(rtspCmd);
        System.out.println("end");
    }
}
