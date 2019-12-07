package net.floodlightcontroller.flowDownstream.web;

import java.io.IOException;
import java.util.HashMap;

import net.floodlightcontroller.CONST.Const;
import net.floodlightcontroller.StreamingTransmit.IStreamingTransmitService;
import net.floodlightcontroller.camera.Camera;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.flowDownstream.IFlowDownstreamService;
import utils.Utils;

import org.projectfloodlight.openflow.types.DatapathId;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.sun.javafx.collections.MappingChange.Map;

public class FlowDownstreamResource extends ServerResource{

	protected Logger logger = LoggerFactory.getLogger(FlowDownstreamResource.class);//output logger
	
	@Post
	public String handlePost(String inputStr){
		//fmjson is what we get yep!
		//and now we need to phrase fmjson and save it!
		IOFSwitchService switchService = (IOFSwitchService) getContext().getAttributes().get(IOFSwitchService.class.getCanonicalName());
		IFlowDownstreamService service = (IFlowDownstreamService) getContext().getAttributes().get(IFlowDownstreamService.class.getCanonicalName());
		IDeviceService deviceManager = 
                (IDeviceService)getContext().getAttributes().
                    get(IDeviceService.class.getCanonicalName());
		logger.info("connected stream server...");
		IDevice streamServer = Utils.deviceSearch(deviceManager, Const.STREAM_SERVER_IP);
		if (streamServer == null) {
			System.out.println("streamServer is null");
			return "fail";
		}
		service.pullStream(switchService, streamServer, inputStr);
		logger.info("pull stream success!");
		return "pull stream success!";
	}
	
	

}