package net.floodlightcontroller.StreamingTransmit;

import org.restlet.resource.ServerResource;

import java.io.IOException;

import org.restlet.resource.Post;

public class StreamingTransmitResource extends ServerResource{
	
	protected String IPSrc;
	protected String IPDst;
	
	//handle external post from others and phrase it.
	@Post
	public String handlePost(String fromexternal) throws IOException{
		
		return null;
		
	}

}
