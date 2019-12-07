package net.floodlightcontroller.flowDownstream;

import net.floodlightcontroller.camera.Camera;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;

public interface IFlowDownstreamService extends IFloodlightService {
	public void pullStream(IOFSwitchService switchService, IDevice streamServer, String inputStr);
}