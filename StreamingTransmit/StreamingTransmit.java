package net.floodlightcontroller.StreamingTransmit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ConcurrentSkipListSet;

import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TransportPort;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.StreamingTransmit.web.StreamingTransmitWebRoutable;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.restserver.IRestApiService;

public class StreamingTransmit implements IOFMessageListener, IFloodlightModule, IStreamingTransmitService{


	protected IFloodlightProviderService floodlightProvider;
	protected Set<Long> macAddresses;
	protected static Logger logger;
	protected IRestApiService restApi = null;
	protected String TCPPacket;
	private byte payload[];
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return StreamingTransmit.class.getSimpleName();
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
	    m.put(IStreamingTransmitService.class, this);
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
	    logger = LoggerFactory.getLogger(StreamingTransmit.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		if (restApi != null) restApi.addRestletRoutable(new StreamingTransmitWebRoutable());
	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		// TODO Auto-generated method stub
		return null;
	}
	//in this method, we will implement the TCP connection and flow delivery.
	@Override
	public void StreamTransmitMain(IDevice deviceSrc,IDevice deviceDst,String IPS, String IPD) {
		/*  Only for testing~
			System.out.println("the IPSrc is"+ IPS);
			System.out.println("the IPDst is"+ IPD);
		*/
		System.out.println("the IPSrc is "+ IPS);
		System.out.println("the IPDst is "+ IPD);
		//First,Create a TCP packet for next step;
		//Testing how to find the switch connected to Devices.			
		System.out.println("the Source Device is: " + deviceSrc);
		System.out.println("the Destination Device is: " + deviceDst);
	}
	//TCP Construction Method,
	//@IPSr: Source IP 
	//@IPDs: Destination IP 
	//@Data: Payload Data
	private String TCPcreator(IOFSwitch sw,String IPSr,String IPDs,byte Data[]){
		
		Ethernet l2 = new Ethernet();
		l2.setSourceMACAddress(MacAddress.of("00:00:00:00:00:01"));//Not Sure.
		l2.setDestinationMACAddress(MacAddress.BROADCAST);//Not sure.
		l2.setEtherType(EthType.IPv4);
		IPv4 l3 = new IPv4();
		l3.setSourceAddress(IPSr);
		l3.setDestinationAddress(IPDs);
		l3.setTtl((byte) 64);//Time to live;
		l3.setProtocol(IpProtocol.TCP);
		TCP l4 = new TCP();
		l4.setSourcePort(TransportPort.of(65003));//Not Sure
		l4.setDestinationPort(TransportPort.of(67));//Not Sure
		Data l7 = new Data();
		/*this line will add data in TCP packet*/
		l7.setData(Data);
		l2.setPayload(l3);
		l3.setPayload(l4);
		l4.setPayload(l7);
		byte[] serializedData = l2.serialize();
		
		OFPacketOut po = sw.getOFFactory().buildPacketOut() 
			    .setData(serializedData)
			    .setActions(Collections.singletonList((OFAction) sw.getOFFactory().actions().output(OFPort.FLOOD, 0xffFFffFF)))//this will change.
			    .setInPort(OFPort.CONTROLLER)
			    .build();
			  
			sw.write(po);
		
		
		return null;
	}
	
}
