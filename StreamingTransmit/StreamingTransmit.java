package net.floodlightcontroller.StreamingTransmit;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ConcurrentSkipListSet;

import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;

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
import org.projectfloodlight.openflow.protocol.oxm.OFOxmUdpDst;
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
import net.floodlightcontroller.camera.Camera;
import net.floodlightcontroller.CONST.Const;
import net.floodlightcontroller.StreamingTransmit.web.StreamingTransmitWebRoutable;
import net.floodlightcontroller.core.FloodlightContext;
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
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.routing.Path;
import net.floodlightcontroller.routing.RoutingManager;
import net.floodlightcontroller.util.FlowModUtils;
import utils.Utils;

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
	
	private Map <String,Integer>CameraGroupIdMap;
	private int GroupId;
	private Map <Integer,Set<String>>GroupIdHostCollectionMap;
	private Map <String,Boolean>CameraStaticflowIn;
	
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
	    
	    this.CameraGroupIdMap = new HashMap<String,Integer>();
	    this.CameraStaticflowIn = new HashMap<String,Boolean>();
	    this.GroupIdHostCollectionMap = new HashMap<Integer,Set<String>>();
	    this.GroupId = 0;
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		if (restApi != null) restApi.addRestletRoutable(new StreamingTransmitWebRoutable());
//		IOFSwitchService switchService = (IOFSwitchService) getContext().getAttributes().get(IOFSwitchService.class.getCanonicalName());
		// 下发静态流表用于匹配拉流服务器的packet_in消息
		

	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		switch (msg.getType()) {
	    case PACKET_IN:
	        /* Retrieve the deserialized packet in message */
	        Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
	 

	        /* 
	         * Check the ethertype of the Ethernet frame and retrieve the appropriate payload.
	         * Note the shallow equality check. EthType caches and reuses instances for valid types.
	         */
	        if (eth.getEtherType() == EthType.IPv4) {
	            /* We got an IPv4 packet; get the payload from Ethernet */
	            IPv4 ipv4 = (IPv4) eth.getPayload();
	             
	            /* Various getters and setters are exposed in IPv4 */
	            byte[] ipOptions = ipv4.getOptions();
	            IPv4Address dstIp = ipv4.getDestinationAddress();
	            String srcIP = ipv4.getSourceAddress().toString();
	            if (!srcIP.equals(Const.STREAM_SERVER_IP)) return Command.CONTINUE;
	            System.out.println("packet_in: \n source ip: " + ipv4.getSourceAddress());
	            /* 
	             * Check the IP protocol version of the IPv4 packet's payload.
	             */
	            if (ipv4.getProtocol() == IpProtocol.UDP) {
	                /* We got a UDP packet; get the payload from IPv4 */
	                UDP udp = (UDP) ipv4.getPayload();
	  
	                /* Various getters and setters are exposed in UDP */
	                TransportPort srcPort = udp.getSourcePort();
	                TransportPort dstPort = udp.getDestinationPort();
	                Data udpPayload = (Data) udp.getPayload();
	                if(dstPort.getPort() == 23456) {  //固定端口
	                	System.out.println("Received packet_in from stream server, the faulty camera IP is " + new String(udpPayload.getData()));
	                }
	            }
	 
	        }
	        break;
	    default:
	        break;
	    }
	    return Command.CONTINUE;
