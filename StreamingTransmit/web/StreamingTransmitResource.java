package net.floodlightcontroller.StreamingTransmit.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

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
import net.floodlightcontroller.camera.Camera;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.sun.javafx.collections.MappingChange.Map;

public class StreamingTransmitResource extends ServerResource{
	
	
	String cameraIP;
	List<String> clientIPList = new LinkedList<>();
	

	protected Logger logger = LoggerFactory.getLogger(StreamingTransmitResource.class);//output logger
	
	//handle external post from others and phrase it.
	@Post
	public String handlePost(String fmjson){
		//fmjson is what we get yep!
		//and now we need to phrase fmjson and save it!
		IOFSwitchService switchService = (IOFSwitchService) getContext().getAttributes().get(IOFSwitchService.class.getCanonicalName());
		IStreamingTransmitService service = (IStreamingTransmitService) getContext().getAttributes().get(IStreamingTransmitService.class.getCanonicalName());
		IDeviceService deviceManager = (IDeviceService)getContext().getAttributes().get(IDeviceService.class.getCanonicalName());
		int res;
		System.out.println("xxx"+fmjson);
		try {
			res = phrasejson(fmjson);
		
		
			
//				else { //stop request
//				
//				return ("{\"code\" : -1 , \"msg\":\"input error\",\"data\":null}");
//			}
		} catch (IOException e) {
			return ("{\"code\" : -1 , \"msg\":" + e.toString() + ",\"data\":null}");
		}
		if (res == 0) { //connection request
			System.out.println("connecting to play...");
			IDevice deviceCamera = deviceSearch(cameraIP);
			service.StreamTransmitMain(switchService, deviceCamera, deviceManager, clientIPList);
			
			return ("{\"code\" : 0 , \"msg\":\"success\",\"data\":null}");
		} else { 
			return ("{\"code\" : -1 , \"msg\":\"input error\",\"data\":null}");
		}
	}
	
	@SuppressWarnings("deprecation")
	private int phrasejson(String fmjson) throws IOException{
		MappingJsonFactory f = new MappingJsonFactory();
		JsonParser jp;
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
        if (jp.getText() == "IP_list") {
        	jp.nextToken();
        	String ipList = jp.getText();
			String[] strArr = ipList.split(","); 
			cameraIP = strArr[0];
//			System.out.println("cameraIP:" + cameraIP);
			for (int i = 1; i < strArr.length; i++) {
				clientIPList.add(strArr[i]);
//				System.out.println("clientIP:" + strArr[i]);
			}
        }
		if (cameraIP != null && clientIPList.size() > 0) return 0;
		else return -1;
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