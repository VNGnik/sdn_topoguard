package net.floodlightcontroller.kltn;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import java.util.HashMap;
import java.util.Iterator;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.Controller;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.IListener.Command;
import net.floodlightcontroller.core.util.AppCookie;
import net.floodlightcontroller.core.util.ListenerDispatcher;

import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.packet.*;

//Import them thread

import net.floodlightcontroller.threadpool.IThreadPoolService;


public class Kltn implements IFloodlightModule, IOFMessageListener {

	
	protected IFloodlightProviderService floodlightProvider;
	protected IOFSwitchService switchService;
	protected static ConcurrentMap<OFType, ListenerDispatcher<OFType,IOFMessageListener>> messageListeners;

	// Khai bao instance cua Controllter
	protected Controller aaa;
	// Bien thread
	private static IThreadPoolService threadPoolService;
	private static ScheduledFuture<?> eliPortFromController;
	
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return Kltn.class.getSimpleName();
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

	//@Override
	/*public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		// TODO Auto-generated method stub
		return null;
	}*/

	//@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		// TODO Auto-generated method stub
		Collection< Class< ? extends IFloodlightService >> l = 
				new ArrayList< Class< ? extends IFloodlightService >>();
		l.add(IFloodlightProviderService.class );
		l.add(IOFSwitchService.class );
		l.add(IDeviceService.class);
		return l;
		//return null;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		
		//Khai bao bien Thread
		threadPoolService = context.getServiceImpl(IThreadPoolService.class);
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		messageListeners = new ConcurrentHashMap<OFType, ListenerDispatcher<OFType, IOFMessageListener>>();
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		System.out.println("+++++++++++++++Hello+++++++++++++++++++++++++");
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		
		// Khoi tao gia tri thread, sau khi khoi dong controll thi 5s sau thread moi chay, sau do 16s se chay 1 lan
		eliPortFromController = threadPoolService.getScheduledExecutor().scheduleAtFixedRate(new EliPortFromController(), 5, 16, TimeUnit.SECONDS);
	}
	
	
	//Ham de lay gia tri tu Thread 
	protected class EliPortFromController implements Runnable {

		@Override
		public void run() {
			System.out.println(java.time.LocalTime.now());
			System.out.println(aaa.getValue());
		}
	}
	
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		Ethernet eth = null;

    	System.out.println("11111111111111111111111111111111");
	    switch (msg.getType())
	    {
	    	default:
                List<IOFMessageListener> listeners = null;
                if (messageListeners.containsKey(msg.getType())) {
                    listeners = messageListeners.get(msg.getType()).getOrderedListeners();
                }
                FloodlightContext bc = null;
                bc = cntx;
	    		//eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
	    		if (listeners != null) {
                    if (eth != null) {
                        IFloodlightProviderService.bcStore.put(bc,IFloodlightProviderService.CONTEXT_PI_PAYLOAD,eth);
                    }
	    			if (eth.getPayload() instanceof LLDP)
	    			{
	    				System.out.println("4444444444444444444444444444");
	    				LLDP lldp =  (LLDP) eth.getPayload();
	    				LLDPTLV chId = lldp.getChassisId();
	    				LLDPTLV portId = lldp.getPortId();
	    				//eliPort.put(chId, portId);
	    				//System.out.println(setHashMap);
	    			}
	    			else{
	    				System.out.println("33333333333333333333333333333333");
	    			}
	    		}
	    		return Command.CONTINUE;
	    }
	}
}
