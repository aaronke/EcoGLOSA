package cst.aaron.ecoglosa;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity implements LocationListener{

	private static BluetoothAdapter mBluetoothAdapter;
	private static final UUID MY_UUID=UUID.fromString("66841278-c3d1-11df-ab31-001de000a903");
//	private static OutputStream mOutputStream;
//	private static InputStream mInputStream;
	private final static int 	REQUEST_CODE_BT=1;
	private static final String MY_NAME="Bluetooth_Test_Aaron";
	private AcceptThread acceptThread;
	private ConnectedThread mConnectedThread;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_CONNECTED=1;
	public static final int MESSAGE_DISCONNECTE=3;
	private final static int DISCURABLE_TIME=300;
	
	public static TextView count_TextView;
	public static ImageView signal_ImageView;
	public static TextView connecttion_TextView;
	public static ImageView left_ImageView;
	public static ImageView straight_ImageView;
	public static ImageView right_ImageView;
	public static TextToSpeech textToSpeech;
	private final static int SIGNAL_GREEN=3, SIGNAL_RED=1, SIGNAL_YELLOW=2;
	private final static String KEEP_CURRENT_SPEED="keep current speed";
	private final static String SPEED_UP="Speed up to";
	private final static String SLOW_DOWN="Slow down to";
	private final static String TURN_RED="Signal will turn red ahead";
	private final static String RED_SIGNAL_AHEAD="Red light ahead";
	private LocationManager locationManager;
	private static double current_speed, current_distance;
	private final static double RSU_LATI=-113.530646, RSU_LONG=53.526871;
	private static SpeedometerView speedometerView;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // setup UI view;
        count_TextView=(TextView)findViewById(R.id.signal_count);
        connecttion_TextView=(TextView)findViewById(R.id.connect_label);
        
        speedometerView=(SpeedometerView)findViewById(R.id.speedometerview);
        
        speedometerView.setLabelConverter(new SpeedometerView.LabelConverter() {
			
			@Override
			public String getLabelFor(double progress, double maxProgress) {
				// TODO Auto-generated method stub
				return String.valueOf((int)Math.round(progress));
			}
		});
        
        speedometerView.setMaxSpeed(200);
        speedometerView.setMajorTickStep(20);
        speedometerView.setMinorTicks(1);
        
        speedometerView.setSpeed((int)(Math.random()*10+60));
        
        speedometerView.addColoredRange(20, 80, Color.GREEN);
        speedometerView.addColoredRange(80, 100, Color.YELLOW);
        speedometerView.addColoredRange(100, 200, Color.RED);

        locationManager=(LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
        
        textToSpeech=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {			
			@Override
			public void onInit(int status) {
				// TODO Auto-generated method stub
			
				if (status==TextToSpeech.SUCCESS) {
					textToSpeech.setLanguage(Locale.UK);	
					speakToText("Hello, Welcome to CST Connected Vehicle Lab!");
				}
			}
		});
        
        
        // setup the BlueTooth 
        mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter==null) {
			Toast.makeText(getApplicationContext(), "Your device doesn't support bluetooth~", Toast.LENGTH_SHORT).show();		
		}
        
       /* if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_CODE_BT);
		}*/
        
        
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCURABLE_TIME);
                startActivityForResult(discoverableIntent,REQUEST_CODE_BT);
         }else{
            if(acceptThread==null){
               acceptThread=new AcceptThread();
           	   acceptThread.start();
             }
         }
    }

    public static void speakToText(String string){
    	
    	textToSpeech.speak(string, TextToSpeech.QUEUE_FLUSH, null);
    }
    protected void onActivityResult( int requestCode,  int resultCode, Intent data){
    	
    	switch (requestCode) {
		case REQUEST_CODE_BT:
			if (resultCode==DISCURABLE_TIME) {
				if (acceptThread==null) {
					acceptThread=new AcceptThread();
					acceptThread.start();
				}
			}
			break;

		default:
			break;
		}
    }
    
    private class AcceptThread extends Thread{
    	private BluetoothServerSocket mServerSocket=null;
    	
    	public AcceptThread(){
    		
    		BluetoothServerSocket tmp=null;
    		try {
				
    			tmp=mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(MY_NAME, MY_UUID);
    			Log.v("bluetooth", "serversocket create success");
			} catch (Exception e) {
				// TODO: handle exception	
				Log.v("bluetooth", "serversocket create fail");
			}
    		mServerSocket=tmp;
    	}
    	
    	public void run(){
    		BluetoothSocket mBluetoothSocket=null;
    		while(true){
    			try {
					mBluetoothSocket=mServerSocket.accept();
					
				} catch (Exception e) {
					// TODO: handle exception
			//		Toast.makeText(getApplicationContext(), "unable to connect!", Toast.LENGTH_SHORT).show();
					Log.v("bluetooth", "unable to connect");
					break;
				}
    			if (mBluetoothSocket!=null) {
					// do something about the socket;
    			
    			
    			connected(mBluetoothSocket);
    				Log.v("bluetooth", "bluetooth connected");
    				try {
						mServerSocket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    				break;
				}
    		}
    	}
    	public void cancel(){
    		try {
				mServerSocket.close();
				mHandler.obtainMessage(MESSAGE_DISCONNECTE).sendToTarget();
			} catch (Exception e) {
				// TODO: handle exception
			}
    	}
    }
    public synchronized void connected(BluetoothSocket socket){
    	if (acceptThread!=null) {
			acceptThread.cancel();acceptThread=null;
		}
    	if (mConnectedThread!=null) {
			mConnectedThread.cancel();mConnectedThread=null;
		}
    	mConnectedThread=new ConnectedThread(socket);
    	mConnectedThread.start();
    }
    
    private class ConnectedThread extends Thread{
    	private final BluetoothSocket mmSocket;
    	private  final InputStream mmInputStream;
    	//private  final OutputStream mmOutputStream;
    	
    	public ConnectedThread(BluetoothSocket socket){
    		
    		mmSocket=socket;
    		InputStream tmpInputStream=null;
    		//OutputStream tmpOutputStream=null;
    		try {
				tmpInputStream=socket.getInputStream();
				//tmpOutputStream=socket.getOutputStream();
			} catch (Exception e) {
				// TODO: handle exception
				disconnected_UI();
				Log.v("bluetooth", "tmp sockets not created");
				
			}
    		mmInputStream=tmpInputStream;
    	//	mmOutputStream=tmpOutputStream;
    		
    	}
    	public void run(){
    		byte[] buffer=new byte[1024];
    		int bytes;
    		//String messageString="Hello, I am Android";
    	//	byte[] sent_message=messageString.getBytes();
    		while (true) {
				try {
					
					bytes=mmInputStream.read(buffer);
					//mmOutputStream.write(sent_message);
				//	Log.v("bluetooth", "Send Message:"+messageString);
					mHandler.obtainMessage(MESSAGE_CONNECTED).sendToTarget();
					
					mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
				} catch (Exception e) {
					// TODO: handle exception
					disconnected_UI();
					Log.v("bluetooth", "disconnected");
				}
			}
    	}
    	public void cancel(){
    		try {
				mmSocket.close();
			} catch (Exception e) {
				// TODO: handle exception
				Log.v("bluetooth", "close socket failed");
			}
    	}
    	
    }
    
    private static final Handler mHandler=new Handler(){
    	public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_READ:
				byte[] readbuf=(byte[]) msg.obj;
				String readMessageString=new String(readbuf,0,msg.arg1);
			//	String message_getString=new BigInteger(0,readbuf).toString(16);
			//	String message_getString=byteArrayToHex(readbuf);
				/*int size= readbuf.length;
				int dataint[]= new int[size];
				StringBuilder bStringBuilder=new StringBuilder();
				for (int i = 0; i < msg.arg1; i++) {
					dataint[i]=readbuf[i];
					bStringBuilder.append(Integer.toHexString(dataint[i]));
				}
				String message_getString=bStringBuilder.toString();*/
				readMessageString=readMessageString.substring(5);
				Log.v("bluetooth", "read:"+readMessageString);
			//	Log.v("bluetooth", "read:"+message_getString);
				InfoEntity temp_infoEntity=new InfoEntity();
					temp_infoEntity=MessageParse(readMessageString);
				UpdateUI(temp_infoEntity);
				speakToText(GLOSAUpdate(temp_infoEntity));
				break;
			case MESSAGE_CONNECTED:
				connecttion_TextView.setText("Connected");
				break;
			case MESSAGE_DISCONNECTE:
				connecttion_TextView.setText("not Connected");
				 break;
			default:
				break;
			}
		}
    };

    public static InfoEntity MessageParse(String msg){
    	
    	InfoEntity infoEntity=new InfoEntity();
    	
    	//int  count=Character.getNumericValue(msg.charAt(4));
    		int count=Integer.parseInt(msg.substring(3, 5));
    		Log.v("bluetooth", msg.substring(3, 5));
    	infoEntity.setSignal_time(count);
    	
    	if (msg.contains("GSB")) {
			infoEntity.setDirection_code(InfoEntity.SIGNAL_DIRECTION_RL);
			infoEntity.setSignal_color_code(InfoEntity.SINGAL_GREEN);
		}
    	if (msg.contains("RSB")) {
			infoEntity.setDirection_code(InfoEntity.SIGNAL_DIRECTION_LEFT);
			infoEntity.setSignal_color_code(InfoEntity.SIGNAL_RED);
		}
    	if (msg.contains("YSB")) {
			infoEntity.setDirection_code(InfoEntity.SIGNAL_DIRECTION_STRAIGHT);
			infoEntity.setSignal_color_code(InfoEntity.SIGNAL_YELLOW);
		}
    	infoEntity.setSpeed(current_speed);
    	infoEntity.setDistance(current_distance);
    	return infoEntity;
    	
    }
   private static void disconnected_UI(){
		count_TextView.setVisibility(TextView.GONE);
		connecttion_TextView.setText("not Connected");
   }
   
   // get the advice message type;
   private static String GLOSAUpdate(InfoEntity infoEntity){
	   String advice_mesageString = null;
	   double v_0=infoEntity.getSpeed();
	   double x=infoEntity.getDistance();
	   double v_max=infoEntity.getMax_speed();
	   double t1=x/v_0;
	   int t_m=infoEntity.getSignal_time();
	   
	   if (infoEntity.getSignal_color_code()==SIGNAL_GREEN) {
		   
		   double a_r=1.70*Math.exp(-0.04*v_0);
		   double t2=(x-(Math.pow(v_max, 2)-Math.pow(v_0, 2))/(2*a_r))/v_max
				   +(v_max-v_0)/a_r;
		   if (t1<t_m) {
			advice_mesageString=KEEP_CURRENT_SPEED;
		   }else if (t2<=t_m && t1>t_m) {
			double v_s;
			v_s=v_0+a_r*t_m+Math.pow((a_r*(a_r*t_m*t_m+2*t_m*v_0-2*x)), 0.5);
			advice_mesageString=SPEED_UP+v_s;
		   }else if (t2>t_m) {
			advice_mesageString=TURN_RED;
		   }else {
			// do nothing;
		   }
		   
	   }else if (infoEntity.getSignal_color_code()==SIGNAL_YELLOW) {
		   advice_mesageString=TURN_RED;
		   
	   }else if (infoEntity.getSignal_color_code()==SIGNAL_RED){
		   double a_d=-0.005*Math.pow(v_0, 3)+0.154*v_0+0.493;
		   double v_min=0.5*v_0;
		   double t2=(x-(v_0*v_0-v_min*v_min)/(2*a_d))/v_min+(v_0-v_min)/a_d;
		   if (t1>t_m) {
			   advice_mesageString=KEEP_CURRENT_SPEED;
		   }else if (t1<t_m && t2>t_m) {
			   double v_s=v_0-a_d*t_m+Math.pow((a_d*(a_d*t_m*t_m-2*t_m*v_0+2*x)), 0.5);
			   advice_mesageString=SLOW_DOWN+v_s;
		   }else if (t2<=t_m) {
			   advice_mesageString=RED_SIGNAL_AHEAD;
		   }else {
			   // do nothing;
		   }
			
	   }else {
		   // do nothing;
	   }
	   
	   return advice_mesageString;
	   
   }
   
    private static void UpdateUI(InfoEntity infoentity){
    	double critial_speed=3.6*infoentity.getDistance()/infoentity.getSignal_time();
    	if (critial_speed>200) {
			critial_speed=200;
		}
    	switch (infoentity.getSignal_color_code()) {
		case InfoEntity.SIGNAL_RED:
			count_TextView.setTextColor(Color.RED);
			speedometerView.addColoredRange(critial_speed, 200, Color.RED);
			speedometerView.addColoredRange(infoentity.getDistance()/(infoentity.getSignal_time()+60), critial_speed, Color.GREEN);
			speedometerView.addColoredRange(infoentity.getDistance()/(infoentity.getSignal_time()+63), infoentity.getDistance()/(infoentity.getSignal_time()+60), Color.YELLOW);
			break;
		case InfoEntity.SIGNAL_YELLOW:
			count_TextView.setTextColor(Color.YELLOW);
			speedometerView.addColoredRange(critial_speed, 200, Color.YELLOW);
			speedometerView.addColoredRange(infoentity.getDistance()/(infoentity.getSignal_time()+60), critial_speed, Color.GREEN);
			speedometerView.addColoredRange(infoentity.getDistance()/(infoentity.getSignal_time()+120), infoentity.getDistance()/(infoentity.getSignal_time()+60), Color.RED);
			break;
		case InfoEntity.SINGAL_GREEN:
			count_TextView.setTextColor(Color.GREEN);
			speedometerView.addColoredRange(critial_speed, 200, Color.GREEN);
			speedometerView.addColoredRange(infoentity.getDistance()/(infoentity.getSignal_time()+3), critial_speed, Color.YELLOW);
			speedometerView.addColoredRange(infoentity.getDistance()/(infoentity.getSignal_time()+63), infoentity.getDistance()/(infoentity.getSignal_time()+3), Color.RED);
			break;
		default: 
			count_TextView.setVisibility(TextView.GONE);
			connecttion_TextView.setText("not Connected");
			break;
		}
    	
    	count_TextView.setText(""+infoentity.getSignal_time());
    	
    }
    @Override
    public void onPause(){
    	if (textToSpeech!=null) {
			textToSpeech.stop();
			textToSpeech.shutdown();
		}
    	super.onPause();
    }
    @Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		if (acceptThread!=null) {
			acceptThread.cancel();
		}
		if (mConnectedThread!=null) {
			mConnectedThread.cancel();
		}
		
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		current_speed=location.getSpeed();
		Location temp_location=new Location("test");
		temp_location.setLatitude(RSU_LATI);
		temp_location.setLongitude(RSU_LONG);
		current_distance=location.distanceTo(temp_location);
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
}
