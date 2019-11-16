package net.floodlightcontroller.InitaitedDeviceToDataBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.VlanVid;
import org.restlet.data.Form;
import org.restlet.data.Status;

import net.floodlightcontroller.InitaitedDeviceToDataBase.web.InitaitedDeviceRoutable;
import net.floodlightcontroller.StreamingTransmit.IStreamingTransmitService;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.module.Run;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.devicemanager.IDeviceService.DeviceField;
import net.floodlightcontroller.devicemanager.internal.Device;
import net.floodlightcontroller.devicemanager.internal.DeviceIndex;
import net.floodlightcontroller.devicemanager.internal.DeviceManagerImpl;
import net.floodlightcontroller.devicemanager.web.AbstractDeviceResource;
import net.floodlightcontroller.linkdiscovery.Link;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.util.FilterIterator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import net.floodlightcontroller.threadpool.IThreadPoolService;

public class InitaitedDevice implements IFloodlightModule,IInitaitedDeviceService, Runnable{

	protected IFloodlightProviderService floodlightProvider;
	protected IDeviceService DeviceService; 
	protected ITopologyService TopologyService;
	protected IRestApiService restApiService;
    protected IThreadPoolService threadPoolService;
    public static final String MAC_ERROR = 
            "Invalid MAC address: must be a 48-bit quantity, " + 
            "expressed in hex as AA:BB:CC:DD:EE:FF";
    public static final String VLAN_ERROR = 
            "Invalid VLAN: must be an integer in the range 0-4095";
    public static final String IPV4_ERROR = 
            "Invalid IPv4 address: must be in dotted decimal format, " + 
            "234.0.59.1";
    public static final String IPV6_ERROR = 
            "Invalid IPv6 address: must be a valid IPv6 format.";
    public static final String DPID_ERROR = 
            "Invalid Switch DPID: must be a 64-bit quantity, expressed in " + 
            "hex as AA:BB:CC:DD:EE:FF:00:11";
    public static final String PORT_ERROR = 
            "Invalid Port: must be a positive integer";
    protected Map<EnumSet<DeviceField>, DeviceIndex> secondaryIndexMap;

// the following two methods are used to tell the module system that we provide some services.
// In this module, we only use internal service and won't let information out of it.
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
	    m.put(IInitaitedDeviceService.class, this);
	    return m;
	}
/******************************************************************************/
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
	    Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
	    l.add(IFloodlightProviderService.class);
	    l.add(IDeviceService.class);
	    l.add(ITopologyService.class);
	    l.add(IRestApiService.class);
	    l.add(IThreadPoolService.class);
	    return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		DeviceService = context.getServiceImpl(IDeviceService.class);
		TopologyService = context.getServiceImpl(ITopologyService.class);
	    restApiService = context.getServiceImpl(IRestApiService.class);
	    threadPoolService = context.getServiceImpl(IThreadPoolService.class);
	}
// module external initiazation.
	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		restApiService.addRestletRoutable(new InitaitedDeviceRoutable());
		startPeriodicReadDevicesAndSwitches(threadPoolService.getScheduledExecutor());
	}
/**************************************Main
 * @throws JsonProcessingException ******************************************/
	@Override
	public void run() {
		System.out.println("begin!");
		int counter = 1 ;
		Iterator<? extends IDevice> result;
//		result = getDevices();
        Collection<? extends IDevice> AllDevices;
        Map<DatapathId, Set<Link>> AllLinks;
        while(true) { 
        	System.out.println("begin! "+ counter);
        	try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}//10s
        	AllDevices = DeviceService.getAllDevices();
        	AllLinks = TopologyService.getAllLinks();
        	System.out.println(AllLinks);
        	counter++;
        }
	}
	public String gettopology(){
		System.out.println("begin!");        	
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//10s
        Collection<? extends IDevice> AllDevices;
        Map<DatapathId, Set<Link>> AllLinks;
        AllDevices = DeviceService.getAllDevices();
        AllLinks = TopologyService.getAllLinks();
        String a = AllDevices.toString() + AllLinks.toString();
        return a;
	}
	int portStatsInterval = 10;
	
    void setThreadPoolService(IThreadPoolService tp) {
        threadPoolService = tp;
    }
    IThreadPoolService getThreadPoolService() {
        return threadPoolService;
    }
    public ScheduledFuture<?> startPeriodicReadDevicesAndSwitches(ScheduledExecutorService ses)
    {
        ScheduledFuture<?> ReadTask =
            ses.scheduleAtFixedRate(
                this, portStatsInterval,
                portStatsInterval, TimeUnit.SECONDS);
        return ReadTask;
    }
/***************************************END*********************************************/
}
