package net.floodlightcontroller.StreamingTransmit;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ConcurrentSkipListSet;

import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;

import org.projectfloodlight.openflow.protocol.OFBucket;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFGroupType;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmIpv4Dst;
import org.projectfloodlight.openflow.protocol.match.Match.Builder;
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

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.StreamingTransmit.web.Camera;
import net.floodlightcontroller.StreamingTransmit.web.StreamingTransmitWebRoutable;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.types.NodePortTuple;
import net.floodlightcontroller.core.util.AppCookie;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.routing.Path;
import net.floodlightcontroller.routing.RoutingManager;
import net.floodlightcontroller.util.FlowModUtils;

public class StreamingTransmit implements IOFMessageListener, IFloodlightModule, IStreamingTransmitService{


	protected IFloodlightProviderService floodlightProvider;
	protected Set<Long> macAddresses;
	protected static Logger logger;
	protected IRestApiService restApi = null;
	protected String TCPPacket;
	private byte payload[];
	private DatapathId dpidSrc;
	private DatapathId dpidDst;
	private OFPort portSrc;
	private OFPort portDst;
	private RoutingManager routingmanager;
	
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
	    this.routingmanager = new RoutingManager();
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
	public void StreamTransmitMain(IOFSwitchService switchService, IDevice deviceCamera,IDevice deviceClient,
			Camera camera, String IPDst, int portDst) {
		
		String cameraIp = camera.getCameraIp();
		int cameraPort = camera.getCameraPort();
		this.portSrc = OFPort.ofInt(portDst);  //client is source in TCP
		this.portDst = OFPort.ofInt(cameraPort);  //camera is destination in TCP
		System.out.println("the IP of camera is "+ cameraIp);
		System.out.println("the IP of client is "+ IPDst);
		//First,Create a TCP packet for next step;
		//Testing how to find the switch connected to Devices.			
		System.out.println("the camera Device is: " + deviceCamera);
		System.out.println("the client Device is: " + deviceClient);
		
		System.out.println("camera mac: " + deviceCamera.getMACAddress());
		System.out.println("client mac: " + deviceClient.getMACAddress());
		String rtspCmd = camera.getRtspAddr();
		System.out.println(rtspCmd);
		byte[] data = rtspCmd.getBytes();

		TCPcreator(camera.getSwitch(), deviceClient, deviceCamera, IPDst, cameraIp, data); //camera is destination, client is source;
		AddStaticFlows(switchService,deviceCamera,deviceClient);
//		AddStaticGroupFlows(switchService,deviceCamera,deviceClient);
	}
	
	//send static flow entry
	private DatapathId findDpidByDeviceIp(IDevice device) {
		SwitchPort [] switchPort = device.getAttachmentPoints();
		DatapathId dpid = switchPort[0].getNodeId();
		return dpid;
	}
	
	private OFPort findDpidPortByDeviceIp(IDevice device) {
		SwitchPort [] switchPort = device.getAttachmentPoints();
		OFPort port = switchPort[0].getPortId();
		return port;
	}
	
	private void AddStaticFlows(IOFSwitchService switchService,IDevice deviceSrc,IDevice deviceDst) {
		this.dpidSrc = findDpidByDeviceIp(deviceSrc); 
		this.dpidDst = findDpidByDeviceIp(deviceDst);
		this.portSrc = findDpidPortByDeviceIp(deviceSrc);
		this.portDst = findDpidPortByDeviceIp(deviceDst);
		
		Path path = routingmanager.getPath(dpidSrc, portSrc, dpidDst, portDst);
		List<NodePortTuple> nodeportList = path.getPath();
		System.out.println("start add flow...");
		for(int index = 0;index<nodeportList.size();index+=2) {
			DatapathId switchDPID = nodeportList.get(index).getNodeId();
            IOFSwitch sw = switchService.getSwitch(switchDPID);
            //构造流表
    		//封装flowmod消息
    		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
    		//匹配域
    		Builder mb = sw.getOFFactory().buildMatch();
    		mb.setExact(MatchField.ETH_TYPE, EthType.IPv4);
    		mb.setExact(MatchField.IPV4_SRC, IPv4Address.of(deviceSrc.getIPv4Addresses()[0].toString()));
    		mb.setExact(MatchField.IP_PROTO,IpProtocol.UDP);
    		mb.setExact(MatchField.IN_PORT, OFPort.of(2));
    		//指令
    		List<OFAction> actions = new ArrayList<OFAction>();
//    		actions.add(sw.getOFFactory().actions().output(nodeportList.get(index+1).getPortId(), Integer.MAX_VALUE));
    		actions.add(sw.getOFFactory().actions().output(OFPort.of(3), Integer.MAX_VALUE));
    		
    		IPv4Address desIp = IPv4Address.of("192.168.3.101");
    		OFOxmIpv4Dst.Builder oxmbDstIp = sw.getOFFactory().oxms().buildIpv4Dst();
    		oxmbDstIp.setValue(desIp);
    		OFActionSetField setFieldActionDstIp = sw.getOFFactory().actions().setField(oxmbDstIp.build());
    		actions.add(setFieldActionDstIp);
    		
    		actions.add(sw.getOFFactory().actions().output(OFPort.of(4), Integer.MAX_VALUE));
    		//封装flowmod
    		U64 cookie = AppCookie.makeCookie(2, 0);
    		fmb.setCookie(cookie)
    		.setHardTimeout(0)
    		.setIdleTimeout(0)
    		.setBufferId(OFBufferId.NO_BUFFER)
    		.setPriority(5)
    		.setMatch(mb.build());
    		FlowModUtils.setActions(fmb, actions, sw);
    		sw.write(fmb.build());
		}
		System.out.println("add flows success!");
	}
	
