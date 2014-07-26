package robot1.example1.ioiorobot;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class Client extends Activity {
	// CONNECTION
	RelativeLayout bg;
	InetAddress serverAddr;
	String readTemp = "Unknow", readGPS = "Unknow";
	String command;
	Handler handCam, handTemp, handGPS;

	int SERVERPORT = 1111, SERVERPORT_TEMP = 1112, SERVERPORT_COMMAND = 1113,
			SERVERPORT_GPS = 1114;
	static String SERVER_IP = "0.0.0.0";
	Socket socket_cam = null, socket_temp = null, socket_gps;
	Socket socket_command;
	// INTERFACE
	ImageView _fw, _bw, _tr, _tl;
	ToggleButton on_off;//,autoCon;
	TextView _temp, _GPS;
	TextView _targetIP;// , _targetPort;
	Button _connect, _menu, _exit;
	// CAMERA
	Bitmap bmp, bmpShow;
	// input
	InputStream input_cam, input_temp, input_gps;
	DataInputStream dis, dis_temp, dis_gps;
	// loop thread
	static Boolean loop_state_cam = true, loop_state_temp = true,
			loop_state_GPS = true;

	Matrix mat;
	ImageView iv;

	// check IP address limit
	String result1, result2;
	// String[] _IPADD;
	int check1, check2, check3, check4;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.client);
		handCam = new Handler();
		handTemp = new Handler();
		handGPS = new Handler();

		setValue();

		_fw.setEnabled(false);
		_bw.setEnabled(false);
		_tl.setEnabled(false);
		_tr.setEnabled(false);
		on_off.setEnabled(false);
		
		bg.setBackgroundColor(Color.argb(100, 240, 135, 124));
		_temp.setText("Temperature : No data");

		
		_connect.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				try {
					_connect.setEnabled(false);
					//on_off.setChecked(false);
					// loop_state_GPS = true;

					if (!(_targetIP.getText().toString().equals(""))) {
						SERVER_IP = _targetIP.getText().toString();
						// cut IP address to Integer
						String[] _IPADD = SERVER_IP.split("\\.");
						if (_IPADD[0] != null && _IPADD[1] != null
								&& _IPADD[2] != null && _IPADD[3] != null) {
							check1 = Integer.parseInt(_IPADD[0]);
							check2 = Integer.parseInt(_IPADD[1]);
							check3 = Integer.parseInt(_IPADD[2]);
							check4 = Integer.parseInt(_IPADD[3]);
							// check IP address I
							if ((check1 > 0) && (check1 < 256) && (check2 > 0)
									&& (check2 < 256) && (check3 > 0)
									&& (check3 < 256) && (check4 > 0)
									&& (check4 < 256)) {

								// check socket
								if (socket_cam != null) {
									socket_cam.close();
									socket_cam = null;
								}
								if (socket_command != null) {
									socket_command.close();
									socket_command = null;
								}
								if (socket_temp != null) {
									socket_temp.close();
									socket_temp = null;
								}
								if (socket_gps != null) {
									socket_gps.close();
									socket_gps = null;
								}

								loop_state_cam = true;
								new Thread(ClientThread).start();

							} else {
								Toast.makeText(getBaseContext(),
										"IP address incorrect.",
										Toast.LENGTH_SHORT).show();
							}
						} else {
							Toast.makeText(getBaseContext(),
									"Please enter IP address.",
									Toast.LENGTH_SHORT).show();
						}
						// I
					} else {
						Toast.makeText(getBaseContext(),
								"Please enter IP address.", Toast.LENGTH_SHORT)
								.show();
					}

				} catch (Exception e) {
					Toast.makeText(getBaseContext(), "Error IP address.",
							Toast.LENGTH_SHORT).show();
				}
			}
		});

		_menu.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent go_Menu = new Intent(getApplicationContext(),
						MainActivity.class);
				startActivity(go_Menu);
				finish();
			}
		});

		_exit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				finish();
			}
		});

		/*autoCon.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// TODO Auto-generated method stub
				if ((isChecked == true)) {
					_fw.setEnabled(false);
					_bw.setEnabled(false);
					_tl.setEnabled(false);
					_tr.setEnabled(false);
					command = "auto_on";
					sendCommand(command);
				} else if ((isChecked == false)) {
					_fw.setEnabled(true);
					_bw.setEnabled(true);
					_tl.setEnabled(true);
					_tr.setEnabled(true);
					command = "auto_off";
					sendCommand(command);
				}
			}
		});*/
		
		on_off.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if ((isChecked == true)) {
					_temp.setText("Temperature : No data");
					loop_state_temp = true;
					if (socket_temp != null) {

						try {
							socket_temp.close();
							socket_temp = null;
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
					new Thread(_DataInputTemp).start();
					on_off.setBackgroundResource(R.drawable.on);
					command = "tempOn";
					sendCommand(command);
				} else if ((isChecked == false)) {
					loop_state_temp = false;
					if (socket_temp != null) {
						// socket_temp = null;

						try {
							socket_temp.close();
							socket_temp = null;
						} catch (IOException e) {
							e.printStackTrace();
						}

					}

					_temp.setText("Temperature : No data");
					on_off.setBackgroundResource(R.drawable.off);
					command = "tempOff";
					sendCommand(command);
				}
			}
		});

		// command
		_fw.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if ((event.getAction() == MotionEvent.ACTION_DOWN)
				// && (socket_command != null)
				) {
					command = null;
					_fw.setImageResource(R.drawable.forward_on);

					command = "fw";
					sendCommand(command);

				} else if ((event.getAction() == MotionEvent.ACTION_UP)
				// && (socket_command != null)
				) {
					command = null;
					_fw.setImageResource(R.drawable.forward_off);

					command = "wf";
					sendCommand(command);
				}

				return true;
			}
		});
		_bw.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if ((event.getAction() == MotionEvent.ACTION_DOWN)
				// && (socket_command != null)
				) {
					command = null;
					_bw.setImageResource(R.drawable.backward_on);

					command = "bw";
					sendCommand(command);

				} else if ((event.getAction() == MotionEvent.ACTION_UP)
				// && (socket_command != null)
				) {
					command = null;
					_bw.setImageResource(R.drawable.backward_off);

					command = "wb";
					sendCommand(command);
				}

				return true;
			}

		});
		_tl.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if ((event.getAction() == MotionEvent.ACTION_DOWN)
				// && (socket_command != null)
				) {
					command = null;
					_tl.setImageResource(R.drawable.left_on);

					command = "tl";
					sendCommand(command);

				} else if ((event.getAction() == MotionEvent.ACTION_UP)
				// && (socket_command != null)
				) {
					command = null;
					_tl.setImageResource(R.drawable.left_off);

					command = "lt";
					sendCommand(command);
				}

				return true;
			}
		});
		_tr.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if ((event.getAction() == MotionEvent.ACTION_DOWN)
				// && (socket_command.isConnected())
				) {
					command = null;
					_tr.setImageResource(R.drawable.right_on);

					command = "tr";
					sendCommand(command);

				}
				if ((event.getAction() == MotionEvent.ACTION_UP)
				// && (socket_command.isConnected())
				) {
					command = null;
					_tr.setImageResource(R.drawable.right_off);

					command = "rt";
					sendCommand(command);
				}

				return true;
			}

		});
	}

	public void sendCommand(String a) {
		if (socket_command != null) {
			try {
				PrintWriter out = new PrintWriter(
						new BufferedWriter(new OutputStreamWriter(
								socket_command.getOutputStream())), true);
				out.println(a);
				out.flush();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (socket_command == null) {
			Toast.makeText(getApplicationContext(), "No connect",
					Toast.LENGTH_SHORT).show();
		}

	}

	@Override
	public void onPause() {
		super.onPause();
		SharedPreferences settings = getSharedPreferences("Pref", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("IP", _targetIP.getText().toString());
		editor.commit();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			if (socket_cam != null) {
				socket_cam.close();
				socket_cam = null;
			}
			if (socket_temp != null) {
				socket_temp.close();
				socket_temp = null;
			}
			if (socket_command != null) {
				socket_command.close();
				socket_command = null;
			}
			if (socket_gps != null) {
				socket_gps.close();
				socket_gps = null;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/** ################# Connection ################## */
	Runnable ClientThread = new Runnable() {

		@Override
		public void run() {

			try {
				// InetAddress
				serverAddr = InetAddress.getByName(SERVER_IP);
				socket_cam = new Socket(serverAddr, SERVERPORT);
				// socket_temp = new Socket(serverAddr, SERVERPORT_TEMP);
				socket_command = new Socket(serverAddr, SERVERPORT_COMMAND);
				socket_gps = new Socket(serverAddr, SERVERPORT_GPS);
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
				socket_cam = null;
				// socket_temp = null;
				socket_command = null;
				socket_gps = null;
			} catch (IOException e1) {
				e1.printStackTrace();
				socket_cam = null;
				// socket_temp = null;
				socket_command = null;
				socket_gps = null;

			}

			if (socket_cam == null) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getApplicationContext(),
								"Connection fail camera.", Toast.LENGTH_SHORT)
								.show();
						_connect.setEnabled(true);

						_fw.setEnabled(false);
						_bw.setEnabled(false);
						_tl.setEnabled(false);
						_tr.setEnabled(false);
						on_off.setEnabled(false);

					}

				});
			}

			if (socket_command == null) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getApplicationContext(),
								"Connection fail command.", Toast.LENGTH_SHORT)
								.show();
						_connect.setEnabled(true);

						_fw.setEnabled(false);
						_bw.setEnabled(false);
						_tl.setEnabled(false);
						_tr.setEnabled(false);
						on_off.setEnabled(false);

					}
				});
			}

			// IF CAN CONNECT
			if ((socket_cam != null) && (socket_command != null)) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						_fw.setEnabled(true);
						_bw.setEnabled(true);
						_tl.setEnabled(true);
						_tr.setEnabled(true);
						on_off.setEnabled(true);
						Toast.makeText(getApplicationContext(),
								"Robot connected.", Toast.LENGTH_SHORT).show();
						bg.setBackgroundColor(Color.argb(100, 64, 240, 35));
						new Thread(_DataInputCam).start();
						new Thread(_DataInputGPS).start();

					}
				});

			}
		}

	};

	/** ################# TEMP Input ################## */
	Runnable _DataInputTemp = new Runnable() {

		@SuppressWarnings("deprecation")
		public void run() {

			try {
				// InetAddress serverAddrr = InetAddress.getByName(SERVER_IP);
				if (socket_temp != null) {
					socket_temp.close();
					socket_temp = null;
				}
				socket_temp = new Socket(serverAddr, SERVERPORT_TEMP);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				socket_temp = null;
			}
			try {
				while (loop_state_temp) {
					// IF TEMP
					// fix com 085-517-4876
					if (socket_temp == null) {
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								_temp.setText("Temperature : No data");
								Toast.makeText(getApplicationContext(),
										"Temperature : No connect Robot.",
										Toast.LENGTH_SHORT).show();
							}
						});
						loop_state_temp = false;
						// break;
					}
					if (socket_temp != null) {

						try {
							input_temp = socket_temp.getInputStream();
							dis_temp = new DataInputStream(input_temp);

							if (socket_temp != null) {
								readTemp = dis_temp.readLine();
								if (readTemp == null) {
									handTemp.post(new Runnable() {

										@Override
										public void run() {
											_temp.setText("Temperature : No data");
										}
									});
									break;
								} else {
									handTemp.post(new Runnable() {

										@Override
										public void run() {
											_temp.setText(readTemp);
										}
									});
								}
							}

						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();

							runOnUiThread(new Runnable() {

								@Override
								public void run() {
									Toast.makeText(getApplicationContext(),
											"Temperature : No data.",
											Toast.LENGTH_SHORT).show();
									_temp.setText("Temperature : No data");
								}
							});

							break;
						}
						/*
						 * try { Thread.sleep(100); } catch
						 * (InterruptedException e1) { // TODO Auto-generated
						 * catch block e1.printStackTrace(); }
						 */
					}
				}

			} catch (Exception e) {

				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						Toast.makeText(getApplicationContext(),
								"Temperature : Thread error.",
								Toast.LENGTH_SHORT).show();
						_temp.setText("Temperature : No data");
					}
				}); // break;

			}

		}
	};

	/** ################# CAMERA Input ################## */
	Runnable _DataInputCam = new Runnable() {

		public void run() {
			try {
				while (loop_state_cam) {

					// try {
					if (socket_cam == null) {
						runOnUiThread(new Runnable() {
							public void run() {
								iv.setImageResource(R.drawable.ic_launcher);
								Toast.makeText(getApplicationContext(),
										"Camera: Robot Disconnect.",
										Toast.LENGTH_SHORT).show();

							}
						});
						// socket_command.close();
						_connect.setEnabled(true);
						_fw.setEnabled(false);
						_bw.setEnabled(false);
						_tl.setEnabled(false);
						_tr.setEnabled(false);
						on_off.setEnabled(false);
						break;
					} else {
						// IF VDO
						input_cam = socket_cam.getInputStream();
						dis = new DataInputStream(input_cam);
						int size = dis.readInt();
						byte[] store_vdo = new byte[size];
						dis.readFully(store_vdo);
						bmp = BitmapFactory.decodeByteArray(store_vdo, 0,
								store_vdo.length);
						mat = new Matrix();
						mat.postRotate(90);
						bmpShow = Bitmap.createBitmap(bmp, 0, 0,
								bmp.getWidth(), bmp.getHeight(), mat, true);

						handCam.post(new Runnable() {

							@Override
							public void run() {
								iv.setImageBitmap(bmpShow);
							}
						});

					}

					// }
					//

				}
			} catch (IOException e) {

				runOnUiThread(new Runnable() {
					public void run() {

						iv.setImageResource(R.drawable.ic_launcher);
						Toast.makeText(getApplicationContext(),
								"Camera: No data.", Toast.LENGTH_SHORT).show();
						bg.setBackgroundColor(Color.argb(100, 240, 135, 124));
						_connect.setEnabled(true);
						_fw.setEnabled(false);
						_bw.setEnabled(false);
						_tl.setEnabled(false);
						_tr.setEnabled(false);
						on_off.setEnabled(false);
						on_off.setChecked(false);
						_temp.setText("Temperature : No data");
						on_off.setBackgroundResource(R.drawable.off);
						loop_state_temp = false;
						if (socket_temp != null) {
							// socket_temp = null;

							try {
								socket_temp.close();
								socket_temp = null;
							} catch (IOException e) {
								e.printStackTrace();
							}

						}
										

					}
				});

				loop_state_cam = false;
			}
		}
	};

	/** ################# GPS Input ################## */
	Runnable _DataInputGPS = new Runnable() {

		@SuppressWarnings("deprecation")
		@Override
		public void run() {
			// TODO Auto-generated method stub
			while (loop_state_GPS) {
				if (socket_gps != null) {
					try {

						input_gps = socket_gps.getInputStream();
						dis_gps = new DataInputStream(input_gps);

						if (socket_gps != null) {
							readGPS = dis_gps.readLine();
							if (readGPS == null) {
								handGPS.post(new Runnable() {

									@Override
									public void run() {
										_GPS.setText("No data");
									}
								});
								break;
							} else {
								handGPS.post(new Runnable() {

									@Override
									public void run() {
										_GPS.setText(readGPS);
									}
								});
							}
						}

						// }
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();

						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								Toast.makeText(getApplicationContext(),
										"GPS : No data.", Toast.LENGTH_SHORT)
										.show();
								_GPS.setText("Unknow,Unknow");
							}
						});

						break;
					}
				} else {
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							Toast.makeText(getApplicationContext(),
									"GPS : No data.", Toast.LENGTH_SHORT)
									.show();
							_GPS.setText("No data");
						}
					});
				}

			}
		}
	};

	// SET VALUE
	public void setValue() {
		_fw = (ImageView) findViewById(R.id.fw);
		_bw = (ImageView) findViewById(R.id.bw);
		_tl = (ImageView) findViewById(R.id.tl);
		_tr = (ImageView) findViewById(R.id.tr);
		on_off = (ToggleButton) findViewById(R.id.toggleButton1);
		_temp = (TextView) findViewById(R.id.textView2);
		_targetIP = (TextView) findViewById(R.id.target_ip);
		// _targetPort = (TextView) findViewById(R.id.target_port);
		_connect = (Button) findViewById(R.id.connect);
		_menu = (Button) findViewById(R.id._menu);
		_exit = (Button) findViewById(R.id._exit);
		iv = (ImageView) findViewById(R.id.imageView1);
		SharedPreferences settings = getSharedPreferences("Pref", 0);
		String ip = settings.getString("IP", "");
		// String port = settings.getString("PORT", "");
		_targetIP.setText(ip);
		_GPS = (TextView) findViewById(R.id.showgps);
		bg = (RelativeLayout) findViewById(R.id.RA1);
				//autoCon=(ToggleButton) findViewById(R.id.AutoControl);
	}

}