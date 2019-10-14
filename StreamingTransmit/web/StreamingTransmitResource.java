package net.floodlightcontroller.StreamingTransmit.web;

import java.io.IOException;
import java.util.HashMap;

import net.floodlightcontroller.StreamingTransmit.IStreamingTransmitService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;

import org.projectfloodlight.openflow.types.DatapathId;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.sun.javafx.collections.MappingChange.Map;

public class StreamingTransmitResource extends ServerResource{
	
	Camera camera;   //camera information
	
	// the destination IP address and port
	String IPDst;
	int portDst;
	int camId; //stop action: not sure
	String action; //stop action: not sure
	protected Logger logger = LoggerFactory.getLogger(StreamingTransmitResource.class);//output logger
	
	//handle external post from others and phrase it.
	@Post
	public String handlePost(String fmjson) throws IOException{
		//fmjson is what we get yep!
		//and now we need to phrase fmjson and save it!
		IOFSwitchService switchService = (IOFSwitchService) getContext().getAttributes().get(IOFSwitchService.class.getCanonicalName());
		IStreamingTransmitService service = (IStreamingTransmitService) getContext().getAttributes().get(IStreamingTransmitService.class.getCanonicalName());
		int res = 0;
		try {
			res = phrasejson(fmjson);
		}catch(Exception e) {
			System.out.println(e);
        	logger.error("Error Pharse json"); 
            return ("{\"status\" : -1 , \"msg\":\"Could not parse json!!!\"}" + e.toString());
		}
		
		if (res == 0) { //connection request
			System.out.println("connecting to play...");
			IDevice deviceCamera = deviceSearch(camera.getCameraIp());
			IDevice Terminal = deviceSearch(IPDst);
			DatapathId dpid = findDpidByDeviceIp(deviceCamera);
			System.out.println("camera switch dpid： " + dpid);
			IOFSwitch sw = switchService.getSwitch(dpid);
			camera.setSwitch(sw);
			System.out.println("camera information: " + camera.toString());
			service.StreamTransmitMain(switchService, deviceCamera, Terminal, camera, IPDst, portDst);
			return ("{\"status\" : 0 , \"msg\":\"Get your Post! Now transfer viedo Streaming! \"}");
		} else { //stop request
			
			return ("{\"status\" : 0 , \"msg\":\"Get your Post! Now stop viedo Streaming! \"}");
		}
		
	}
	//return: 0: connect; 1: stop
	@SuppressWarnings("deprecation")
	private int phrasejson(String fmjson) throws IOException {
		// TODO Auto-generated method stub
		MappingJsonFactory f = new MappingJsonFactory();
		JsonParser jp;
		int cameraId = -1;
		String cameraIp = null;
		int cameraPort = -1;
		String userName = null;
		String passwd = null;
		String rtstAddr = null;
		
        try {
            jp = f.createJsonParser(fmjson);
        } catch (JsonParseException e) {
            throw new IOException(e);
        }
        
        jp.nextToken();
        if (jp.getText() != "{") {
        	
            throw new IOException("Expected START_ARRAY");
        }
        jp.nextToken();
        if (jp.getText() == "camera")
        {
        	jp.nextToken();
            if (jp.getText() != "{") {
            	
                throw new IOException("Expected START_ARRAY");
            }
            
        	for (int i = 0; i < 6; i++) {
        		jp.nextToken();
	        	String attr = jp.getText();
	        	switch(attr) {
	        	case "id": jp.nextToken(); cameraId = jp.getIntValue(); break;
	        	case "ip": jp.nextToken(); cameraIp = jp.getText(); break;
	        	case "port": jp.nextToken(); cameraPort = jp.getIntValue(); break;
	        	case "username": jp.nextToken(); userName = jp.getText(); break;
	        	case "passwd": jp.nextToken(); passwd = jp.getText(); break;
	        	case "rtstAddr": jp.nextToken(); rtstAddr = jp.getText(); jp.nextToken(); break;
	        	default: throw new IOException("error feild!");
	        	} 
	        	
        	}
        	this.camera = new Camera(cameraId, cameraIp, cameraPort, userName, passwd,rtstAddr);
        	jp.nextToken();
            if (jp.getText() == "dest")
            {
            	jp.nextToken();
                if (jp.getText() != "{") {
                	
                    throw new IOException("Expected START_ARRAY");
                }
            	jp.nextToken();
            	if (jp.getText() == "ip") {
            		jp.nextToken();
            		IPDst= jp.getText();
            	} else throw new IOException("Expected IPDst");
            	jp.nextToken();
            	if (jp.getText() == "port") {
            		jp.nextToken();
            		portDst = jp.getIntValue();
            	} else throw new IOException("Expected portDst");
            	
            }else throw new IOException("Expected IPDst");
            return 0; //connection request
        }else if (jp.getText() == "action"){
        	jp.nextToken();
        	action = jp.getText();
        	jp.nextToken();
        	camId = jp.getIntValue();
        	
        	return 1; //stop
        } else throw new IOException("Unknow json!");
	}
	
	//search correspond device according to the Phrased IP
    private IDevice deviceSearch(String IP){
    	IDeviceService deviceManager = 
                (IDeviceService)getContext().getAttributes().
                    get(IDeviceService.class.getCanonicalName());
    	for (IDevice D : deviceManager.getAllDevices())
        {
        	if(D.toString().contains(IP))
        	{
        		return D;
        	}
    }
    	return null;
    }
    private DatapathId findDpidByDeviceIp(IDevice device) {
		SwitchPort [] switchPort = device.getAttachmentPoints();
		DatapathId dpid = switchPort[0].getNodeId();
		return dpid;
	}

}