	private void AddStaticGroupFlows(IOFSwitchService switchService,IDevice deviceSrc,IDevice deviceDst) {
		this.dpidSrc = findDpidByDeviceIp(deviceSrc); 
		this.dpidDst = findDpidByDeviceIp(deviceDst);
		this.portSrc = findDpidPortByDeviceIp(deviceSrc);
		this.portDst = findDpidPortByDeviceIp(deviceDst);
		new Thread() {
			public void run() {
				Path path = routingmanager.getPath(dpidSrc, portSrc, dpidDst, portDst);
				List<NodePortTuple> nodeportList = path.getPath();
				int lenth = nodeportList.size();
				OFPort outPort = nodeportList.get(lenth-1).getPortId();
				System.out.println("开始下发组表表");
				addGroup(switchService);
				addStaticGroupReal(switchService,outPort);
				System.out.println("下发成功");	
			}						
		}.start();
		
	}
	public void addGroup(IOFSwitchService switchService) {
		//拿到交换记对象
		Set<DatapathId> ids = switchService.getAllSwitchDpids();
		DatapathId swid = null;
		for(DatapathId id : ids) {
			swid = id;
			break;
		}
		IOFSwitch sw = switchService.getSwitch(swid);
		
		//组表下发
		List<OFBucket> buckets = new ArrayList<OFBucket>();
		
		org.projectfloodlight.openflow.protocol.OFGroupAdd.Builder gmb = sw.getOFFactory().buildGroupAdd();
		gmb.setBuckets(buckets);
		OFGroup group = OFGroup.of(1);
		gmb.setGroup(group);
		gmb.setGroupType(OFGroupType.ALL);
		sw.write(gmb.build());
		System.out.println("添加组表结束");
	}
	
	public void addStaticGroupReal(IOFSwitchService switchService,OFPort outPort) {
		//拿到交换记对象
		Set<DatapathId> ids = switchService.getAllSwitchDpids();
		DatapathId swid = null;
		for(DatapathId id : ids) {
			swid = id;
			break;
		}
		IOFSwitch sw = switchService.getSwitch(swid);
		
		//组表下发
		List<OFBucket> buckets = new ArrayList<OFBucket>();
		
		//填充buckets列表
		//--------------------第1个bucket填充
		OFBucket.Builder bucket1 = sw.getOFFactory().buildBucket();
		List<OFAction> actions1 = new ArrayList<OFAction>();
		
		actions1.add(sw.getOFFactory().actions().output(OFPort.of(3), Integer.MAX_VALUE));
		
		bucket1.setActions(actions1);
		bucket1.setWatchPort(OFPort.ANY);
		bucket1.setWatchGroup(OFGroup.ANY);
		
		//--------------------第二个bucket填充
		OFBucket.Builder bucket2 = sw.getOFFactory().buildBucket();
		List<OFAction> actions2 = new ArrayList<OFAction>();
		
		actions2.add(sw.getOFFactory().actions().output(OFPort.of(6), Integer.MAX_VALUE));
		
		//绑定两个action到bucket2
		bucket2.setActions(actions2);
		bucket2.setWatchPort(OFPort.ANY);
		bucket2.setWatchGroup(OFGroup.ANY);
		
		buckets.add(bucket1.build());
		buckets.add(bucket2.build());
		
		
		org.projectfloodlight.openflow.protocol.OFGroupAdd.Builder gmb = sw.getOFFactory().buildGroupAdd();
		gmb.setBuckets(buckets);
		OFGroup group = OFGroup.of(1);
		gmb.setGroup(group);
		gmb.setGroupType(OFGroupType.ALL);
		sw.write(gmb.build());
		System.out.println("添加组表项结束");
	}
	
	//TCP Construction Method,
	//@IPSr: Source IP 
	//@IPDs: Destination IP 
	//@Data: Payload Data
	private void TCPcreator(IOFSwitch sw,IDevice deviceSrc,IDevice deviceDst,String IPSr,String IPDs,byte Data[]){
//test: curl http://localhost:8080/wm/streamingtransit/Trans -X POST -d '{"camera":{"id":1,"ip":"192.168.3.254","port":1,"username":"admin","passwd":"admin123","rtstAddr":"rtsp://admin:admin123@192.168.3.254:554/Streaming/Channels/101?transportmode=unicast&profile=Profile_1"},"dest":{"ip":"192.168.3.101","port":1}}'

		Ethernet l2 = new Ethernet();
		l2.setSourceMACAddress(deviceSrc.getMACAddress());//Not Sure.
		l2.setDestinationMACAddress(deviceDst.getMACAddress());//Not sure.
		l2.setEtherType(EthType.IPv4);
		
		IPv4 l3 = new IPv4();
		l3.setSourceAddress(IPSr);
		l3.setDestinationAddress(IPDs);
		l3.setTtl((byte) 64);//Time to live;
		l3.setProtocol(IpProtocol.TCP);
		
		TCP l4 = new TCP();
		l4.setSourcePort(portSrc.getPortNumber());//Not Sure
		l4.setDestinationPort(portDst.getPortNumber());//Not Sure
		
		Data l7 = new Data();
		/*this line will add data in TCP packet*/
		l7.setData(Data);
		l2.setPayload(l3);
		l3.setPayload(l4);
		l4.setPayload(l7);
		byte[] serializedData = l2.serialize();
		System.out.println("client Port:" + portSrc + "  camera Port:" + portDst);
		OFPacketOut po = sw.getOFFactory().buildPacketOut() 
			    .setData(serializedData)
			    .setActions(Collections.singletonList((OFAction) sw.getOFFactory().actions().output(portDst,Integer.MAX_VALUE)))//this will change.
			    .setInPort(OFPort.CONTROLLER)
			    .build();
			  
		sw.write(po);		
	}
	
}