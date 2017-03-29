package cl.droid.iot.kura.protocol.zigbee4java;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.bubblecloud.zigbee.ZigBeeApi;
import org.bubblecloud.zigbee.api.Device;
import org.bubblecloud.zigbee.api.DeviceListener;
import org.bubblecloud.zigbee.network.NodeListener;
import org.bubblecloud.zigbee.network.ZigBeeEndpoint;
import org.bubblecloud.zigbee.network.ZigBeeNode;
import org.bubblecloud.zigbee.network.model.DiscoveryMode;
//import org.bubblecloud.zigbee.network.port.ZigBeePort;
import org.bubblecloud.zigbee.v3.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ZigBeeCoordinatorHandler{
	private Logger logger = LoggerFactory.getLogger(ZigBeeCoordinatorHandler.class);
	
    protected int panId = 4570;
    protected int channelId = 15;

    protected ZigBeeApi zigbeeApi = null;
    protected SerialPort networkInterface;
    
    private Thread mainThread = null;
    
    protected Future<?> m_handle = null; // restartJob
    protected ScheduledThreadPoolExecutor m_worker;
	
    protected void startZigBee(SerialPort networkInterface) {
    	logger.debug("startZigBee...");
        this.networkInterface = networkInterface;

        // Start the network. This is a scheduled task to ensure we give the coordinator
        // some time to initialise itself!
        startZigBeeNetwork();
        logger.debug("startZigBee... Done.");
    }
    private void startZigBeeNetwork() {
    	logger.debug("startZigBeeNetwork...");
    	
    	Runnable runnable = new Runnable() {
	        @Override
	        public void run() {
	          logger.debug("ZigBee network starting");
	          m_handle = null;
	          initialiseZigBee();
	        }
	      };

        logger.debug("Scheduling ZigBee start");
        m_handle = m_worker.schedule(runnable, 1, TimeUnit.SECONDS);
        logger.debug("startZigBeeNetwork... Done.");
    }
//    abstract void restartZigBeeNetwork();
    protected void restartZigBeeNetwork() {
    	logger.debug("startZigBeeNetwork...");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                logger.debug("ZigBee network restarting");
                m_handle = null;
                initialiseZigBee();
            }
        };
        logger.debug("Scheduleing ZigBee restart");
        m_handle = m_worker.schedule(runnable, 15, TimeUnit.SECONDS);
        logger.debug("startZigBeeNetwork... Done.");
    }
    private void initialiseZigBee() {
    	logger.debug("initialiseZigBee...");
    	mainThread = Thread.currentThread();

        final Set<DiscoveryMode> discoveryModes = new HashSet<DiscoveryMode>();
        discoveryModes.add(DiscoveryMode.Addressing);
        discoveryModes.add(DiscoveryMode.Announce);
        boolean resetNetwork = false;
        zigbeeApi = new ZigBeeApi(networkInterface, panId, channelId, null, resetNetwork, discoveryModes);

        logger.debug("post startup");
        logger.debug("pre initializeHardware");
        zigbeeApi.initializeHardware();
        logger.debug("post initializeHardware");

        boolean reset = false;
        int channel = zigbeeApi.getZigBeeNetworkManager().getCurrentChannel();
        int pan = zigbeeApi.getZigBeeNetworkManager().getCurrentPanId();
        if (channel != channelId || pan != panId) {
            logger.info("ZigBee current pan={}, channel={}. Network will be reset.", pan, channel);
            reset = true;
        }

        logger.debug("pre initializeNetwork");
        if (!zigbeeApi.initializeNetwork(reset)) {

            // Shut down the ZigBee library
            if (zigbeeApi != null) {
                zigbeeApi.shutdown();
                zigbeeApi = null;
            }

            restartZigBeeNetwork();

            return;
        }
        logger.debug("post initializeNetwork");
        zigbeeApi.addDeviceListener(new DeviceListener() {
            @Override
            public void deviceAdded(Device device) {
                print("Device added: " + device.getEndpointId() + " (#" + device.getNetworkAddress() + ")");
            }

            @Override
            public void deviceUpdated(Device device) {
                print("Device updated: " + device.getEndpointId() + " (#" + device.getNetworkAddress() + ")");
            }

            @Override
            public void deviceRemoved(Device device) {
                print("Device removed: " + device.getEndpointId() + " (#" + device.getNetworkAddress() + ")");
            }
        });
        zigbeeApi.addNodeListener(new NodeListener() {
			@Override
			public void nodeAdded(ZigBeeNode node) {
                print("Node added: " + node.getIeeeAddress() + " (#" + node.getNetworkAddress() + ")");
			}

			@Override
			public void nodeDiscovered(ZigBeeNode node) {
                print("Node discovered: " + node.getIeeeAddress() + " (#" + node.getNetworkAddress() + ")");
			}

			@Override
			public void nodeUpdated(ZigBeeNode node) {
                print("Node updated: " + node.getIeeeAddress() + " (#" + node.getNetworkAddress() + ")");
			}

			@Override
			public void nodeRemoved(ZigBeeNode node) {
                print("Node removed: " + node.getIeeeAddress() + " (#" + node.getNetworkAddress() + ")");
			}
        });
        
        print("There are " + zigbeeApi.getDevices().size() + " known devices in the network.");

        logger.debug("ZigBee console ready.");

        final List<ZigBeeEndpoint> endpoints = new ArrayList<ZigBeeEndpoint>();
        for (final ZigBeeNode node : zigbeeApi.getNodes()) {
            for (final ZigBeeEndpoint endpoint : zigbeeApi.getNodeEndpoints(node)) {
                endpoints.add(endpoint);
            }
        }
        waitForNetwork();
        logger.debug("initialiseZigBee... Done.");
    }
    protected void waitForNetwork() {
    	logger.debug("waitForNetwork...");

    	logger.debug("waitForNetwork... Done.");
    }
	
    private void print(final String line) {
        logger.debug(line);
    }

}
