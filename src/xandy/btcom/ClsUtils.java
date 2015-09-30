package xandy.btcom;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

public class ClsUtils {
	
	/**
	 * ���豸��� �ο�Դ��
	 * /Settings/src/com/android/settings/bluetooth/CachedBluetoothDevice.java
	 */
	static public void connect(Class btClass,BluetoothDevice btDevice) throws Exception {
		Method connectMethod = btClass.getMethod("connect");
		Boolean returnValue = (Boolean) connectMethod.invoke(btDevice);
	}

	/**
	 * ���豸��� �ο�Դ��
	 * /Settings/src/com/android/settings/bluetooth/CachedBluetoothDevice.java
	 */
	static public boolean createBond(Class btClass,BluetoothDevice btDevice) throws Exception {
		Method createBondMethod = btClass.getMethod("createBond");
		Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
		return returnValue.booleanValue();
	}

	/**
	 * ���豸������ �ο�Դ��
	 * /Settings/src/com/android/settings/bluetooth/CachedBluetoothDevice.java
	 */
	static public boolean removeBond(Class btClass,BluetoothDevice btDevice) throws Exception {
		Method removeBondMethod = btClass.getMethod("removeBond");
		Boolean returnValue = (Boolean) removeBondMethod.invoke(btDevice);
		return returnValue.booleanValue();
	}

	/**
	 * 
	 * @param clsShow
	 */
	static public void printAllInform(Class clsShow) {
		try {
			// ȡ�����з���
			Method[] hideMethod = clsShow.getMethods();
			int i = 0;
			for (; i < hideMethod.length; i++) {
				Log.e("method name", hideMethod[i].getName());
			}
			// ȡ�����г���
			Field[] allFields = clsShow.getFields();
			for (i = 0; i < allFields.length; i++) {
				Log.e("Field name", allFields[i].getName());
			}
		} catch (SecurityException e) {
			// throw new RuntimeException(e.getMessage());
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// throw new RuntimeException(e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}