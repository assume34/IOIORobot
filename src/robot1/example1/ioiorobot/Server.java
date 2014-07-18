package robot1.example1.ioiorobot;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.PulseInput;
import ioio.lib.api.PulseInput.PulseMode;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jwetherell.motion_detection.data.GlobalData;
import com.jwetherell.motion_detection.data.Preferences;
import com.jwetherell.motion_detection.detection.IMotionDetection;
import com.jwetherell.motion_detection.detection.RgbMotionDetection;
import com.jwetherell.motion_detection.image.ImageProcessing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.hardware.Camera.PreviewCallback;
import android.hardware.SensorEventListener;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class Server extends IOIOActivity implements SurfaceHolder.Callback,
		PreviewCallback {
	// Auto
	Boolean _Autocon = false;
	// GPS
	LocationManager _LocationManager;
	PrintWriter outGPS;
	String provider;
	// Connection
	String ip = "0.0.0.0";

	private ServerSocket serverSocket_FOR_CAM = null,
			serverSocket_FOR_TEMP = null, serverSocket_FOR_COMMAND = null,
			serverSocket_FOR_location = null;

	private Socket socket = null, socket_for_temp = null,
			socket_for_command = null, socket_for_location = null;
	// port
	private static int SERVERPORT_FOR_CAM = 1111, SERVERPORT_FOR_TEMP = 1112,
			SERVERPORT_FOR_COMMAND = 1113, SERVERPORT_FOR_location = 1114;
	// Socket clientSocket;
	private BufferedReader input;
	private String read;
	// Interface
	TextView myIp, status, clear, resetGPS;
	ImageView picStatus;
	EditText port, ipaddress;
	Button menu;
	TextView temp;
	TextView Latitude, Longitude;

	// IOIO board
	static boolean _movefw = false, _movebw = false, _movetl = false, _movetr = false,
			check_on_off = false;
	static float cal;
	static String _tempval;
	int cWays, cPosition;
	int[] cCheckDistance = new int[3];

	double _Distance;
	int ioio_command = 0;
	// CAMERA
	Camera mCamera;
	SurfaceView mPreview;
	OutputStream outCam;
	DataOutputStream dosCam;
	ByteArrayOutputStream bosCam;

	boolean connect_state = false, ServerThreadCheck = true,
			DataInputThreadCheck = true;

	// TEMP
	PrintWriter outTemp;

	// _sensor1
	static final AtomicBoolean computing = new AtomicBoolean(false);
	static final float grav[] = new float[3]; // Gravity (a.k.a accelerometer
												// data)
	static final float mag[] = new float[3]; // Magnetic
	static final float gravThreshold = 0.5f;
	static final float magThreshold = 1.0f;
	static SensorManager sensorMgr = null;
	static List<Sensor> sensors = null;
	static Sensor sensorGrav = null;
	static Sensor sensorMag = null;

	static float prevGrav = 0.0f;
	static float prevMag = 0.0f;

	// _Motiondetect2

	static SurfaceView preview = null;
	static SurfaceHolder previewHolder = null;
	static Camera camera = null;
	static boolean inPreview = false;
	static long mReferenceTime = 0;
	static IMotionDetection detector = null;
	static Handler handCam;
	static volatile AtomicBoolean processing = new AtomicBoolean(false);

	static boolean statsMove;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("MAIN", "here");
		try {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().addFlags(
					WindowManager.LayoutParams.FLAG_FULLSCREEN
							| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

			setContentView(R.layout.server);
			_ServerValue();

			mPreview.getHolder().addCallback(this);
			mPreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			// TEST
			detector = new RgbMotionDetection();

			// interface
			myIp.setText("My IP : " + getIP() + "\n" + "Port : "
					+ String.valueOf(SERVERPORT_FOR_CAM) + ","
					+ String.valueOf(SERVERPORT_FOR_TEMP) + ","
					+ String.valueOf(SERVERPORT_FOR_COMMAND) + ","
					+ String.valueOf(SERVERPORT_FOR_location));

			_LocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			Criteria criteria = new Criteria();
			provider = _LocationManager.getBestProvider(criteria, false);
			Location local = _LocationManager.getLastKnownLocation(provider);

			if (local != null) {
				_LocationManager.requestLocationUpdates(provider, 500, 1, test);

				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						Toast.makeText(getBaseContext(),
								"Provider :" + provider, Toast.LENGTH_SHORT)
								.show();
					}
				});
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

		// connect 1
		try {
			if (socket != null) {
				socket.close();
				socket = null;
			}
			if (socket_for_temp != null) {
				socket_for_temp.close();
				socket_for_temp = null;
			}
			if (socket_for_command != null) {
				socket_for_command.close();
				socket_for_command = null;
			}
			if (socket_for_location != null) {
				socket_for_location.close();
				socket_for_location = null;
			}

			if (serverSocket_FOR_CAM != null) {
				serverSocket_FOR_CAM.close();
				serverSocket_FOR_CAM = null;
			}
			if (serverSocket_FOR_TEMP != null) {
				serverSocket_FOR_TEMP.close();
				serverSocket_FOR_TEMP = null;
			}
			if (serverSocket_FOR_COMMAND != null) {
				serverSocket_FOR_COMMAND.close();
				serverSocket_FOR_COMMAND = null;
			}
			if (serverSocket_FOR_location != null) {
				serverSocket_FOR_location.close();
				serverSocket_FOR_location = null;
			}

			new Thread(ServerThread).start();
			// new Thread(_resetGPS).start();
		} catch (IOException e) {
			e.printStackTrace();
		}

		resetGPS.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				try {
					_LocationManager.requestLocationUpdates(provider, 500, 1,
							test);
				} catch (Exception e) {
					// TODO: handle exception
				}

			}
		});

		menu.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				//Intent goMenu1 = new Intent(Server.this, MainActivity.class);
				//startActivity(goMenu1);
				finish();

			}
		});

	clear.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// text.setText("");
				/*try {
					if (socket != null) {
						socket.close();
						socket = null;
					}
					if (socket_for_temp != null) {
						socket_for_temp.close();
						socket_for_temp = null;
					}
					if (socket_for_command != null) {
						socket_for_command.close();
						socket_for_command = null;
					}
					if (socket_for_location != null) {
						socket_for_location.close();
						socket_for_location = null;
					}

					if (serverSocket_FOR_CAM != null) {
						serverSocket_FOR_CAM.close();
						serverSocket_FOR_CAM = null;
					}
					if (serverSocket_FOR_TEMP != null) {
						serverSocket_FOR_TEMP.close();
						serverSocket_FOR_TEMP = null;
					}
					if (serverSocket_FOR_COMMAND != null) {
						serverSocket_FOR_COMMAND.close();
						serverSocket_FOR_COMMAND = null;
					}
					if (serverSocket_FOR_location != null) {
						serverSocket_FOR_location.close();
						serverSocket_FOR_location = null;
					}
					new Thread(ServerThread).start();
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							picStatus.setImageResource(R.drawable.yellow);
							status.setText("Status : Waiting Connect");
						}
					});

				} catch (IOException e) {
					e.printStackTrace();
				}
*/
			}
		});

	}

	/*protected void onStart() {
		super.onStart();
		try {
			sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);

			sensors = sensorMgr.getSensorList(Sensor.TYPE_ACCELEROMETER);
			if (sensors.size() > 0)
				sensorGrav = sensors.get(0);

			sensors = sensorMgr.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
			if (sensors.size() > 0)
				sensorMag = sensors.get(0);

			sensorMgr.registerListener((SensorEventListener) this, sensorGrav,
					SensorManager.SENSOR_DELAY_NORMAL);
			sensorMgr.registerListener((SensorEventListener) this, sensorMag,
					SensorManager.SENSOR_DELAY_NORMAL);
		} catch (Exception ex1) {
			try {
				if (sensorMgr != null) {
					sensorMgr.unregisterListener((SensorEventListener) this,
							sensorGrav);
					sensorMgr.unregisterListener((SensorEventListener) this,
							sensorMag);
					sensorMgr = null;
				}
			} catch (Exception ex2) {
				ex2.printStackTrace();
			}
		}
	};*/

	@Override
	protected void onResume() {
		super.onResume();
		try {
			mCamera = Camera.open();
			mCamera.setDisplayOrientation(90);
			this.registerReceiver(this.WifiReceiver, new IntentFilter(
					ConnectivityManager.CONNECTIVITY_ACTION));

			sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);

			sensors = sensorMgr.getSensorList(Sensor.TYPE_ACCELEROMETER);
			if (sensors.size() > 0)
				sensorGrav = sensors.get(0);

			sensors = sensorMgr.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
			if (sensors.size() > 0)
				sensorMag = sensors.get(0);

			sensorMgr.registerListener((SensorEventListener) this, sensorGrav,
					SensorManager.SENSOR_DELAY_NORMAL);
			sensorMgr.registerListener((SensorEventListener) this, sensorMag,
					SensorManager.SENSOR_DELAY_NORMAL);
		} catch (Exception ex1) {
			try {
				if (sensorMgr != null) {
					sensorMgr.unregisterListener((SensorEventListener) this,
							sensorGrav);
					sensorMgr.unregisterListener((SensorEventListener) this,
							sensorMag);
					sensorMgr = null;
				}
			} catch (Exception ex2) {
				ex2.printStackTrace();
			}
		}
		// _LocationManager.requestLocationUpdates(provider, 400, 1, test);

	}

	/*public void onStop() {
		super.onStop();
		_LocationManager.removeUpdates(test);
		try {
			sensorMgr
					.unregisterListener((SensorEventListener) this, sensorGrav);
			sensorMgr.unregisterListener((SensorEventListener) this, sensorMag);
			sensorMgr = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/

	public void onPause() {
		super.onPause();
		try {
		mCamera.setPreviewCallback(null);
		mCamera.release();
		
			if (socket != null) {
				socket.close();
				socket = null;
			}
			if (socket_for_temp != null) {
				socket_for_temp.close();
				socket_for_temp = null;
			}
			if (socket_for_command != null) {
				socket_for_command.close();
				socket_for_command = null;
			}
			if (socket_for_location != null) {
				socket_for_location.close();
				socket_for_location = null;
			}

			if (serverSocket_FOR_CAM != null) {
				serverSocket_FOR_CAM.close();
				serverSocket_FOR_CAM = null;
			}
			if (serverSocket_FOR_TEMP != null) {
				serverSocket_FOR_TEMP.close();
				serverSocket_FOR_TEMP = null;
			}
			if (serverSocket_FOR_COMMAND != null) {
				serverSocket_FOR_COMMAND.close();
				serverSocket_FOR_COMMAND = null;
			}
			if (serverSocket_FOR_location != null) {
				serverSocket_FOR_location.close();
				serverSocket_FOR_location = null;
			}

			finish();

		} catch (IOException e) {
			e.printStackTrace();
		}
		_LocationManager.removeUpdates(test);
		try {
			sensorMgr
					.unregisterListener((SensorEventListener) this, sensorGrav);
			sensorMgr.unregisterListener((SensorEventListener) this, sensorMag);
			sensorMgr = null;
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		try {

			if (socket != null) {
				socket.close();
				socket = null;
			}
			if (socket_for_temp != null) {
				socket_for_temp.close();
				socket_for_temp = null;
			}
			if (socket_for_command != null) {
				socket_for_command.close();
				socket_for_command = null;
			}
			if (socket_for_location != null) {
				socket_for_location.close();
				socket_for_location = null;
			}

			if (serverSocket_FOR_CAM != null) {
				serverSocket_FOR_CAM.close();
				serverSocket_FOR_CAM = null;
			}
			if (serverSocket_FOR_TEMP != null) {
				serverSocket_FOR_TEMP.close();
				serverSocket_FOR_TEMP = null;
			}
			if (serverSocket_FOR_COMMAND != null) {
				serverSocket_FOR_COMMAND.close();
				serverSocket_FOR_COMMAND = null;
			}
			if (serverSocket_FOR_location != null) {
				serverSocket_FOR_location.close();
				serverSocket_FOR_location = null;
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		unregisterReceiver(this.WifiReceiver);

	}

	/** ################# GPS ################## */
	LocationListener test = new LocationListener() {

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onLocationChanged(Location location) {
			// TODO Auto-generated method stub
			try {
			Latitude.setText("Latitude "
					+ String.format("%.7f", location.getLatitude()));
			Longitude.setText("Longitude "
					+ String.format("%.7f", location.getLongitude()));

			

				String outG = "Latitude "
						+ String.format("%.7f", location.getLatitude()) + ","
						+ "Longitude "
						+ String.format("%.7f", location.getLongitude());
				outGPS = new PrintWriter(new BufferedWriter(
						new OutputStreamWriter(
								socket_for_location.getOutputStream())), true);
				outGPS.println(outG);
				outGPS.flush();

			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	};

	/** ################# CAMERA OUTPUTSTREAM ################## */
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		Log.d("CameraSystem", "surfaceChanged");
		try {
		Camera.Parameters params = mCamera.getParameters();
		List<Camera.Size> previewSize = params.getSupportedPreviewSizes();
		// List<Camera.Size> pictureSize = params.getSupportedPictureSizes();
		// params.setPictureSize(pictureSize.get(0).width,pictureSize.get(0).height);
		params.setPreviewSize(previewSize.get(0).width,
				previewSize.get(0).height);
		params.setJpegQuality(100);
		mCamera.setParameters(params);
		mCamera.setPreviewCallback(this);
		
			mCamera.setPreviewDisplay(mPreview.getHolder());
			mCamera.startPreview();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void surfaceCreated(SurfaceHolder arg0) {
	}

	public void surfaceDestroyed(SurfaceHolder arg0) {
	}

	public void onPreviewFrame(final byte[] arg0, Camera arg1) {

	Camera.Size MotionDetectSize = arg1.getParameters().getPreviewSize();
		if (statsMove) {
			if (!GlobalData.isPhoneInMotion()) {
				DetectionThread thread = new DetectionThread(arg0,
						MotionDetectSize.width, MotionDetectSize.height);
				thread.start();
			}
		}
		if (arg0 != null) {

			Bitmap bitmap;
			int w = arg1.getParameters().getPreviewSize().width;
			int h = arg1.getParameters().getPreviewSize().height;
			int[] rgbs = new int[w * h];
			
			if ((socket != null) && (socket_for_command != null)) {

				if (arg0 != null && connect_state) {

					decodeYUV420(rgbs, arg0, w, h);
					bitmap = Bitmap.createBitmap(rgbs, w, h, Config.ARGB_8888);

					bosCam = new ByteArrayOutputStream();
					bitmap.compress(CompressFormat.JPEG, 50, bosCam);// quality
					byte[] data = bosCam.toByteArray();
					@SuppressWarnings("unused")
					int asd=data.length;
					try {
						dosCam.writeInt(data.length);
						dosCam.write(data);
						//dosCam.flush();
					} catch (IOException e) {
						connect_state = false;
						// check_on_off = false;
					}
					// ReadysendTemp = true;
				}

			}

		}
		// });
		// }
	}

	public void decodeYUV420(int[] rgb, byte[] yuv420, int width, int height) {
		try {
			final int frameSize = width * height;

			for (int j = 0, yp = 0; j < height; j++) {
				int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
				for (int i = 0; i < width; i++, yp++) {
					int y = (0xff & ((int) yuv420[yp])) - 16;
					if (y < 0)
						y = 0;
					if ((i & 1) == 0) {
						v = (0xff & yuv420[uvp++]) - 128;
						u = (0xff & yuv420[uvp++]) - 128;
					}

					int y1192 = 1192 * y;
					int r = (y1192 + 1634 * v);
					int g = (y1192 - 833 * v - 400 * u);
					int b = (y1192 + 2066 * u);

					if (r < 0)
						r = 0;
					else if (r > 262143)
						r = 262143;
					if (g < 0)
						g = 0;
					else if (g > 262143)
						g = 262143;
					if (b < 0)
						b = 0;
					else if (b > 262143)
						b = 262143;

					rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000)
							| ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	/** ################# OPEN SOCKET SERVER ################## */
	Runnable ServerThread = new Runnable() {
		public void run() {
			ServerThreadCheck = true;

			try {
				serverSocket_FOR_CAM = new ServerSocket(SERVERPORT_FOR_CAM);
				serverSocket_FOR_TEMP = new ServerSocket(SERVERPORT_FOR_TEMP);
				serverSocket_FOR_COMMAND = new ServerSocket(
						SERVERPORT_FOR_COMMAND);
				serverSocket_FOR_location = new ServerSocket(
						SERVERPORT_FOR_location);
			} catch (IOException e) {
				ServerThreadCheck = false;

			}
			while (ServerThreadCheck) {
				try {
					socket = serverSocket_FOR_CAM.accept();
					// socket_for_temp = serverSocket_FOR_TEMP.accept();
					socket_for_command = serverSocket_FOR_COMMAND.accept();
					socket_for_location = serverSocket_FOR_location.accept();
					//
					outCam = socket.getOutputStream();
					dosCam = new DataOutputStream(outCam);
					connect_state = true;

					_LocationManager.requestLocationUpdates(provider, 500, 1,
							test);

					new Thread(DataInputThread).start();
					new Thread(DataTemp).start();
					// new Thread(_resetGPS).start();
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							picStatus.setImageResource(R.drawable.green);
							status.setText("Status : Connected");
						}
					});

				} catch (IOException e) {
					break;

				}
			}

		}

	};

	/** ################# update GPS ################## */
	/*
	 * Runnable _resetGPS = new Runnable() {
	 * 
	 * public void run() { while (true) { try {
	 * _LocationManager.requestLocationUpdates(provider, 500, 1, test); } catch
	 * (Exception e) { // TODO: handle exception runOnUiThread(new Runnable() {
	 * 
	 * @Override public void run() { // TODO Auto-generated method stub
	 * Toast.makeText(getBaseContext(), "error", Toast.LENGTH_SHORT).show(); }
	 * }); break; } }
	 * 
	 * }
	 * 
	 * };
	 */

	/** ################# Temp accept ################## */
	Runnable DataTemp = new Runnable() {

		public void run() {

			while (true) {

				try {

					socket_for_temp = serverSocket_FOR_TEMP.accept();

				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					break;
				}

			}
		}

	};

	/** ################# DataInput(CHECK COMMAND IOIO) ################## */
	Runnable DataInputThread = new Runnable() {

		public void run() {
			DataInputThreadCheck = true;
			try {
				input = new BufferedReader(new InputStreamReader(
						socket_for_command.getInputStream()));
			} catch (IOException e) {
				e.printStackTrace();
				DataInputThreadCheck = false;
			}

			while (DataInputThreadCheck) {
				try {
					if (socket_for_command.isConnected()) {
						// read=null;
						read = input.readLine();
					} else {
						// connect_state = false;
						read = null;

					}
					//
					if (read != null) {

						// forward
						if (read.equals("fw")) {
							_movefw = true;
							ioio_command = 1;
							statsMove=false;
						} else if (read.equals("wf")) {
							_movefw = false;
							ioio_command = 0;
							//statsMove=true;
						}
						// back ward
						else if (read.equals("bw")) {
							_movebw = true;
							ioio_command = 2;
							statsMove=false;
						} else if (read.equals("wb")) {
							_movebw = false;
							ioio_command = 0;
							statsMove=true;
						}
						// turn left
						else if (read.equals("tl")) {
							_movetl = true;
							ioio_command = 3;
							statsMove=false;
						} else if (read.equals("lt")) {
							_movetl = false;
							ioio_command = 0;
							//statsMove=true;
						}
						// turn right
						else if (read.equals("tr")) {
							_movetr = true;
							ioio_command = 4;
							statsMove=false;
						} else if (read.equals("rt")) {
							_movetr = false;
							ioio_command = 0;
							//statsMove=true;
						}
						// temp on off
						else if (read.equals("tempOn")) {
							check_on_off = true;
						} else if (read.equals("tempOff")) {
							check_on_off = false;
						}
						// auto control
						else if (read.equals("auto_on")) {
							_Autocon = true;
						} else if (read.equals("auto_off")) {
							_Autocon = false;
						}

					} else {
						// dis command
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								picStatus.setImageResource(R.drawable.yellow);
								status.setText("Status : Waiting Connect");
								temp.setText("");
							}
						});
						ioio_command = 0;
						check_on_off = false;
						_Autocon = false;
						statsMove=false;
					}
				} catch (IOException e) {
					e.printStackTrace();
					break;
				} catch (Exception e) {
				}
			}

		}
	};

	/** ################# SHOW MY IP ADDRESS ################## */
	public String getIP() {
		
			WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			int ipAddress = wifiInfo.getIpAddress();
			String ip = (ipAddress & 0xFF) + "." + ((ipAddress >> 8) & 0xFF)
					+ "." + ((ipAddress >> 16) & 0xFF) + "."
					+ ((ipAddress >> 24) & 0xFF);

		return ip;
	}

	/** ################# CHECK STATUS CONECTION ################## */
	private BroadcastReceiver WifiReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context arg0, Intent ttt) {
			try {
				ConnectivityManager myConnect = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
				NetworkInfo MyNetworkInfo = myConnect
						.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

				if (MyNetworkInfo.isConnected()) {
					// dis wifi
					picStatus.setImageResource(R.drawable.yellow);
					status.setText("Status : Waiting Connect");
					myIp.setText("My IP : " + getIP() + "\n" + "Port : "
							+ String.valueOf(SERVERPORT_FOR_CAM) + ","
							+ String.valueOf(SERVERPORT_FOR_TEMP) + ","
							+ String.valueOf(SERVERPORT_FOR_COMMAND) + ","
							+ String.valueOf(SERVERPORT_FOR_location));

				}
				if (!MyNetworkInfo.isConnected()) {
					myIp.setText("My IP : " + getIP() + "\n" + "Port : "
							+ String.valueOf(SERVERPORT_FOR_CAM) + ","
							+ String.valueOf(SERVERPORT_FOR_TEMP) + ","
							+ String.valueOf(SERVERPORT_FOR_COMMAND) + ","
							+ String.valueOf(SERVERPORT_FOR_location));
					picStatus.setImageResource(R.drawable.red);
					status.setText("Status : Disconnect");

				}

			} catch (Exception e) {
				// TODO: handle exception
			}
		}

	};

	/** ###################### IOIO BOARD ############################### */
	class MoveRobot extends BaseIOIOLooper {
		PwmOutput Mleft, Mright;
		AnalogInput _Temp_pin44;
		DigitalOutput _Trigger_pin34;
		PulseInput _Echo_pin35;

		@Override
		protected void setup() throws ConnectionLostException {
			Mleft = ioio_.openPwmOutput(1, 40);
			Mright = ioio_.openPwmOutput(2, 40);
			_Temp_pin44 = ioio_.openAnalogInput(44);
			_Trigger_pin34 = ioio_.openDigitalOutput(34, false);
			_Echo_pin35 = ioio_.openPulseInput(35, PulseMode.POSITIVE);
			// _Autocon = true;
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Toast.makeText(getApplicationContext(), "IOIO Connected",
							Toast.LENGTH_SHORT).show();
				}
			});

		}

		@Override
		public void loop() throws ConnectionLostException, InterruptedException {
			try {
				// if (_Autocon != true) {
				Thread.sleep(20);
				switch (ioio_command) {
				case 1:// fw
					Mleft.setPulseWidth(1000);
					Mright.setPulseWidth(2000);
					break;
				case 2:// bw
					Mleft.setPulseWidth(2000);
					Mright.setPulseWidth(1000);
					break;
				case 3:// tl
					Mleft.setPulseWidth(1000);
					Mright.setPulseWidth(1000);
					break;
				case 4:// tr
					Mright.setPulseWidth(2000);
					Mleft.setPulseWidth(2000);
					break;

				case 0:
					Mleft.setPulseWidth(0);
					Mright.setPulseWidth(0);
					break;
				}
				// }
				/*
				 * if (_Autocon == true) { Thread.sleep(10); cCheckDistance[0] =
				 * (int) wut(); Thread.sleep(2000); aTurnLeft();
				 * Thread.sleep(2000); aStop(); cCheckDistance[1] = (int) wut();
				 * Thread.sleep(2000); aTurnRight(); Thread.sleep(4000);
				 * aStop(); cCheckDistance[2] = (int) wut(); Thread.sleep(2000);
				 * aTurnLeft(); Thread.sleep(2000); aStop(); cPosition =
				 * _check();
				 * 
				 * if (cPosition > 10) { if (cCheckDistance[0] == cPosition) {
				 * aForward(); new Thread(move_move).start(); } if
				 * (cCheckDistance[1] == cPosition) { aTurnLeft();
				 * Thread.sleep(2000); aStop(); aForward(); new
				 * Thread(move_move).start(); } if (cCheckDistance[2] ==
				 * cPosition) { aTurnRight(); Thread.sleep(2000); aStop();
				 * aForward(); new Thread(move_move).start(); } } else {
				 * aBackward(); Thread.sleep(2000); aStop(); }
				 * 
				 * }
				 */

				if (check_on_off == true) {
				/*	new Thread(_sendTemp).start();
					Thread.sleep(100);*/
					Thread.sleep(100);
					if (socket_for_temp.isConnected()) {
						try {
							cal = _Temp_pin44.getVoltage();
							_tempval = sum(cal);

							try {

								String command = _tempval;
								outTemp = new PrintWriter(new BufferedWriter(
										new OutputStreamWriter(
												socket_for_temp
														.getOutputStream())),
										true);
								outTemp.println(command);
								outTemp.flush();

							} catch (UnknownHostException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							} catch (Exception e) {
								e.printStackTrace();
							}

						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (ConnectionLostException e) {
							e.printStackTrace();
						}
						runOnUiThread(new Runnable() {

							public void run() {
								temp.setText(_tempval);
							}

						});
					} else {
						runOnUiThread(new Runnable() {

							public void run() {
								temp.setText("");
							}

						});
					}

				}
				if (check_on_off != true) {
					runOnUiThread(new Runnable() {

						public void run() {
							temp.setText("");
						}

					});
				}
			} catch (Exception e) {
				// Thread.sleep(10);
				Mleft.setPulseWidth(0);
				Mright.setPulseWidth(0);
				Toast.makeText(getApplicationContext(),
						"Problem something about ioio.", Toast.LENGTH_SHORT)
						.show();
			}

		}

		public int _check() {
			// TODO Auto-generated method stub
			if (cCheckDistance[0] > cCheckDistance[1]) {
				cWays = cCheckDistance[0];
			}
			if (cCheckDistance[1] > cCheckDistance[0]) {
				cWays = cCheckDistance[1];
			}
			if (cCheckDistance[2] > cWays) {
				cWays = cCheckDistance[2];
			}
			return cWays;
		}

		public void aStop() throws ConnectionLostException {
			// TODO Auto-generated method stub
			Mleft.setPulseWidth(0);
			Mright.setPulseWidth(0);
		}

		public void aBackward() throws ConnectionLostException {
			// TODO Auto-generated method stub
			Mleft.setPulseWidth(2000);
			Mright.setPulseWidth(1000);
		}

		public void aForward() throws ConnectionLostException {
			// TODO Auto-generated method stub
			Mleft.setPulseWidth(1000);
			Mright.setPulseWidth(2000);
		}

		public void aTurnRight() throws ConnectionLostException {
			// TODO Auto-generated method stub
			Mleft.setPulseWidth(2000);
			Mright.setPulseWidth(2000);
		}

		public void aTurnLeft() throws ConnectionLostException {
			// TODO Auto-generated method stub
			Mleft.setPulseWidth(1000);
			Mright.setPulseWidth(1000);
		}

		public double wut() throws ConnectionLostException,
				InterruptedException {
			// TODO Auto-generated method stub
			_Trigger_pin34.write(false);
			// TimeUnit.MICROSECONDS.sleep(2);
			Thread.sleep((long) 0.002);
			_Trigger_pin34.write(true);
			// TimeUnit.MICROSECONDS.sleep(10);
			Thread.sleep((long) 0.01);
			_Trigger_pin34.write(false);
			// Centimeters
			_Distance = (_Echo_pin35.getDuration() * 1000000) / 58;
			return _Distance;
			// Inches
			// DistanceOutput = (UltraSonicEcho.getDuration() * 1000000) / 148;
		}

		public String sum(float a) {
			float sum1 = ((cal * 1000) - 2) / 20;
			float sum2 = ((cal * 1000) + 2) / 20;
			_tempval = String.format("Temperature : +%.2f,-%.2f", sum2, sum1);
			return (String) _tempval;
		}

		Runnable _sendTemp = new Runnable() {

			public void run() {

				try {
					Thread.sleep(10);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				if (socket_for_temp.isConnected()) {
					try {
						cal = _Temp_pin44.getVoltage();
						_tempval = sum(cal);

						try {

							String command = _tempval;
							outTemp = new PrintWriter(
									new BufferedWriter(new OutputStreamWriter(
											socket_for_temp.getOutputStream())),
									true);
							outTemp.println(command);
							outTemp.flush();

						} catch (UnknownHostException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						} catch (Exception e) {
							e.printStackTrace();
						}

					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ConnectionLostException e) {
						e.printStackTrace();
					}
					runOnUiThread(new Runnable() {

						public void run() {
							temp.setText(_tempval);
						}

					}); 
				} else {
					runOnUiThread(new Runnable() {

						public void run() {
							temp.setText("");
						}

					});
				}

			}

		};
		Runnable move_move = new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				while (true) {
					try {

						_Trigger_pin34.write(false);
						Thread.sleep((long) 0.002);
						_Trigger_pin34.write(true);
						Thread.sleep((long) 0.01);
						_Trigger_pin34.write(false);
						// For Centimeters
						_Distance = (_Echo_pin35.getDuration() * 1000000) / 58;
						if (_Distance < 10) {
							Mleft.setPulseWidth(0);
							Mright.setPulseWidth(0);
							break;
						}
					} catch (ConnectionLostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
	}

	protected IOIOLooper createIOIOLooper() {
		return new MoveRobot();
	}

	/** ################# SET VALUE ################## */
	private void _ServerValue() {
		myIp = (TextView) findViewById(R.id.myip);
		status = (TextView) findViewById(R.id.status);
		picStatus = (ImageView) findViewById(R.id.picStatus);
		menu = (Button) findViewById(R.id.menu);
		clear = (TextView) findViewById(R.id.textView2);
		Latitude = (TextView) findViewById(R.id.textView1);
		Longitude = (TextView) findViewById(R.id.textView5);
		resetGPS = (TextView) findViewById(R.id.textView3);
		mPreview = (SurfaceView) findViewById(R.id.surfaceView1);
		temp = (TextView) findViewById(R.id.textView4);

	}

	SensorEventListener xxx = new SensorEventListener() {

		@Override
		public void onSensorChanged(SensorEvent event) {
			// TODO Auto-generated method stub
			try {

				if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
					grav[0] = event.values[0];
					grav[1] = event.values[1];
					grav[2] = event.values[2];
				} else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
					mag[0] = event.values[0];
					mag[1] = event.values[1];
					mag[2] = event.values[2];
				}

				float gravity = grav[0] + grav[1] + grav[2];
				float magnetic = mag[0] + mag[1] + mag[2];

				float gravDiff = Math.abs(gravity - prevGrav);
				float magDiff = Math.abs(magnetic - prevMag);
				Log.i("SensorsActivity", "gravDiff=" + gravDiff + " magDiff="
						+ magDiff);

				if ((Float.compare(prevGrav, 0.0f) != 0 && Float.compare(
						prevMag, 0.0f) != 0)
						&& (gravDiff > gravThreshold || magDiff > magThreshold)) {
					GlobalData.setPhoneInMotion(true);
				} else {
					GlobalData.setPhoneInMotion(false);
				}

				prevGrav = gravity;
				prevMag = magnetic;

				computing.set(false);
			} catch (Exception e) {
				// TODO: handle exception
			}

		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
			try {

				if (sensor == null)
					throw new NullPointerException();

				if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD
						&& accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
					Log.e("SensorsActivity", "Compass data unreliable");
				}

			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		
	};

	private static final class DetectionThread extends Thread {

		private byte[] data;
		private int width;
		private int height;

		public DetectionThread(byte[] data, int width, int height) {
			this.data = data;
			this.width = width;
			this.height = height;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void run() {
			if (!processing.compareAndSet(false, true))
				return;

			// Log.d(TAG, "BEGIN PROCESSING...");
			try {
				// Previous frame
				int[] pre = null;
				if (Preferences.SAVE_PREVIOUS)
					pre = detector.getPrevious();

				// Current frame (with changes)
				// long bConversion = System.currentTimeMillis();
				int[] img = null;
				if (Preferences.USE_RGB) {
					img = ImageProcessing.decodeYUV420SPtoRGB(data, width,
							height);
				} else {
					img = ImageProcessing.decodeYUV420SPtoLuma(data, width,
							height);
				}
				// long aConversion = System.currentTimeMillis();
				// Log.d(TAG, "Converstion="+(aConversion-bConversion));

				// Current frame (without changes)
				int[] org = null;
				if (Preferences.SAVE_ORIGINAL && img != null)
					org = img.clone();

				if (img != null && detector.detect(img, width, height)) {
					// The delay is necessary to avoid taking a picture while in
					// the
					// middle of taking another. This problem can causes some
					// phones
					// to reboot.
					long now = System.currentTimeMillis();
					if (now > (mReferenceTime + Preferences.PICTURE_DELAY)) {
						mReferenceTime = now;

						Bitmap previous = null;
						if (Preferences.SAVE_PREVIOUS && pre != null) {
							if (Preferences.USE_RGB) {
								previous = ImageProcessing.rgbToBitmap(pre,
										width, height);
							} else {
								previous = ImageProcessing.lumaToGreyscale(pre,
										width, height);
							}
						}

						Bitmap original = null;
						if (Preferences.SAVE_ORIGINAL && org != null) {
							if (Preferences.USE_RGB) {
								original = ImageProcessing.rgbToBitmap(org,
										width, height);
							} else {
								original = ImageProcessing.lumaToGreyscale(org,
										width, height);
							}
						}
						Bitmap bitmap = null;
						if (Preferences.SAVE_CHANGES) {
							if (Preferences.USE_RGB) {
								bitmap = ImageProcessing.rgbToBitmap(img,
										width, height);

							} else {
								bitmap = ImageProcessing.lumaToGreyscale(img,
										width, height);

							}

						}

						Looper.prepare();
						// new SavePhotoTask().execute(previous,
						// original,bitmap);
						new SavePhotoTask().execute(original, bitmap);

					}

				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				processing.set(false);
			}
			// Log.d(TAG, "END PROCESSING...");

			processing.set(false);
		}
	};

	private static final class SavePhotoTask extends
			AsyncTask<Bitmap, Integer, Integer> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected Integer doInBackground(Bitmap... data) {
			try {
				for (int i = 0; i < data.length; i++) {
					final Bitmap bitmap = data[i];

					String name = String.valueOf(System.currentTimeMillis());

					if (bitmap != null)
						save(name, bitmap);
				}

			} catch (Exception e) {
				// TODO: handle exception
			}
			return 1;
		}

		private void save(String name, Bitmap bitmap) {
			try {
				File myDir = new File(Environment.getExternalStorageDirectory()
						+ "/AAA", name + ".jpg");

				if (myDir.exists())
					myDir.delete();

				try {
					FileOutputStream fos = new FileOutputStream(myDir.getPath());
					bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
					fos.close();
				} catch (java.io.IOException e) {
					Log.e("PictureDemo", "Exception in photoCallback", e);
				}
			} catch (Exception e) {
				// TODO: handle exception
			}

		}
	}
}
