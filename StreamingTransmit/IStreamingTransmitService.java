package net.floodlightcontroller.StreamingTransmit;

import net.floodlightcontroller.StreamingTransmit.web.Camera;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;

public interface IStreamingTransmitService extends IFloodlightService {
	public void StreamTransmitMain(IDevice deviceCamera,IDevice deviceClient,Camera camera, String IPDst, int portDst);//Add Service to controller.
}