package cl.droid.iot.kura.protocol.zigbee4java;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.bubblecloud.zigbee.ZigBeeApi;
import org.bubblecloud.zigbee.api.Device;
import org.bubblecloud.zigbee.api.ZigBeeApiConstants;
import org.bubblecloud.zigbee.api.ZigBeeDeviceException;
import org.bubblecloud.zigbee.api.cluster.Cluster;
import org.bubblecloud.zigbee.api.cluster.general.OnOff;
import org.bubblecloud.zigbee.api.cluster.impl.api.core.Attribute;
import org.bubblecloud.zigbee.network.port.ZigBeePort;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerialExample extends ZigBeeCoordinatorHandler implements ConfigurableComponent, ZigBeePort {
	private static final Logger s_logger = LoggerFactory.getLogger(SerialExample.class);

	  private static final String SERIAL_DEVICE_PROP_NAME= "serial.device";
	  private static final String SERIAL_BAUDRATE_PROP_NAME= "serial.baudrate";
	  private static final String SERIAL_DATA_BITS_PROP_NAME= "serial.data-bits";
	  private static final String SERIAL_PARITY_PROP_NAME= "serial.parity";
	  private static final String SERIAL_STOP_BITS_PROP_NAME= "serial.stop-bits";
	  private static final String SERIAL_CMD_PROP_NAME= "serial.cmd";

	  private ConnectionFactory m_connectionFactory;
	  private CommConnection m_commConnection;
	  private InputStream m_commIs;
	  private OutputStream m_commOs;
	  private Map<String, Object> m_properties;
	  
	  private Map<String, ConsoleCommand> commands = new HashMap<String, ConsoleCommand>();

	  // ----------------------------------------------------------------
	  //
	  // Dependencies
	  //
	  // ----------------------------------------------------------------
	  public void setConnectionFactory(ConnectionFactory connectionFactory) {
	    this.m_connectionFactory = connectionFactory;
	  }

	  public void unsetConnectionFactory(ConnectionFactory connectionFactory) {
	    this.m_connectionFactory = null;
	  }

	  // ----------------------------------------------------------------
	  //
	  // Activation APIs
	  //
	  // ----------------------------------------------------------------
	  
	  protected void activateCommands(){
		  s_logger.info("Activating activateCommands...");
//			commands.put("quit", 		new QuitCommand());
//			commands.put("help", 		new HelpCommand());
			commands.put("list", 		new ListCommand());
//			commands.put("nodes", 		new NodesCommand());
			commands.put("desc", 		new DescribeCommand());
//			commands.put("bind", 		new BindCommand());
//			commands.put("unbind", 		new UnbindCommand());
			commands.put("on", 			new OnCommand());
			commands.put("off", 		new OffCommand());
//			commands.put("color",		new ColorCommand());
//			commands.put("level", 		new LevelCommand());
//			commands.put("listen", 	    new ListenCommand());
//			commands.put("unlisten",    new UnlistenCommand());
//			commands.put("subscribe", 	new SubscribeCommand());
//			commands.put("unsubscribe", new UnsubscribeCommand());
//			commands.put("read", 		new ReadCommand());
//			commands.put("write", 		new WriteCommand());
			commands.put("join",        new JoinCommand());
//			commands.put("lqi", 		new LqiCommand());
			s_logger.info("Activating activateCommands... Done.");
		  
	  }

	  protected void activate(ComponentContext componentContext, Map<String,Object> properties) {
	    s_logger.info("Activating SerialExample...");

	    m_worker = new ScheduledThreadPoolExecutor(1);
	    m_properties = new HashMap<String, Object>();
	    dostartZigBee(properties);
	    
	    activateCommands();
	    s_logger.info("Activating SerialExample... Done.");
	  }

	  protected void deactivate(ComponentContext componentContext) {
	    s_logger.info("Deactivating SerialExample...");

	    // shutting down the worker and cleaning up the properties

	    s_logger.info("Deactivating m_handle...");
	    if(m_handle!=null){
	    	m_handle.cancel(true);
	    	m_handle = null;
	    }
	    s_logger.info("Deactivating m_handle... Done.");
	    
	    s_logger.info("Deactivating zigbeeApi...");
        if (zigbeeApi != null){
            zigbeeApi.shutdown();
            zigbeeApi = null;
            }
        s_logger.info("Deactivating zigbeeApi... Done.");
        
	    try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
        }

        s_logger.info("Deactivating m_worker...");
	    if(m_worker!=null){
	    	m_worker.shutdownNow();
	    	m_worker = null;
	    }
	    s_logger.info("Deactivating m_worker... Done.");
	    
	    //close the serial port
	    closePort();
	    s_logger.info("Deactivating SerialExample... Done.");
	  }

	  public void updated(Map<String,Object> properties) {
	    s_logger.info("Updated updated...");

	    doUpdate(properties);
	    s_logger.info("Updated updated... Done.");
	  }

	  // ----------------------------------------------------------------
	  //
	  // Private Methods
	  //
	  // ----------------------------------------------------------------

	  /**
	   * Called after a new set of properties has been configured on the service
	   */
	  private void doUpdate(Map<String, Object> properties) {
		  s_logger.info("doUpdate...");
		    	if (properties!=null){
		    		for (String s : properties.keySet()) {
		    			s_logger.info("Update - "+s+": "+properties.get(s));
		    		}
		    		String cmd = (String) properties.get(SERIAL_CMD_PROP_NAME);
		    		s_logger.info("Update - CMD - "+cmd);
		    		if(zigbeeApi!=null)
		    		{
		    			processInputLine(zigbeeApi, cmd);
		    		}else{
		    			s_logger.info("Update - zigbeeApi null");
		    		}
		    	}
		   s_logger.info("doUpdate... Done.");
	}
	  
	  private void dostartZigBee(Map<String, Object> properties) {
		s_logger.info("dostartZigBee...");
	    try {
	    	if (properties!=null)
	    		for (String s : properties.keySet()) {
	    			s_logger.info("Update - "+s+": "+properties.get(s));
	    		}

	      // cancel a current worker handle if one if active
	      if (m_handle != null) {
	        m_handle.cancel(true);
	      }

	      //store the properties
	      m_properties.clear();
	      m_properties.putAll(properties);

	      startZigBee(this);

	    } catch (Throwable t) {
	        s_logger.error("Unexpected Throwable", t);
	      }
	    s_logger.info("dostartZigBee... Done.");
	  }

	  private void openPort() {
		  s_logger.info("openPort...");
	    String port = (String) m_properties.get(SERIAL_DEVICE_PROP_NAME);

	    if (port == null) {
	      s_logger.info("Port name not configured");
	      return;
	    }

	    int baudRate = Integer.valueOf((String) m_properties.get(SERIAL_BAUDRATE_PROP_NAME));
	    int dataBits = Integer.valueOf((String) m_properties.get(SERIAL_DATA_BITS_PROP_NAME));
	    int stopBits = Integer.valueOf((String) m_properties.get(SERIAL_STOP_BITS_PROP_NAME));
	    String sParity = (String) m_properties.get(SERIAL_PARITY_PROP_NAME);
	    int parity = CommURI.PARITY_NONE;

	    if (sParity.equals("none")) {
	      parity = CommURI.PARITY_NONE;
	    } else if (sParity.equals("odd")) {
	        parity = CommURI.PARITY_ODD;
	    } else if (sParity.equals("even")) {
	        parity = CommURI.PARITY_EVEN;
	    }

	    String uri = new CommURI.Builder(port)
	    .withBaudRate(baudRate)
	    .withDataBits(dataBits)
	    .withStopBits(stopBits)
	    .withParity(parity)
	    .withTimeout(2000)
	    .build().toString();

	    try {
	      m_commConnection = (CommConnection) m_connectionFactory.createConnection(uri, 1, false);
	      m_commIs = m_commConnection.openInputStream();
	      m_commOs = m_commConnection.openOutputStream();
	      s_logger.info(port+" open");
	    } catch (IOException e) {
	      s_logger.error("Failed to open port " + port, e);
	      cleanupPort();
	    }
	    s_logger.info("openPort... Done.");
	  }

	  private void cleanupPort() {
		  s_logger.info("cleanupPort...");

	    if (m_commIs != null) {
	      try {
	        s_logger.info("Closing port input stream...");
	        m_commIs.close();
	        s_logger.info("Closed port input stream");
	      } catch (IOException e) {
	          s_logger.error("Cannot close port input stream", e);
	      }
	      m_commIs = null;
	    }

	    if (m_commOs != null) {
	      try {
	        s_logger.info("Closing port output stream...");
	        m_commOs.flush();
	        m_commOs.close();
	        s_logger.info("Closed port output stream");
	      } catch (IOException e) {
	          s_logger.error("Cannot close port output stream", e);
	      }
	      m_commOs = null;
	    }

	    if (m_commConnection != null) {
	      try {
	        s_logger.info("Closing port...");
	        m_commConnection.close();
	        s_logger.info("Closed port");
	      } catch (IOException e) {
	          s_logger.error("Cannot close port", e);
	      }
	      m_commConnection = null;
	    }
	    s_logger.info("cleanupPort... Done.");
	  }

	private void closePort() {
		  s_logger.info("closePort...");
		  cleanupPort();
		  s_logger.info("closePort... Done.");
	  }

	@Override
	public void close() {
		s_logger.debug("ZigBeePort close...");
		this.closePort();
		s_logger.debug("ZigBeePort close... Done.");
		
	}

	@Override
	public InputStream getInputStream() {
		return m_commIs;
	}

	@Override
	public OutputStream getOutputStream() {
		return m_commOs;
	}

	@Override
	public boolean open() {
		s_logger.debug("ZigBeePort open...");
		try {
			this.openPort();
			s_logger.debug("ZigBeePort open... Done.");
			return true;
	    } catch (Exception e) {
	    	s_logger.error("Error...", e);
	        return false;
	    }
	}
    /**
     * Interface for console commands.
     */
    private interface ConsoleCommand {
        /**
         * Get command description.
         * @return the command description
         */
        String getDescription();

        /**
         * Get command syntax.
         * @return the command syntax
         */
        String getSyntax();

        /**
         *
         * @param zigbeeApi
         * @param args
         * @return
         */
        boolean process(final ZigBeeApi zigbeeApi, final String[] args);
    }
    private class JoinCommand implements ConsoleCommand {
        /**
         * {@inheritDoc}
         */
        public String getDescription() {
            return "Enable or diable network join.";
        }
        /**
         * {@inheritDoc}
         */
        public String getSyntax() {
            return "join [enable|disable]";
        }
        /**
         * {@inheritDoc}
         */
        public boolean process(final ZigBeeApi zigbeeApi, final String[] args) {
            if (args.length != 2) {
                return false;
            }
            
            boolean join = false;
            if(args[1].toLowerCase().startsWith("e")) {
            	join = true;
            }

            if (!zigbeeApi.permitJoin(join)) {
                if (join) {
                    print("ZigBee API permit join enable ... [FAIL]");
                } else {
                    print("ZigBee API permit join disable ... [FAIL]");
                }
            } else {
                if (join) {
                    print("ZigBee API permit join enable ... [OK]");
                } else {
                    print("ZigBee API permit join disable ... [OK]");
                }
            }
            return true;
        }
    }
    
    private void print(final String line) {
//      System.out.println("\r" + line);
    	s_logger.debug(line);
  }
    
    private void processInputLine(final ZigBeeApi zigbeeApi, final String inputLine) {
        if (inputLine.length() == 0) {
            return;
        }
        final String[] args = inputLine.split(" ");
        try {
            if (commands.containsKey(args[0])) {
                executeCommand(zigbeeApi, args[0], args);
                return;
            } else {
                for (final String command : commands.keySet()) {
                    if (command.charAt(0) == inputLine.charAt(0)) {
                        executeCommand(zigbeeApi, command, args);
                        return;
                    }
                }
                print("Uknown command. Use 'help' command to list available commands.");
            }
        } catch (final Exception e) {
            print("Exception in command execution: ");
            e.printStackTrace();
        }
    }
    
    /**
     * Executes command.
     * @param zigbeeApi the ZigBee API
     * @param command the command
     * @param args the arguments including the command
     */
    private void executeCommand(final ZigBeeApi zigbeeApi, final String command, final String[] args) {
        final ConsoleCommand consoleCommand = commands.get(command);
        if (!consoleCommand.process(zigbeeApi, args)) {
            print(consoleCommand.getSyntax());
        }
    }
    
    /**
     * Prints list of devices to console.
     */
    private class ListCommand implements ConsoleCommand {
        /**
         * {@inheritDoc}
         */
        public String getDescription() {
            return "Lists devices.";
        }
        /**
         * {@inheritDoc}
         */
        public String getSyntax() {
            return "list";
        }
        /**
         * {@inheritDoc}
         */
        public boolean process(final ZigBeeApi zigbeeApi, final String[] args) {
            final List<Device> devices = zigbeeApi.getDevices();
            for (int i = 0; i < devices.size(); i++) {
                final Device device = devices.get(i);
                print(i + ") " + device.getEndpointId() +
                		" [" + device.getNetworkAddress() + "]" +
                		" : " + device.getDeviceType());
            }
            return true;
        }
    }
    
    /**
     * Switches a device on.
     */
    private class OnCommand implements ConsoleCommand {
        /**
         * {@inheritDoc}
         */
        public String getDescription() {
            return "Switches device on.";
        }
        /**
         * {@inheritDoc}
         */
        public String getSyntax() {
            return "on DEVICEID";
        }
        /**
         * {@inheritDoc}
         */
        public boolean process(final ZigBeeApi zigbeeApi, final String[] args) {
            if (args.length != 2) {
                return false;
            }

            final Device device = getDeviceByIndexOrEndpointId(zigbeeApi, args[1]);
            if (device == null) {
                return false;
            }
            final OnOff onOff = device.getCluster(OnOff.class);
            try {
                onOff.on();
            } catch (ZigBeeDeviceException e) {
                e.printStackTrace();
            }

            return true;
        }
    }
    
    /**
     * Switches a device off.
     */
    private class OffCommand implements ConsoleCommand {
        /**
         * {@inheritDoc}
         */
        public String getDescription() {
            return "Switches device off.";
        }
        /**
         * {@inheritDoc}
         */
        public String getSyntax() {
            return "off DEVICEID";
        }
        /**
         * {@inheritDoc}
         */
        public boolean process(final ZigBeeApi zigbeeApi, final String[] args) {
            if (args.length != 2) {
                return false;
            }

            final Device device = getDeviceByIndexOrEndpointId(zigbeeApi, args[1]);
            if (device == null) {
                return false;
            }
            final OnOff onOff = device.getCluster(OnOff.class);
            try {
                onOff.off();
            } catch (ZigBeeDeviceException e) {
                e.printStackTrace();
            }

            return true;
        }
    }
    
    /**
     * Prints device information to console.
     */
    private class DescribeCommand implements ConsoleCommand {
        /**
         * {@inheritDoc}
         */
        public String getDescription() {
            return "Describes a device.";
        }
        /**
         * {@inheritDoc}
         */
        public String getSyntax() {
            return "desc DEVICEID";
        }
        /**
         * {@inheritDoc}
         */
        public boolean process(final ZigBeeApi zigbeeApi, final String[] args) {
            if (args.length != 2) {
                return false;
            }

            final Device device = getDeviceByIndexOrEndpointId(zigbeeApi, args[1]);

            if (device == null) {
                return false;
            }

            print("Network Address  : " + device.getNetworkAddress());
            print("Extended Address : " + device.getIeeeAddress());
            print("Endpoint Address : " + device.getEndPointAddress());
            print("Device Profile   : " + ZigBeeApiConstants.getProfileName(device.getProfileId())+ String.format("  (0x%04X)", device.getProfileId()));
            print("Device Category  : " + ZigBeeApiConstants.getCategoryDeviceName(device.getProfileId(), device.getDeviceTypeId()));
            print("Device Type      : " + device.getDeviceType() + String.format("  (0x%04X)", device.getDeviceTypeId()));
            print("Device Version   : " + device.getDeviceVersion());
            print("Implementation   : " + device.getClass().getName());
            print("Input Clusters   : ");
            for (int c : device.getInputClusters()) {
                final Cluster cluster = device.getCluster(c);
                print("                 : " + c + " " + ZigBeeApiConstants.getClusterName(c));
                if (cluster != null) {
                    for (int a = 0; a < cluster.getAttributes().length; a++) {
                        final Attribute attribute = cluster.getAttributes()[a];
                        print("                 :    " + attribute.getId()
                                + " "
                                + "r"
                                + (attribute.isWritable() ? "w" : "-")
                                + (attribute.isReportable() ? "s" : "-")
                                + " "
                                + attribute.getName()
                                + " "
                                + (attribute.getReporter() != null ? "(" +
                                Integer.toString(attribute.getReporter().getReportListenersCount()) + ")" : "")
                                + "  [" + attribute.getZigBeeType() + "]");
                    }
                }
            }
            print("Output Clusters  : ");
            for (int c : device.getOutputClusters()) {
                print("                 : " + c + " " + ZigBeeApiConstants.getClusterName(c));
            }

            return true;
        }
    }
    
    /**
     * Gets device from ZigBee API either with index or endpoint ID
     * @param zigbeeApi the zigbee API
     * @param deviceIdentifier the device identifier
     * @return
     */
    private Device getDeviceByIndexOrEndpointId(ZigBeeApi zigbeeApi, String deviceIdentifier) {
        Device device;
        try {
            device = zigbeeApi.getDevices().get(Integer.parseInt(deviceIdentifier));
        } catch (final Exception e) {
            device = zigbeeApi.getDevice(deviceIdentifier);
        }
        return device;
    }

}
