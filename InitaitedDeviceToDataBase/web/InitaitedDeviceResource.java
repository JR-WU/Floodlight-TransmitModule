package net.floodlightcontroller.InitaitedDeviceToDataBase.web;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import net.floodlightcontroller.InitaitedDeviceToDataBase.IInitaitedDeviceService;
import net.floodlightcontroller.StreamingTransmit.IStreamingTransmitService;

public class InitaitedDeviceResource extends ServerResource {
	@Get("json")
	public String CollectTopolopy() {
		IInitaitedDeviceService service = (IInitaitedDeviceService) getContext().getAttributes().get(IInitaitedDeviceService.class.getCanonicalName());
		String result = service.gettopology();
		return result;
	}
}
