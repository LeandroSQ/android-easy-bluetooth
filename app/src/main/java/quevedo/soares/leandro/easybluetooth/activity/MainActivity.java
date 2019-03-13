package quevedo.soares.leandro.easybluetooth.activity;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

import quevedo.soares.leandro.easybluetooth.R;
import quevedo.soares.leandro.easybluetooth.adapter.DeviceListAdapter;
import quevedo.soares.leandro.lib.BluetoothScanner;

public class MainActivity extends AppCompatActivity implements BluetoothScanner.BluetoothScanListener, DeviceListAdapter.OnBluetoothDeviceClickListener {

	private TextView tvHeader;
	private TextView tvNoDeviceFound;
	private RecyclerView rvDevices;
	private ConstraintLayout clFooter;
	private ImageView ivRefreshDevices;
	private ProgressBar pbDevices;

	private boolean inDiscoveryMode = false;

	private DeviceListAdapter adapter;
	private BluetoothScanner bluetoothScanner = new BluetoothScanner (this, this);

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
		this.adapter.clearList ();

		this.bluetoothScanner.startDiscovery ();
	}

	private void stopBluetoothDeviceScan () {
		this.inDiscoveryMode = false;
		this.pbDevices.setVisibility (View.GONE);
		this.ivRefreshDevices.setVisibility (View.VISIBLE);
	}

	@Override
	protected void onActivityResult (int requestCode, int resultCode, @Nullable Intent data) {
		this.bluetoothScanner.handleActivityResult (requestCode, resultCode, data);

		super.onActivityResult (requestCode, resultCode, data);
	}

	@Override
	public void onRequestPermissionsResult (int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		this.bluetoothScanner.handlePermissionsResult (requestCode, permissions, grantResults);
		super.onRequestPermissionsResult (requestCode, permissions, grantResults);
	}

	@Override
	public void onBluetoothDeviceFound (BluetoothDevice device, boolean paired) {
		this.adapter.addDevice (new DeviceListAdapter.Item (device, paired));
	}

	@Override
	public void onBluetoothScanEnd (ArrayList<BluetoothDevice> deviceList) {
		stopBluetoothDeviceScan ();

		if (deviceList.isEmpty ()) {
			tvNoDeviceFound.setVisibility (View.VISIBLE);
		} else {
			tvNoDeviceFound.setVisibility (View.GONE);
		}
	}

	@Override
	public void onBluetoothError (String message) {
		stopBluetoothDeviceScan ();

		showError (message);
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
		if (inDiscoveryMode) { this.bluetoothScanner.stopDiscovery (); }

		Intent intent = new Intent (this, ControlPanelActivity.class);
		intent.putExtra ("device", device);
		ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation (this, tvHeader, "header");
		startActivity (intent, options.toBundle ());
	}
}
