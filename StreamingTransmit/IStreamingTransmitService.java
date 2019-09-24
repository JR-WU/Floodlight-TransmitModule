package net.floodlightcontroller.StreamingTransmit;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;

public interface IStreamingTransmitService extends IFloodlightService {
	public void StreamTransmitMain(IDevice source, IDevice dest, String IPSrc, String IPDst);//Add Service to controller.
}
