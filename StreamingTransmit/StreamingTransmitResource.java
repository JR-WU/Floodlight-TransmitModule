package net.floodlightcontroller.StreamingTransmit;

import java.io.IOException;

import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamingTransmitResource extends ServerResource{
	
	protected String IPSrc;
	protected String IPDst;
	protected Logger logger = LoggerFactory.getLogger(StreamingTransmitResource.class);//output logger
	
	//handle external post from others and phrase it.
	@Post
	public String handlePost(String fromexternal) throws IOException{
		
		
		return null;
		
	}

}
