package net.floodlightcontroller.StreamingTransmit.web;

import java.io.IOException;
import net.floodlightcontroller.StreamingTransmit.IStreamingTransmitService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;

import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;

public class StreamingTransmitResource extends ServerResource{
	
	public String IPSrc;
	public String IPDst;
	protected Logger logger = LoggerFactory.getLogger(StreamingTransmitResource.class);//output logger
	
	//handle external post from others and phrase it.
	@Post
	public String handlePost(String fmjson) throws IOException{
		//fmjson is what we get yep!
		//and now we need to phrase fmjson and save it!
		IStreamingTransmitService service = (IStreamingTransmitService) getContext().getAttributes().get(IStreamingTransmitService.class.getCanonicalName());
		try {
			phrasejson(fmjson);
		}catch(Exception e) {
        	logger.error("Error Pharse json"); 
            return "{status:Error!!!!Could not parse this fucking json, see log for details.}";
		}
		
		//maybe need some other attributes
		IDevice Cam = deviceSearch(IPSrc);
		IDevice Terminal = deviceSearch(IPDst);
		service.StreamTransmitMain(Cam,Terminal,IPSrc, IPDst);
		return ("{\"status\" : Get your Post! Now transfer viedo Streaming! \"}");
		
	}
	//because we don't know the message format sent by them, so we make an easy one to test function.
	@SuppressWarnings("deprecation")
	private void phrasejson(String fmjson) throws IOException {
		// TODO Auto-generated method stub
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
        System.out.println(jp.getText());
        if (jp.getText() == "IPSrc")
        {
        	jp.nextToken();
        	IPSrc = jp.getText();
        }else {throw new IOException("Expected IPDst");}
        jp.nextToken();
        if (jp.getText() == "IPDst")
        {
        	jp.nextToken();
        	IPDst= jp.getText();
        }else throw new IOException("Expected IPDst");
        return;
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

}
