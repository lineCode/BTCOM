package xandy.btcom;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import xandy.btcom.Bluetooth.ServerOrCilent;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class ChatActivity extends Activity implements OnItemClickListener ,OnClickListener{
    /** Called when the activity is first created. */
	
	private ListView mListView;
	private ArrayList<deviceListItem>list;
	private Button sendButton;
	private Button disconnectButton;
	private EditText editMsgView;
	private RadioGroup mRadioGroup = null;
	private RadioButton mAsciiRadioButton = null;
	private RadioButton mHexRadioButton = null;
	DeviceListAdapter mAdapter;
	Context mContext;
	
	/* һЩ��������������������� */
	public static final String PROTOCOL_SCHEME_L2CAP = "btl2cap";
	public static final String PROTOCOL_SCHEME_RFCOMM = "btspp";
	public static final String PROTOCOL_SCHEME_BT_OBEX = "btgoep";
	public static final String PROTOCOL_SCHEME_TCP_OBEX = "tcpobex";
	
	private BluetoothServerSocket mBTServerSocket = null;
	private ServerThread startServerThread = null;
	private clientThread clientConnectThread = null;
	private BluetoothSocket mBTSocket = null;
	private BluetoothDevice device = null;
	private ReadThread mReadThread = null;;	
	private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	
	public boolean mAsciiSendOn = true;
	
    private final static char[] mChars = "0123456789ABCDEF".toCharArray();  
    private final static String mHexStr = "0123456789ABCDEF";  
    
	public String mMsgText = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); 
        setContentView(R.layout.chat);
        
        if(mBluetoothAdapter == null){
        	Toast.makeText(this, "�޷����ֻ���������ȷ���ֻ��Ƿ����������ܣ�", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//����
        
        mContext = this;
        init();
    }
    
	private void init() {		   
		list = new ArrayList<deviceListItem>();
		mAdapter = new DeviceListAdapter(this, list);
		mListView = (ListView) findViewById(R.id.list);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
		mListView.setFastScrollEnabled(true);
		editMsgView= (EditText)findViewById(R.id.MessageText);	
		editMsgView.clearFocus();
		
		sendButton= (Button)findViewById(R.id.btn_msg_send);
		sendButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				mMsgText=editMsgView.getText().toString();
				if (mMsgText.length()>0) {
					sendMessageHandle(mMsgText);	
					editMsgView.setText("");
					editMsgView.clearFocus();
					//close InputMethodManager
					InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE); 
					imm.hideSoftInputFromWindow(editMsgView.getWindowToken(), 0);
				}else
				Toast.makeText(mContext, "�������ݲ���Ϊ�գ�", Toast.LENGTH_SHORT).show();
			}
		});
		
		disconnectButton = (Button)findViewById(R.id.btn_disconnect);
		disconnectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
		        if (Bluetooth.serviceOrCilent == ServerOrCilent.CILENT) 
				{
		        	shutdownClient();
				}
				else if (Bluetooth.serviceOrCilent == ServerOrCilent.SERVICE) 
				{
					shutdownServer();
				}
				Bluetooth.isOpen = false;
				Bluetooth.serviceOrCilent=ServerOrCilent.NONE;
				Toast.makeText(mContext, "�ѶϿ����ӣ�", Toast.LENGTH_SHORT).show();
			}
		});	
		
		mRadioGroup = (RadioGroup)findViewById(R.id.ascii_hex_select);
		mAsciiRadioButton = (RadioButton)findViewById(R.id.ascii_radio_button);
		mHexRadioButton = (RadioButton)findViewById(R.id.hex_radio_button);
		
		mRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				// TODO Auto-generated method stub
				if(mAsciiRadioButton.getId() == checkedId){
					mAsciiSendOn = true;
				}else{
					mAsciiSendOn = false;
				}
			}
		});
	}    

    private Handler LinkDetectedHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	//Toast.makeText(mContext, (String)msg.obj, Toast.LENGTH_SHORT).show();
        	if(msg.what==1)
        	{
        		list.add(new deviceListItem((String)msg.obj, true));
        	}
        	else
        	{
        		list.add(new deviceListItem((String)msg.obj, false));
        	}
			mAdapter.notifyDataSetChanged();
			mListView.setSelection(list.size() - 1);
        }
        
    };    
    
    @Override
    public synchronized void onPause() {
        super.onPause();
    }
    @Override
    public synchronized void onResume() {
        super.onResume();
        if(Bluetooth.isOpen)
        {
        	Toast.makeText(mContext, "�����Ѿ��򿪣�����ͨ�š����Ҫ�ٽ������ӣ����ȶϿ���", Toast.LENGTH_SHORT).show();
        	return;
        }
        if(Bluetooth.serviceOrCilent==ServerOrCilent.CILENT)
        {
			String address = Bluetooth.BlueToothAddress;
			if(!address.equals("null"))
			{
				device = mBluetoothAdapter.getRemoteDevice(address);	
				clientConnectThread = new clientThread();
				clientConnectThread.start();
				Bluetooth.isOpen = true;
			}
			else
			{
				Toast.makeText(mContext, "address is null !", Toast.LENGTH_SHORT).show();
			}
        }
        else if(Bluetooth.serviceOrCilent==ServerOrCilent.SERVICE)
        {        	      	
        	startServerThread = new ServerThread();
        	startServerThread.start();
        	Bluetooth.isOpen = true;
        }
    }
	//�����ͻ���
	private class clientThread extends Thread { 		
		public void run() {
			try {
				//����һ��Socket���ӣ�ֻ��Ҫ��������ע��ʱ��UUID��
				// socket = device.createRfcommSocketToServiceRecord(BluetoothProtocols.OBEX_OBJECT_PUSH_PROTOCOL_UUID);
				mBTSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
				//����
				Message msg2 = new Message();
				msg2.obj = "���Ժ��������ӷ�����:"+Bluetooth.BlueToothAddress;
				msg2.what = 0;
				LinkDetectedHandler.sendMessage(msg2);
				
				mBTSocket.connect();
				
				Message msg = new Message();
				msg.obj = "�Ѿ������Ϸ���ˣ����Է�����Ϣ��";
				msg.what = 0;
				LinkDetectedHandler.sendMessage(msg);
				//������������
				mReadThread = new ReadThread(ChatActivity.this, mBTSocket, LinkDetectedHandler);
				mReadThread.start();
			} 
			catch (IOException e) 
			{
				Log.e("connect", "", e);
				Message msg = new Message();
				msg.obj = "���ӷ�����쳣���Ͽ�����������һ�ԡ�";
				msg.what = 0;
				LinkDetectedHandler.sendMessage(msg);
			} 
		}
	};

	//����������
	private class ServerThread extends Thread { 
		public void run() {
					
			try { 
				/* ����һ������������ 
				 * �����ֱ𣺷��������ơ�UUID	 */	
				mBTServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(PROTOCOL_SCHEME_RFCOMM,
						UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));		
				
				Log.d("server", "wait cilent connect...");
				
				Message msg = new Message();
				msg.obj = "���Ժ����ڵȴ��ͻ��˵�����...";
				msg.what = 0;
				LinkDetectedHandler.sendMessage(msg);
				
				/* ���ܿͻ��˵��������� */
				mBTSocket = mBTServerSocket.accept();
				Log.d("server", "accept success !");
				
				Message msg2 = new Message();
				String info = "�ͻ����Ѿ������ϣ����Է�����Ϣ��";
				msg2.obj = info;
				msg.what = 0;
				LinkDetectedHandler.sendMessage(msg2);
				//������������
				mReadThread = new ReadThread(ChatActivity.this, mBTSocket, LinkDetectedHandler);
				mReadThread.start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};
	/* ֹͣ������ */
	private void shutdownServer() {
		new Thread() {
			public void run() {
				if(startServerThread != null)
				{
					startServerThread.interrupt();
					startServerThread = null;
				}
				if(mReadThread != null)
				{
					mReadThread.interrupt();
					mReadThread = null;
				}				
				try {					
					if(mBTSocket != null)
					{
						mBTSocket.close();
						mBTSocket = null;
					}
					if (mBTServerSocket != null)
					{
						mBTServerSocket.close();/* �رշ����� */
						mBTServerSocket = null;
					}
				} catch (IOException e) {
					Log.e("server", "mserverSocket.close()", e);
				}
			};
		}.start();
	}
	/* ֹͣ�ͻ������� */
	private void shutdownClient() {
		new Thread() {
			public void run() {
				if(clientConnectThread!=null)
				{
					clientConnectThread.interrupt();
					clientConnectThread= null;
				}
				if(mReadThread != null)
				{
					mReadThread.interrupt();
					mReadThread = null;
				}
				if (mBTSocket != null) {
					try {
						mBTSocket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					mBTSocket = null;
				}
			};
		}.start();
	}
	
    /**  
     * ���16�����ַ����Ƿ���Ч 
     * @param sHex String 16�����ַ��� 
     * @return boolean 
     */    
    public static boolean checkHexStr(String sHex){    
        String sTmp = sHex.toString().trim().replace(" ", "").toUpperCase(Locale.US);  
        int iLen = sTmp.length();  
          
        if (iLen > 1 && iLen%2 == 0){  
            for(int i=0; i<iLen; i++)  
                if (!mHexStr.contains(sTmp.substring(i, i+1)))  
                    return false;  
            return true;  
        }  
        else  
            return false;  
    } 
    
    /** 
     * bytes�ַ���ת��ΪByteֵ 
     * @param src String Byte�ַ�����ÿ��Byte֮��û�зָ���(�ַ���Χ:0-9 A-F) 
     * @return byte[] 
     */  
    public static byte[] hexStr2Bytes(String src){  
        /*������ֵ���й淶������*/  
        src = src.trim().replace(" ", "").toUpperCase(Locale.US);  
        //����ֵ��ʼ��  
        int m=0,n=0;  
        int iLen=src.length()/2; //���㳤��  
        byte[] ret = new byte[iLen]; //����洢�ռ�  
          
        for (int i = 0; i < iLen; i++){  
            m=i*2+1;  
            n=m+1;  
            ret[i] = (byte)(Integer.decode("0x"+ src.substring(i*2, m) + src.substring(m,n)) & 0xFF);  
        }  
        return ret;  
    } 
    
    /** 
     * bytesת����ʮ�������ַ��� 
     * @param b byte[] byte���� 
     * @param iLen int ȡǰNλ���� N=iLen 
     * @return String ÿ��Byteֵ֮��ո�ָ� 
     */  
    public String byte2HexStr(byte[] b, int iLen){  
        StringBuilder sb = new StringBuilder();  
        for (int n=0; n<iLen; n++){  
            sb.append(mChars[(b[n] & 0xFF) >> 4]);  
            sb.append(mChars[b[n] & 0x0F]);  
            sb.append(' ');  
        }  
        return sb.toString().trim().toUpperCase(Locale.US);  
    }    
	
	//��������
	private void sendMessageHandle(String msg) 
	{	
		if (mBTSocket == null) 
		{
			Toast.makeText(mContext, "û������", Toast.LENGTH_SHORT).show();
			return;
		}
		try {				
			OutputStream os = mBTSocket.getOutputStream(); 
			if(mAsciiSendOn){
				os.write(msg.getBytes());
			}else{
				if(checkHexStr(msg)){
					os.write(hexStr2Bytes(msg));
				}else{
					Toast.makeText(mContext, "16���Ʒ��͵����ݲ��Ϸ���", Toast.LENGTH_SHORT).show();
					return;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			
		list.add(new deviceListItem(msg, false));
		mAdapter.notifyDataSetChanged();
		mListView.setSelection(list.size() - 1);
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (Bluetooth.serviceOrCilent == ServerOrCilent.CILENT) 
		{
        	shutdownClient();
		}
		else if (Bluetooth.serviceOrCilent == ServerOrCilent.SERVICE) 
		{
			shutdownServer();
		}
        Bluetooth.isOpen = false;
		Bluetooth.serviceOrCilent = ServerOrCilent.NONE;
    }
	public class SiriListItem {
		String message;
		boolean isSiri;

		public SiriListItem(String msg, boolean siri) {
			message = msg;
			isSiri = siri;
		}
	}
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO Auto-generated method stub
	}
	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
	}	
	public class deviceListItem {
		String message;
		boolean isSiri;

		public deviceListItem(String msg, boolean siri) {
			message = msg;
			isSiri = siri;
		}
	} 
}