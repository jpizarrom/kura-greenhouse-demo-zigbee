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
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimulatedGreenhouseSensorService extends SerialExample implements SensorService {
	
	private static final Logger s_logger = LoggerFactory.getLogger(SimulatedGreenhouseSensorService.class);
	private static final String APP_ID = "cl.droid.iot.kura.protocol.zigbee4java.SimulatedGreenhouseSensorService";
	private ScheduledThreadPoolExecutor _scheduledThreadPoolExecutor;
	private ScheduledFuture<?> _handle;
	
	private static final String GH_SCHEDULE_PROP_NAME= "gh.schedule_enabled";
	private static final String GH_DEVICE_PROP_NAME= "gh.device";
	
	private List<SensorChangedListener> _listeners = new CopyOnWriteArrayList<SensorChangedListener>();
	
	float _temperature = 20;
	String _lightState = "on";
	
	protected void activateCommands(){
		s_logger.info("Activating activateCommands...");
		super.activateCommands();
		s_logger.info("Activating activateCommands... Done.");
	}
	
	public SimulatedGreenhouseSensorService() {
		super();
		s_logger.info("Bundle " + APP_ID + " has started!");
		_scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);

		_handle = _scheduledThreadPoolExecutor.scheduleAtFixedRate(
				new Runnable() {
					@Override
					public void run() {
						print("run1");
						if (m_properties.containsKey(GH_SCHEDULE_PROP_NAME) && m_properties.containsKey(GH_DEVICE_PROP_NAME) &&(Boolean) m_properties.get(GH_SCHEDULE_PROP_NAME)){
						print("run2");	
						String device = (String) m_properties.get(GH_DEVICE_PROP_NAME);
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

					}
				}, 0, 60000, TimeUnit.MILLISECONDS);

	}

	@Override
	public Object getSensorValue(String sensorName) throws NoSuchSensorOrActuatorException {
		// TODO Auto-generated method stub
		s_logger.info("getSensorValue "+sensorName);
		return null;
	}

	@Override
	public void setActuatorValue(String actuatorName, Object value) throws NoSuchSensorOrActuatorException {
		// TODO Auto-generated method stub
		s_logger.info("setActuatorValue "+actuatorName);
//		JsonObject jsonObject = new JsonParser().parse("{\"name\": \"John\"}").getAsJsonObject();

		if(zigbeeApi!=null && actuatorName.equals("commands"))
		{
			s_logger.info("Update - zigbeeApi not null");
			
			String cmd = null;
//			try {
//				JSONObject obj = new JSONObject((String)value);
//				s_logger.info("JSONObject "+ obj.toString());
//				cmd = obj.getJSONObject("data").getString("cmd");
				cmd = Json.parse((String)value).asObject().get("data").asObject().getString("cmd", null);
//			} catch (JSONException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			if(cmd!=null){
				s_logger.info("cmd = " + cmd);
				processInputLine(zigbeeApi, cmd);
			}else{
				s_logger.info("Update - zigbeeApi cmd null");
			}
		}else{
			s_logger.info("cmd = null");
		}
		
	}
	
	public void addSensorChangedListener(SensorChangedListener listener) {
		s_logger.info("addSensorChangedListener");
		_listeners.add(listener);
	}

	public void removeSensorChangedListener(SensorChangedListener listener) {
		s_logger.info("removeSensorChangedListener");
		_listeners.remove(listener);
	}
	
	protected void notifyListeners(String sensorName, Object newValue) {
		s_logger.info("notifyListeners");
		for (SensorChangedListener listener : _listeners) {
			listener.sensorChanged(sensorName, newValue);
		}
	}
	
    private void print(final String line) {
    	s_logger.debug(line);
  }

}
