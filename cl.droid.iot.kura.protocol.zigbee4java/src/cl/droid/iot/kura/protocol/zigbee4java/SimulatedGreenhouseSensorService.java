package cl.droid.iot.kura.protocol.zigbee4java;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.bubblecloud.zigbee.ZigBeeApi;
import org.bubblecloud.zigbee.api.Device;
import org.bubblecloud.zigbee.api.cluster.Cluster;
import org.bubblecloud.zigbee.api.cluster.impl.api.core.Attribute;
import org.bubblecloud.zigbee.api.cluster.impl.api.core.ZigBeeClusterException;
import org.eclipse.iot.greenhouse.sensors.SensorChangedListener;
import org.eclipse.iot.greenhouse.sensors.SensorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimulatedGreenhouseSensorService extends SerialExample implements SensorService {
	
	private static final Logger s_logger = LoggerFactory.getLogger(SimulatedGreenhouseSensorService.class);
	private static final String APP_ID = "cl.droid.iot.kura.protocol.zigbee4java.SimulatedGreenhouseSensorService";
	private ScheduledThreadPoolExecutor _scheduledThreadPoolExecutor;
	private ScheduledFuture<?> _handle;
	
	private List<SensorChangedListener> _listeners = new CopyOnWriteArrayList<SensorChangedListener>();
	
	float _temperature = 20;
	String _lightState = "on";
	
	public SimulatedGreenhouseSensorService() {
		super();
		s_logger.info("Bundle " + APP_ID + " has started!");
		_scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);

		_handle = _scheduledThreadPoolExecutor.scheduleAtFixedRate(
				new Runnable() {
					@Override
					public void run() {
						print("run");
						String device = "0";
						String cusler;
						String attribute = "0";
						String sensorName;
//						1026 TemperatureMeasurement
//						1029 Relative Humidity Measurement
//						2820 ElectricalMeasurement
						try {
							cusler = "1026";
							sensorName = "temperature";
							final ConsoleCommand consoleCommand = new ReadCommand();
							 String[] args = {"read", device, cusler, attribute, sensorName};
							if (!consoleCommand.process(zigbeeApi, args)) {
						            print(consoleCommand.getSyntax());
						    }
						} catch (Exception e) {
			                print("Failed to process temperature.");
			                e.printStackTrace();
			            }
						try {
							cusler = "1029";
							sensorName = "relativehumidity";
							final ConsoleCommand consoleCommand = new ReadCommand();
							 String[] args = {"read", device, cusler, attribute, sensorName};
							if (!consoleCommand.process(zigbeeApi, args)) {
						            print(consoleCommand.getSyntax());
						    }
						} catch (Exception e) {
			                print("Failed to process relativehumidity.");
			                e.printStackTrace();
			            }
						try {
							cusler = "2820";
							sensorName = "moisture";
							final ConsoleCommand consoleCommand = new ReadCommand();
							 String[] args = {"read", device, cusler, attribute, sensorName};
							if (!consoleCommand.process(zigbeeApi, args)) {
						            print(consoleCommand.getSyntax());
						    }
						} catch (Exception e) {
			                print("Failed to process moisture.");
			                e.printStackTrace();
			            }

					}
				}, 0, 60000, TimeUnit.MILLISECONDS);

	}

	@Override
	public Object getSensorValue(String sensorName) throws NoSuchSensorOrActuatorException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setActuatorValue(String actuatorName, Object value) throws NoSuchSensorOrActuatorException {
		// TODO Auto-generated method stub
		
	}
	
	public void addSensorChangedListener(SensorChangedListener listener) {
		_listeners.add(listener);
	}

	public void removeSensorChangedListener(SensorChangedListener listener) {
		_listeners.remove(listener);
	}
	
	private void notifyListeners(String sensorName, Object newValue) {
		for (SensorChangedListener listener : _listeners) {
			listener.sensorChanged(sensorName, newValue);
		}
	}
	
    private void print(final String line) {
    	s_logger.debug(line);
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
    
    /**
     * Reads an attribute from a device.
     */
    private class ReadCommand implements ConsoleCommand {
        /**
         * {@inheritDoc}
         */
        public String getDescription() {
            return "Read an attribute.";
        }
        /**
         * {@inheritDoc}
         */
        public String getSyntax() {
            return "read [DEVICE] [CLUSTER] [ATTRIBUTE]";
        }
        /**
         * {@inheritDoc}
         */
        public boolean process(final ZigBeeApi zigbeeApi, final String[] args) {
            if (args.length != 5) {
                return false;
            }

            final int clusterId;
            try {
                clusterId = Integer.parseInt(args[2]);
            } catch (final NumberFormatException e) {
                return false;
            }
            final int attributeId;
            try {
                attributeId = Integer.parseInt(args[3]);
            } catch (final NumberFormatException e) {
                return false;
            }

            final Device device = getDeviceByIndexOrEndpointId(zigbeeApi, args[1]);
            if (device == null) {
                print("Device not found.");
                return false;
            }

            final Cluster cluster = device.getCluster(clusterId);
            if (cluster == null) {
                print("Cluster not found.");
                return false;
            }

            final Attribute attribute = cluster.getAttribute(attributeId);
            if (attribute == null) {
                print("Attribute not found.");
                return false;
            }

            try {
                print(attribute.getName() + "=" + attribute.getValue());
                notifyListeners(args[4], attribute.getValue());
                
            } catch (ZigBeeClusterException e) {
                print("Failed to read attribute.");
                e.printStackTrace();
            }

            return true;
        }
    }

}
