public class Camera {
    private int id;
    private int port;
    private String ip;
    private String username;
    private String passwd;
    private String rtspAddr;

    public Camera(int id, int port, String ip, String username, String passwd, String rtspAddr) {
        this.id = id;
        this.port = port;
        this.ip = ip;
        this.username = username;
        this.passwd = passwd;
        this.rtspAddr = rtspAddr;
    }

    public int getId() {
        return id;
    }

    public int getPort() {
        return port;
    }

    public String getIp() {
        return ip;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswd() {
        return passwd;
    }

    public String getRtspAddr() {
        return rtspAddr;
    }

    @Override
    public String toString() {
        return "Camera{" +
                "id=" + id +
                ", port=" + port +
                ", ip='" + ip + '\'' +
                ", username='" + username + '\'' +
                ", passwd='" + passwd + '\'' +
                ", rtspAddr='" + rtspAddr + '\'' +
                '}';
    }
}
