package net.floodlightcontroller.StreamingTransmit;

import java.io.IOException;
import net.floodlightcontroller.StreamingTransmit.IStreamingTransmitService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;

import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;

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
		}catch(IOException e) {
        	logger.error("Error Pharse json"); 
            return "{status:Error!!!!Could not parse this fucking json, see log for details.}";
		}
		
		IDevice source = deviceSearch(IPSrc);
		IDevice Dst = deviceSearch(IPDst);
		//maybe need some other attributes
		service.StreamTransmitMain(source, Dst, this.IPSrc, this.IPDst);
		return "1";
		
	}

	private void phrasejson(String fmjson) {
		// TODO Auto-generated method stub
		
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
