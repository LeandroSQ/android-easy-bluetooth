package quevedo.soares.leandro.lib;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.ArrayList;

public class BluetoothScanner {

	private static final int BLUETOOTH_ENABLE_REQUEST_CODE = 1902;
	private static final int LOCATION_ENABLE_REQUEST_CODE = 1932;

	private boolean includePairedDevices = false;

	private ArrayList<BluetoothDevice> bluetoothDeviceList;
	private Activity activity;
	private BluetoothAdapter bluetoothAdapter;
	private BluetoothScanListener listener;
	private boolean isInDiscoveryMode = false;
	private BroadcastReceiver bluetoothBroadcastReceiver = new BroadcastReceiver () {
		@Override
		public void onReceive (Context context, Intent intent) {
			// Verify the intent integrity
			if (intent == null) {
				return;
			}

			switch (intent.getAction ()) {
				case BluetoothDevice.ACTION_FOUND:
					// A device was found!
					BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);

					// Verify if the device wasn't listed before
					// In some Android devices the bluetooth adapter finds the same device twice
					for (BluetoothDevice bluetoothDevice : bluetoothDeviceList) {
						// If we've got a device already on list, just ignore
						if (bluetoothDevice.getAddress ().equals (device.getAddress ())) {
							return;
						}
					}

					// Append to the list
					bluetoothDeviceList.add (device);
					// Notify the listener
					listener.onBluetoothDeviceFound (device, bluetoothAdapter.getBondedDevices ().contains (device));
					Log.d ("bluetooth_helper", "Found bluetooth device with id of " + device.getAddress ());
					break;
				case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
					// Set the discoveryMode flag
					isInDiscoveryMode = true;

					// The scan has just started, clear the device list
					Log.d ("bluetooth_helper", "Bluetooth discovery started");
					bluetoothDeviceList = new ArrayList<> ();

					// If requested, appends the already paired devices to the list
					if (includePairedDevices) {
						bluetoothDeviceList.addAll (getPairedDevices ());
					}
					break;
				case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
					// The scan has ended, notify the listener
					Log.d ("bluetooth_helper", "Bluetooth discovery finished");
					if (bluetoothDeviceList == null) {
						bluetoothDeviceList = new ArrayList<> ();
					}

					listener.onBluetoothScanEnd (bluetoothDeviceList);
					activity.unregisterReceiver (this);

					isInDiscoveryMode = false;
					break;
			}
		}
	};

	public BluetoothScanner (Activity activity, BluetoothScanListener listener) {
		this.activity = activity;
		this.listener = listener;

		Log.d ("bluetooth_helper", "Bluetooth helper instance created!");
	}

	public boolean isAdapterAvailable () {
		return this.bluetoothAdapter != null && this.bluetoothAdapter.isEnabled ();
	}

	public void stopDiscovery () {
		if (this.isAdapterAvailable () && this.bluetoothAdapter.isDiscovering ()) {
			this.bluetoothAdapter.cancelDiscovery ();
		}
	}

	public boolean inDiscoveryMode () {
		return this.isInDiscoveryMode;
	}

	public ArrayList<BluetoothDevice> getPairedDevices () {
		return new ArrayList<> (bluetoothAdapter.getBondedDevices ());
	}

	@TargetApi (19)
	public boolean pairWith (BluetoothDevice device) {
		try {
			return device.createBond ();
		} catch (Exception e) {
			e.printStackTrace ();
			return false;
		}
	}

	//<editor-fold defaultstate="Collapsed" desc="Permission handling">
	public boolean handleActivityResult (int requestCode, int resultCode, Intent data) {
		// Check if we're able to restart
		if (requestCode == BLUETOOTH_ENABLE_REQUEST_CODE) {
			if (this.bluetoothAdapter.isEnabled ()) {
				this.startDiscovery ();
			} else {
				this.listener.onBluetoothError ("O seu adaptador Bluetooth está desativado!");
			}

			return true;
		} else {
			return false;
		}
	}

	public void startDiscovery () {
		startDiscovery (false);
	}

	public void startDiscovery (boolean includePairedDevices) {
		this.includePairedDevices = includePairedDevices;

		Log.d ("bluetooth_helper", "Trying to list all available devices");

		if (checkAdapterAvailability ()) {
			Log.d ("bluetooth_helper", "Starting bluetooth device discovery...");
			this.activity.registerReceiver (this.bluetoothBroadcastReceiver, new IntentFilter (BluetoothDevice.ACTION_FOUND));
			this.activity.registerReceiver (this.bluetoothBroadcastReceiver, new IntentFilter (BluetoothAdapter.ACTION_DISCOVERY_STARTED));
			this.activity.registerReceiver (this.bluetoothBroadcastReceiver, new IntentFilter (BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

			this.bluetoothAdapter.startDiscovery ();
		}
	}

	private boolean checkAdapterAvailability () {
		Log.d ("bluetooth_helper", "Checking bluetooth adapter availability...");
		boolean state = true;

		// Get the default bluetooth adapter
		this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter ();
		// Check if the device has a valid one
		if (this.bluetoothAdapter == null) {
			state = false;
			Log.d ("bluetooth_helper", "No bluetooth adapter in the running device");
			this.listener.onBluetoothError ("O seu dispositivo não possui nenhum adaptador Bluetooth!");
		}

		// Check if bluetooth adapter is active
		if (!this.bluetoothAdapter.isEnabled ()) {
			// If not, request user to enable it!
			state = false;
			Log.d ("bluetooth_helper", "Bluetooth adapter not enabled");
			openBluetoothSettings ();
		}

		// Check if location provider is granted
		if (ContextCompat.checkSelfPermission (activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			// If not, request user to enable it!
			state = false;
			Log.d ("bluetooth_helper", "App doesn't has location permission granted");
			ActivityCompat.requestPermissions (
					activity,
					new String[] { Manifest.permission.ACCESS_COARSE_LOCATION },
					LOCATION_ENABLE_REQUEST_CODE
			);
		}

		return state;
	}
	//</editor-fold>

	//<editor-fold defaultstate="Collapsed" desc="Internal utils">
	private void openBluetoothSettings () {
		Log.d ("bluetooth_helper", "Opening bluetooth settngs screen...");
		Intent intent = new Intent (BluetoothAdapter.ACTION_REQUEST_ENABLE);
		this.activity.startActivityForResult (intent, BLUETOOTH_ENABLE_REQUEST_CODE);
	}

	public boolean handlePermissionsResult (int requestCode, String[] permissions, int[] grantResults) {
		if (requestCode == LOCATION_ENABLE_REQUEST_CODE) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				this.startDiscovery ();
			} else {
				this.listener.onBluetoothError ("Permissão de GPS negada!");
			}

			return true;
		} else {
			return false;
		}
	}
	//</editor-fold>

	public interface BluetoothScanListener {

		void onBluetoothDeviceFound (BluetoothDevice device, boolean paired);

		void onBluetoothScanEnd (ArrayList<BluetoothDevice> deviceList);

		void onBluetoothError (String message);

	}
}
