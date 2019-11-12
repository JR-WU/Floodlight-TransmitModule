package net.floodlightcontroller.flowDownstream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.projectfloodlight.openflow.protocol.OFBucket;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFGroupType;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.match.Match.Builder;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmIpv4Dst;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFGroup;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.CONST.Const;
import net.floodlightcontroller.camera.Camera;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IListener.Command;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.types.NodePortTuple;
import net.floodlightcontroller.core.util.AppCookie;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.flowDownstream.web.FlowDownstreamWebRoutable;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.routing.Path;
import net.floodlightcontroller.routing.RoutingManager;
import net.floodlightcontroller.util.FlowModUtils;
import utils.Utils;

public class FlowDownstream implements IOFMessageListener, IFloodlightModule, IFlowDownstreamService{

	protected IFloodlightProviderService floodlightProvider;
	protected Set<Long> macAddresses;
	protected static Logger logger;
	protected IRestApiService restApi = null;
	private byte payload[];
	private RoutingManager routingmanager;
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return FlowDownstream.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
	    m.put(IFlowDownstreamService.class, this);
	    return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		// TODO Auto-generated method stub
		Collection<Class<? extends IFloodlightService>> l =
	            new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IRestApiService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		this.restApi = context.getServiceImpl(IRestApiService.class);
	    floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
	    macAddresses = new ConcurrentSkipListSet<Long>();
	    logger = LoggerFactory.getLogger(FlowDownstream.class);
	    this.routingmanager = new RoutingManager();
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		if (restApi != null) restApi.addRestletRoutable(new FlowDownstreamWebRoutable());

	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void pullStream(IOFSwitchService switchService, IDevice streamServer, String cameraList) {
		IOFSwitch sw = switchService.getSwitch(Utils.findDpidByDevice(streamServer));
	
		byte[] data = cameraList.getBytes();
		UDPcreator(sw, streamServer, data); 
	}
	
	//TCP Construction Method,
	//@IPSr: Source IP 
	//@IPDs: Destination IP 
	//@Data: Payload Data
	private void UDPcreator(IOFSwitch sw,IDevice streamServer,byte Data[]){
//		System.out.println("statr udpcreator..");
		OFPort serverPort = Utils.findPortByDevice(streamServer);
		
		Ethernet l2 = new Ethernet();
		l2.setSourceMACAddress(MacAddress.of("00:00:00:00:00:01"));//Not Sure.
		l2.setDestinationMACAddress(streamServer.getMACAddress());//Not sure.
		l2.setEtherType(EthType.IPv4);
		
		IPv4 l3 = new IPv4();
		l3.setSourceAddress("192.168.3.1");
		l3.setDestinationAddress(Const.STREAM_SERVER_IP);
		l3.setTtl((byte) 64);//Time to live;
		l3.setProtocol(IpProtocol.UDP);
		
		UDP l4 = new UDP();
		l4.setSourcePort(TransportPort.of(53001));
		l4.setDestinationPort(TransportPort.of(Const.STREAM_SERVER_PORT));  //streamServer port：
		
		Data l7 = new Data();
		/*this line will add data in TCP packet*/
		l7.setData(Data);
		l2.setPayload(l3);
		l3.setPayload(l4);
		l4.setPayload(l7);
		byte[] serializedData = l2.serialize();
		OFPacketOut po = sw.getOFFactory().buildPacketOut() 
			    .setData(serializedData)
			    .setActions(Collections.singletonList((OFAction) sw.getOFFactory().actions().output(serverPort,Integer.MAX_VALUE)))//this will change.
			    .setInPort(OFPort.CONTROLLER)
			    .build();
			  
		sw.write(po);		
	}

}
