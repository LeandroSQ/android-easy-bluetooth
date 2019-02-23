package quevedo.soares.leandro.easybluetooth.activity.plain;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import quevedo.soares.leandro.easybluetooth.R;
import quevedo.soares.leandro.easybluetooth.adapter.DeviceListAdapter;

public class MainActivityWithoutLib extends AppCompatActivity implements DeviceListAdapter.OnBluetoothDeviceClickListener {

	private static final int BLUETOOTH_ENABLE_REQUEST_CODE = 1902;
	private static final int LOCATION_ENABLE_REQUEST_CODE = 1932;

	private TextView tvHeader;
	private TextView tvNoDeviceFound;
	private RecyclerView rvDevices;
	private ConstraintLayout clFooter;
	private ImageView ivRefreshDevices;
	private ProgressBar pbDevices;

	private boolean inDiscoveryMode = false;

	private DeviceListAdapter adapter;
	private BluetoothAdapter bluetoothAdapter;

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_main);
		loadComponents ();
	}

	private void loadComponents () {
		tvHeader = findViewById (R.id.tvHeader);
		tvNoDeviceFound = findViewById (R.id.tvNoDeviceFound);
		rvDevices = findViewById (R.id.rvDevices);
		clFooter = findViewById (R.id.clFooter);
		ivRefreshDevices = findViewById (R.id.ivRefreshDevices);
		pbDevices = findViewById (R.id.pbDevices);

		adapter = new DeviceListAdapter (this.rvDevices);
		adapter.setListener (this);

		clFooter.setOnClickListener ((view) -> {
			if (this.inDiscoveryMode) return;

			startBluetoothDeviceScan ();
		});
	}

	@Override
	protected void onStart () {
		super.onStart ();
		if (!this.inDiscoveryMode)
			startBluetoothDeviceScan ();
	}

	private void startBluetoothDeviceScan () {
		this.inDiscoveryMode = true;
		this.tvNoDeviceFound.setVisibility (View.GONE);
		this.pbDevices.setVisibility (View.VISIBLE);
		this.ivRefreshDevices.setVisibility (View.GONE);

		this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter ();
		if (this.bluetoothAdapter == null) {
			this.stopBluetoothDeviceScan ();
			Log.d ("bluetooth_helper", "No bluetooth adapter in the running device");
			this.showError ("O seu dispositivo não possui nenhum adaptador Bluetooth!");
			stopBluetoothDeviceScan ();
		}

		// Check if bluetooth adapter is active
		if (!this.bluetoothAdapter.isEnabled ()) {
			// If not, request user to enable it!
			this.stopBluetoothDeviceScan ();
			Log.d ("bluetooth_helper", "Bluetooth adapter not enabled");
			// Open bluetooth settings
			Log.d ("bluetooth_helper", "Opening bluetooth settngs screen...");
			Intent intent = new Intent (BluetoothAdapter.ACTION_REQUEST_ENABLE);
			this.startActivityForResult (intent, BLUETOOTH_ENABLE_REQUEST_CODE);
		}

		// Check if location provider is granted
		if (ContextCompat.checkSelfPermission (this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			// If not, request user to enable it!
			this.stopBluetoothDeviceScan ();
			Log.d ("bluetooth_helper", "App doesn't has location permission granted");
			ActivityCompat.requestPermissions (this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_ENABLE_REQUEST_CODE);
		}

		if (this.inDiscoveryMode) {
			BroadcastReceiver bluetoothBroadcastReceiver = new BroadcastReceiver () {
				@Override
				public void onReceive (Context context, Intent intent) {
					switch (intent.getAction ()) {
						case BluetoothDevice.ACTION_FOUND:
							// A device was found!
							BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);
							// Append to the list
							adapter.addDevice (new DeviceListAdapter.Item (device, bluetoothAdapter.getBondedDevices ().contains (device)));
							Log.d ("bluetooth_helper", "Found bluetooth device with id of " + device.getAddress ());
							break;
						case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
							// The scan has just started, clear the device list
							Log.d ("bluetooth_helper", "Bluetooth discovery started");
							adapter.clearList ();
							break;
						case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
							// The scan has ended, notify the listener
							Log.d ("bluetooth_helper", "Bluetooth discovery finished");

							if (adapter.isEmpty ()) {
								tvNoDeviceFound.setVisibility (View.VISIBLE);
							} else {
								tvNoDeviceFound.setVisibility (View.GONE);
							}

							unregisterReceiver (this);
							break;
					}
				}
			};

			Log.d ("bluetooth_helper", "Starting bluetooth device discovery...");
			this.registerReceiver (bluetoothBroadcastReceiver, new IntentFilter (BluetoothDevice.ACTION_FOUND));
			this.registerReceiver (bluetoothBroadcastReceiver, new IntentFilter (BluetoothAdapter.ACTION_DISCOVERY_STARTED));
			this.registerReceiver (bluetoothBroadcastReceiver, new IntentFilter (BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
			this.bluetoothAdapter.startDiscovery ();
		}
	}

	private void stopBluetoothDeviceScan () {
		this.bluetoothAdapter.cancelDiscovery ();
		this.inDiscoveryMode = false;
		this.pbDevices.setVisibility (View.GONE);
		this.ivRefreshDevices.setVisibility (View.VISIBLE);
	}

	@Override
	protected void onActivityResult (int requestCode, int resultCode, @Nullable Intent data) {
		if (requestCode == BLUETOOTH_ENABLE_REQUEST_CODE) {
			if (this.bluetoothAdapter.isEnabled ()) {
				this.startBluetoothDeviceScan ();
			} else {
				this.showError ("O seu adaptador Bluetooth está desativado!");
			}
		} else super.onActivityResult (requestCode, resultCode, data);
	}

	@Override
	public void onRequestPermissionsResult (int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == LOCATION_ENABLE_REQUEST_CODE) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				this.startBluetoothDeviceScan ();
			} else {
				this.showError ("Permissão de GPS negada!");
			}
		} else super.onRequestPermissionsResult (requestCode, permissions, grantResults);
	}


	private void showError (String message) {
		Drawable icon = getDrawable (R.drawable.ic_bluetooth).mutate ();
		DrawableCompat.setTint (icon, Color.parseColor ("#e74c3c"));

		new AlertDialog.Builder (this)
				.setTitle ("Atenção")
				.setIcon (icon)
				.setMessage (message)
				.setPositiveButton ("OK", null)
				.show ();
	}

	@Override
	public void onBluetoothDeviceSelected (BluetoothDevice device) {
		if (inDiscoveryMode) { stopBluetoothDeviceScan (); }

		Intent intent = new Intent (this, ControlPanelActivityWithoutLib.class);
		intent.putExtra ("device", device);
		ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation (this, tvHeader, "header");
		startActivity (intent, options.toBundle ());
	}
}
