package kr.ac.postech.dpnm.perfmon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.AppCookie;

import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv4AddressWithMask;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowModSample implements IOFSwitchListener, IFloodlightModule
{
	protected FloodlightModuleContext context;
	protected IOFSwitchService switchService;
	protected U64 cookie;
	public static int FLOWMOD_DEFAULT_IDLE_TIMEOUT = 0; 	// infinite
	public static int FLOWMOD_DEFAULT_HARD_TIMEOUT = 0; 	// infinite
	public static int FLOWMOD_DEFAULT_PRIORITY = 1;
	public static int FLOWMOD_SAMPLE_APP_ID = 1024;		// APP_ID
	protected static Logger logger = LoggerFactory.getLogger( FlowRulesHub.class );
	
	@Override
	public Collection< Class< ? extends IFloodlightService >> getModuleServices()
	{
		return null;
	}

	@Override
	public Map< Class< ? extends IFloodlightService >, IFloodlightService > getServiceImpls()
	{
		return null;
	}

	@Override
	public Collection< Class< ? extends IFloodlightService >> getModuleDependencies()
	{
		Collection< Class< ? extends IFloodlightService >> l = 
				new ArrayList< Class< ? extends IFloodlightService >>();
		l.add( IFloodlightProviderService.class );
		l.add( IOFSwitchService.class );
		return l;
	}

	@Override
	public void init( FloodlightModuleContext context )
			throws FloodlightModuleException
	{
		switchService = context.getServiceImpl( IOFSwitchService.class );
		
		AppCookie.registerApp( FLOWRUlES_MOD_APP_ID, "FlowRulesHub" );
		cookie = AppCookie.makeCookie( FLOWMOD_SAMPLE_APP_ID, 2 );
		
		logger.debug( "FlowRulesHub has been initialized!" );
	}

	@Override
	public void startUp( FloodlightModuleContext context )
			throws FloodlightModuleException
	{
		switchService.addOFSwitchListener( this );
	}

	private void writeIPFlowMod( IOFSwitch sw, String srcIpStr, String dstIpStr, 
	                              boolean isMasked, int outPortNum )
	{
		// generate a Match Filter
		Match.Builder mb = sw.getOFFactory().buildMatch();
		
		if( isMasked )
		{
			mb.setMasked( MatchField.IPV4_SRC, IPv4AddressWithMask.of( srcIpStr ) );
			mb.setMasked( MatchField.IPV4_DST, IPv4AddressWithMask.of( dstIpStr ) );
		}
		else
		{
			mb.setExact( MatchField.IPV4_SRC, IPv4Address.of( srcIpStr ) );
			mb.setExact( MatchField.IPV4_DST, IPv4Address.of( dstIpStr ) );
		}
		mb.setExact( MatchField.ETH_TYPE, EthType.IPv4 );
		
		// generate an action list
		List<OFAction> al = new ArrayList<OFAction>();

		// generate a port and table id instance
		OFPort outPort = OFPort.ofInt( outPortNum );
		OFAction action = sw.getOFFactory().actions().buildOutput().
							 setPort(outPort).setMaxLen(Integer.MAX_VALUE).build();
		al.add( action );
		
		// generate and start to build an OFFlowMod Message
		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
		fmb.setCookie( cookie )		
		.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
		.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
		.setBufferId(OFBufferId.NO_BUFFER)
		.setMatch( mb.build() )
		.setActions( al )
		.setPriority(FLOWMOD_DEFAULT_PRIORITY);
		
		// finally write it out to switch
		sw.write( fmb.build() );
		sw.flush();
		
		logger.debug("Sending 1 new entry to {}", sw.getId().toString() );
	}
	
	private void writeARPFlowMod( IOFSwitch sw, String srcIpStr, String dstIpStr, 
	                               boolean isMasked, int outPortNum )
	{
		// generate a Match Filter
		Match.Builder mb = sw.getOFFactory().buildMatch();
		
		if( isMasked )
		{
			mb.setMasked( MatchField.ARP_SPA, IPv4AddressWithMask.of( srcIpStr ) );
			mb.setMasked( MatchField.ARP_TPA, IPv4AddressWithMask.of( dstIpStr ) );
		}
		else
		{
			mb.setExact( MatchField.ARP_SPA, IPv4Address.of( srcIpStr ) );
			mb.setExact( MatchField.ARP_TPA, IPv4Address.of( dstIpStr ) );
		}
		mb.setExact( MatchField.ETH_TYPE, EthType.ARP );
		
		// generate an action list
		List<OFAction> al = new ArrayList<OFAction>();

		// generate a port and table id instance
		OFPort outPort = OFPort.ofInt( outPortNum );
		OFAction action = sw.getOFFactory().actions().buildOutput().
							 setPort(outPort).setMaxLen(Integer.MAX_VALUE).build();
		al.add( action );
		
		// generate and start to build an OFFlowMod Message
		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
		fmb.setCookie( cookie )		
		.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
		.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
		.setBufferId(OFBufferId.NO_BUFFER)
		.setMatch( mb.build() )
		.setActions( al )
		.setPriority(FLOWMOD_DEFAULT_PRIORITY);
		
		// finally write it out to switch
		sw.write( fmb.build() );
		sw.flush();
		
		logger.debug("Sending 1 new entry to {}", sw.getId().toString() );
	}
	
	private boolean checkEquality( DatapathId switchId, int idx )
	{
		String switchIdStr = switchId.toString();		
		String idxStr = String.format( "00:00:00:00:00:00:02:%02d", idx );
		
		return switchIdStr.equals( idxStr );
	}
	
	@Override
	public void switchAdded( DatapathId switchId )
	{
		IOFSwitch sw = switchService.getSwitch( switchId );
		
		if( checkEquality(switchId, 1) )
		{
			writeIPFlowMod( sw, "10.0.1.1", "10.0.1.2", false, 2 );
			writeARPFlowMod( sw, "10.0.1.1", "10.0.1.2", false, 2 );
			
			writeIPFlowMod( sw, "10.0.1.2", "10.0.1.1", false, 1 );
			writeARPFlowMod( sw, "10.0.1.2", "10.0.1.1", false, 1 );
		}
		if( checkEquality(switchId, 2) || checkEquality(switchId, 3) )
		{
			writeIPFlowMod( sw, "10.0.1.1", "10.0.1.2", false, 2 );
			writeARPFlowMod( sw, "10.0.1.1", "10.0.1.2", false, 2 );
			
			writeIPFlowMod( sw, "10.0.1.2", "10.0.1.1", false, 1 );
			writeARPFlowMod( sw, "10.0.1.2", "10.0.1.1", false, 1 );
		}
		if( checkEquality(switchId, 5) )
		{
			writeIPFlowMod( sw, "10.0.1.1", "10.0.1.2", false, 3 );
			writeARPFlowMod( sw, "10.0.1.1", "10.0.1.2", false, 3 );
			
			writeIPFlowMod( sw, "10.0.1.2", "10.0.1.1", false, 1 );
			writeARPFlowMod( sw, "10.0.1.2", "10.0.1.1", false, 1 );
		}
	}

	@Override
	public void switchRemoved( DatapathId switchId )
	{
		
	}

	@Override
	public void switchActivated( DatapathId switchId )
	{
		
	}

	@Override
	public void switchPortChanged(
									DatapathId switchId,
									OFPortDesc port,
									PortChangeType type )
	{

	}

	@Override
	public void switchChanged( DatapathId switchId )
	{
		
	}
}
