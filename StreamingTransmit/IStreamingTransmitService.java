package net.floodlightcontroller.StreamingTransmit;
import java.util.List;

import net.floodlightcontroller.camera.Camera;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;

public interface IStreamingTransmitService extends IFloodlightService {
	public void StreamTransmitMain(IOFSwitchService switchService, IDevice deviceCamera, IDeviceService deviceManager, List<String> clientIPList);//Add Service to controller.
}