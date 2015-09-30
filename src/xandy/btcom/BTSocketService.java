package xandy.btcom;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/*�ļ�����BTSocketService
 * ���ߣ�Xandy
 * email:xl19862005@126.com
 * ˵�����˹��̴���Ϊ����ѧϰ���ã�δ�������������������κ���ʽ����ҵ������*/
public class BTSocketService extends Service implements Runnable {
	private final String TAG = "BTSocketService";

	private BluetoothServerSocket mBTServerSocket;
	private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	
	private Thread mThread;
	private boolean mThreadState = false;
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
		
		if(mBluetoothAdapter.isEnabled()){
			mThreadState = false;	
			mThread = null;
			
			mThread = new Thread(this);
			mThread.start();
			mThreadState = true; 
		}else{
			mThreadState = false;	
			mThread = null;
		}
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
				
		//����ɱ����ֹͣ����������
	    Intent i = new Intent(this,BTSocketService.class); 
        startService(i);  
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub

		return new BTSocketBinder();
	}
	
	/**
	 * ����һ��binder����
	 * */
	public class BTSocketBinder extends Binder{
		/**
		 * ��ȡBTSocketServiceʵ��
		 * */
		public BTSocketService getService(){
			return BTSocketService.this;
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		BluetoothSocket socket = null;
		
		try {
			/* ����һ������������ 
			 * �����ֱ𣺷��������ơ�UUID	 */	
			mBTServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("btspp",
					UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));		
			
			Log.d(TAG, "Xandy wait cilent connect...");
			/*
			Message msg = new Message();
			msg.obj = "���Ժ����ڵȴ��ͻ��˵�����...";
			msg.what = 0;
			LinkDetectedHandler.sendMessage(msg);
			*/
			
			while(mThreadState){
				/* ���ܿͻ��˵��������� */
				socket = mBTServerSocket.accept();
				
				if(socket != null){
					Log.d(TAG, "Xandy->" + socket.getRemoteDevice().getName() + ": �ͻ����Ѿ�����!");

			        byte[] buffer = new byte[1024];
			        int bytes;
			        InputStream mmInStream = null;
			        
					try {
						mmInStream = socket.getInputStream();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
						break;
					}	
					
					/*Ŀǰֻ֧��P2Pģʽ*/
			        while (mThreadState) {
			            try {
			                // Read from the InputStream
			                if( (bytes = mmInStream.read(buffer)) > 0 )
			                {
			                    byte[] buf_data = new byte[bytes];
						    	for(int i=0; i<bytes; i++)
						    	{
						    		buf_data[i] = buffer[i];
						    	}
								String s = new String(buf_data);
								
								Log.d(TAG,"Xandy->" + socket.getRemoteDevice().getName() + ": " + s);
									
								Intent intent = new Intent();
				                intent.setAction("com.xandy.btcom.socket.ACTION");
				                intent.putExtra("btcom.socket.msg", s);
				                sendBroadcast(intent);
			                }
			            } catch (IOException e) {
			            	try {
								mmInStream.close();
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
								break;
							}
			                break;
			            }  
			        }
			    
				}
			}
			
			mThreadState = false;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
