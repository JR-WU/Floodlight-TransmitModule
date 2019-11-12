package utils;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;

import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;

public class Utils {
    public static IDevice deviceSearch(IDeviceService deviceManager, String IP){
    	
    	for (IDevice D : deviceManager.getAllDevices())
        {
//    		System.out.println("device: " + D.toString());
        	if(D.toString().contains(IP))
        	{
        		return D;
        	}
    }
    	return null;
    }
    public static DatapathId findDpidByDeviceIp(IDevice device) {
		SwitchPort [] switchPort = device.getAttachmentPoints();
		DatapathId dpid = switchPort[0].getNodeId();
		return dpid;
	}

    public static DatapathId findDpidByDevice(IDevice device) {
		SwitchPort [] switchPort = device.getAttachmentPoints();
		DatapathId dpid = switchPort[0].getNodeId();
		return dpid;
	}
	
	public static OFPort findPortByDevice(IDevice device) {
		SwitchPort [] switchPort = device.getAttachmentPoints();
		OFPort port = switchPort[0].getPortId();
		return port;
	}
}
