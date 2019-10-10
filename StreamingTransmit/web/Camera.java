package net.floodlightcontroller.StreamingTransmit.web;

import net.floodlightcontroller.core.IOFSwitch;

public class Camera {
	private int cameraId;
	private String cameraIp;
	private int cameraPort;
	private String userName;
	private String passwd;
	private String rtstAddr;
	private IOFSwitch sw;
	
	public Camera(int cameraId, String cameraIp, int cameraPort, String userName, String passwd,
			String rtstAddr) {
		this.cameraId = cameraId;
		this.cameraIp = cameraIp;
		this.cameraPort = cameraPort;
		this.userName = userName;
		this.passwd = passwd;
		this.rtstAddr = rtstAddr;
	}
	public int getCameraId() {
		return cameraId;
	}
	public void setCameraId(int cameraId) {
		this.cameraId = cameraId;
	}
	public String getCameraIp() {
		return cameraIp;
	}
	public void setCameraIp(String cameraIp) {
		this.cameraIp = cameraIp;
	}
	public int getCameraPort() {
		return cameraPort;
	}
	public void setCameraPort(int cameraPort) {
		this.cameraPort = cameraPort;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPasswd() {
		return passwd;
	}
	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}
	public String getRtstAddr() {
		return rtstAddr;
	}
	public void setRtstAddr(String rtstAddr) {
		this.rtstAddr = rtstAddr;
	}
	public IOFSwitch getSwitch() {
		return sw;
	}
	public void setSwitch(IOFSwitch sw) {
		this.sw = sw;
	}
	@Override
	public String toString() {
		return "Camera [cameraId=" + cameraId + ", cameraIp=" + cameraIp + ", cameraPort=" + cameraPort + ", userName="
				+ userName + ", passwd=" + passwd + ", rtstAddr=" + rtstAddr + ", sw=" + sw + "]";
	}
	
	

}