//	    return null;
	}
	//in this method, we will implement the TCP connection and flow delivery.
	@Override
	public void StreamTransmitMain(IOFSwitchService switchService, IDevice deviceCamera, IDeviceService deviceManager, List<String> clientIPList) {
		String cameraIp = deviceCamera.getIPv4Addresses()[0].toString();
		if (clientIPList.size() == 1) {
			String[] strArr = clientIPList.get(0).split(":");
			String clientIP = strArr[0];
			int udpPort = Integer.parseInt(strArr[1]);
			IDevice deviceClient = Utils.deviceSearch(deviceManager, clientIP);
			if(CameraStaticflowIn.get(cameraIp)==null) {
				CameraStaticflowIn.put(cameraIp, false);			
			}
			
			boolean CameraStaticflowFlag = CameraStaticflowIn.get(cameraIp);
			
			AddStaticFlows(switchService,deviceCamera,deviceClient,CameraStaticflowFlag,cameraIp,clientIP,udpPort);
			
		} else {
//			AddStaticGroupFlows(switchService,deviceCamera,clientIPList);
//			for (String str : clientIPList) {
//				String[] strArr = str.split(":");
//				String clientIP = strArr[0];
//				int udpPort = Integer.parseInt(strArr[1]);
//			}
			
		}
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
	
	private void AddStaticFlows(IOFSwitchService switchService,IDevice deviceSrc,IDevice deviceDst,Boolean flowexist,String cameraIp,String clientIp,int udpPort) {
		this.dpidSrc = findDpidByDeviceIp(deviceSrc); 
		this.dpidDst = findDpidByDeviceIp(deviceDst);
		this.portSrc = findDpidPortByDeviceIp(deviceSrc);
		this.portDst = findDpidPortByDeviceIp(deviceDst);
		
		Path path = routingmanager.getPath(dpidSrc, portSrc, dpidDst, portDst);
		List<NodePortTuple> nodeportList = path.getPath();
		DatapathId firstswitchDPID = nodeportList.get(0).getNodeId();
		OFPort outport = nodeportList.get(1).getPortId();
		//已经被拉过的流，静态流表已经存在了，只需修改组表就行
		if(flowexist) {		
			System.out.println("start add flow...");
			
			int GroupId_this = CameraGroupIdMap.get(cameraIp);
			Set<String> hostcollection = this.GroupIdHostCollectionMap.get(GroupId_this);
			if(hostcollection == null) {
				hostcollection = new HashSet<String>();
			}
			hostcollection.add(clientIp+"/"+udpPort+"/"+outport.getPortNumber());
			this.GroupIdHostCollectionMap.put(GroupId_this, hostcollection);
			System.out.println(GroupIdHostCollectionMap);
			addModifyGroup(switchService,firstswitchDPID,GroupId_this);
			addOtherStaticflows(switchService,nodeportList,deviceSrc,deviceDst);
			
		}else {
			//没有被拉过流，需要先下发静态流表，在下发组表，并记录GroupId
			this.CameraStaticflowIn.put(cameraIp, true);
			
			System.out.println("start add flow...");
					
			addSpaceGroup(switchService,firstswitchDPID,GroupId);
			this.CameraGroupIdMap.put(cameraIp, GroupId);
			
			
			Set<String> hostcollection = this.GroupIdHostCollectionMap.get(GroupId);
			if(hostcollection == null) {
				hostcollection = new HashSet<String>();
			}
			hostcollection.add(clientIp+"/"+udpPort+"/"+outport.getPortNumber());
			this.GroupIdHostCollectionMap.put(GroupId, hostcollection);
			System.out.println(GroupIdHostCollectionMap);
			addStaticGroupReal(switchService,firstswitchDPID,cameraIp,GroupId);
			
			addModifyGroup(switchService,firstswitchDPID,GroupId);
			addOtherStaticflows(switchService,nodeportList,deviceSrc,deviceDst);
			
			GroupId ++;//groupid更新
		}
		
	}
	
	
	private void addSpaceGroup(IOFSwitchService switchService,DatapathId switchDPID,int GroupId) {
		//拿到交换记对象
		Set<DatapathId> ids = switchService.getAllSwitchDpids();
		DatapathId swid = switchDPID;
		IOFSwitch sw = switchService.getSwitch(swid);
		
		//组表下发
		List<OFBucket> buckets = new ArrayList<OFBucket>();
		
		org.projectfloodlight.openflow.protocol.OFGroupAdd.Builder gmb = sw.getOFFactory().buildGroupAdd();
		gmb.setBuckets(buckets);
		OFGroup group = OFGroup.of(GroupId);
		gmb.setGroup(group);
		gmb.setGroupType(OFGroupType.ALL);
		sw.write(gmb.build());
		System.out.println("add space group success");
	}
	
	private void addStaticGroupReal(IOFSwitchService switchService,DatapathId firstDatapathID,String cameraIp,int GroupId) {
		//拿到交换记对象
		Set<DatapathId> ids = switchService.getAllSwitchDpids();
		DatapathId swid = firstDatapathID;
		IOFSwitch sw = switchService.getSwitch(swid);
		
		//封装flowmod消息
		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
		//匹配域
		Builder mb = sw.getOFFactory().buildMatch();
		mb.setExact(MatchField.ETH_TYPE, EthType.IPv4)
		.setExact(MatchField.IPV4_SRC, IPv4Address.of(cameraIp))
		.setExact(MatchField.IP_PROTO, IpProtocol.UDP)
		.setExact(MatchField.UDP_DST, TransportPort.of(16264));
		//指令
		List<OFAction> actions = new ArrayList<OFAction>();
		actions.add(sw.getOFFactory().actions().group(OFGroup.of(GroupId)));
		//封装flowmod
		fmb.setHardTimeout(0)
		.setIdleTimeout(0)
		.setBufferId(OFBufferId.NO_BUFFER)
		.setPriority(5)
		.setMatch(mb.build());
		FlowModUtils.setActions(fmb, actions, sw);
		sw.write(fmb.build());
		System.out.println("add group real success");
	}
	
	private void addOtherStaticflows(IOFSwitchService switchService,List<NodePortTuple> nodeportList,IDevice deviceSrc,IDevice deviceDst) {
		List<NodePortTuple> nodeportlist = nodeportList;
		for(int index = 2;index<nodeportList.size();index+=2) {
			DatapathId switchDPID = nodeportlist.get(index).getNodeId();
            IOFSwitch sw = switchService.getSwitch(switchDPID);
            //构造流表
    		//封装flowmod消息
    		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
    		//匹配域
    		Builder mb = sw.getOFFactory().buildMatch();
    		mb.setExact(MatchField.ETH_TYPE, EthType.IPv4);
    		mb.setExact(MatchField.IPV4_SRC, IPv4Address.of(deviceSrc.getIPv4Addresses()[0].toString()));
    		mb.setExact(MatchField.IPV4_DST, IPv4Address.of(deviceDst.getIPv4Addresses()[0].toString()));
    		mb.setExact(MatchField.IP_PROTO,IpProtocol.UDP);
    		//指令
    		List<OFAction> actions = new ArrayList<OFAction>();
    		
    		actions.add(sw.getOFFactory().actions().output(nodeportlist.get(index+1).getPortId(), Integer.MAX_VALUE));
    		//封装flowmod
    		fmb.setHardTimeout(0)
    		.setIdleTimeout(0)
    		.setBufferId(OFBufferId.NO_BUFFER)
    		.setPriority(5)
    		.setMatch(mb.build());
    		FlowModUtils.setActions(fmb, actions, sw);
    		sw.write(fmb.build());
		}
		System.out.println("add other flows success!");
	}
	
	private void addModifyGroup(IOFSwitchService switchService,DatapathId firstDatapathID,int GroupId) {
		//拿到交换记对象
		Set<DatapathId> ids = switchService.getAllSwitchDpids();
		DatapathId swid = firstDatapathID;
		IOFSwitch sw = switchService.getSwitch(swid);
		Object[] Iplist = GroupIdHostCollectionMap.get(GroupId).toArray();
		//组表下发
		List<OFBucket> buckets = new ArrayList<OFBucket>();
		for(int i = 0;i < Iplist.length;i++) {
			List<OFAction> actions = new ArrayList<OFAction>();
			String[]IpAndPort = Iplist[i].toString().split("/");
			IPv4Address desIp = IPv4Address.of(IpAndPort[0]);
			int udpPort = Integer.parseInt(IpAndPort[1]);
			int outport = Integer.parseInt(IpAndPort[2]);
			
			OFOxmIpv4Dst.Builder oxmbDstIp = sw.getOFFactory().oxms().buildIpv4Dst();
			oxmbDstIp.setValue(desIp);
			OFActionSetField setFieldActionDstIp = sw.getOFFactory().actions().setField(oxmbDstIp.build());
			actions.add(setFieldActionDstIp);
			
			OFOxmUdpDst.Builder oxmUdpDst = sw.getOFFactory().oxms().buildUdpDst();
			oxmUdpDst.setValue(TransportPort.of(udpPort));
			OFActionSetField setFieldActionUdpDst = sw.getOFFactory().actions().setField(oxmUdpDst.build());
			actions.add(setFieldActionUdpDst);
			
			actions.add(sw.getOFFactory().actions().output(OFPort.of(outport),Integer.MAX_VALUE));
			OFBucket.Builder bucket = sw.getOFFactory().buildBucket();
			bucket.setActions(actions)
			.setWatchGroup(OFGroup.ANY)
			.setWatchPort(OFPort.ANY);
			buckets.add(bucket.build());
		}
		
		
		org.projectfloodlight.openflow.protocol.OFGroupModify.Builder gmb = sw.getOFFactory().buildGroupModify();
		gmb.setBuckets(buckets);
		OFGroup group = OFGroup.of(GroupId);
		gmb.setGroup(group);
		gmb.setGroupType(OFGroupType.ALL);
		sw.write(gmb.build());
		System.out.println("modify group success");
	}
	

	

	
}