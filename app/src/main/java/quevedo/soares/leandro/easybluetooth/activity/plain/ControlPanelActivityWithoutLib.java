package quevedo.soares.leandro.easybluetooth.activity.plain;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import quevedo.soares.leandro.easybluetooth.R;

public class ControlPanelActivityWithoutLib extends AppCompatActivity {

	private boolean isConnected = false;
	private boolean ledState = false;
	private BluetoothDevice device;
	private BluetoothSocket socket;
	private TextView tvDeviceName;
	private ImageButton btnState;
	private Vibrator vibrator;

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_control_panel);

		// Change the status bar color
		getWindow ().setStatusBarColor (Color.parseColor ("#27ae60"));
		loadComponents ();
	}

	@Override
	protected void onDestroy () {
		super.onDestroy ();
		this.disconnectFromDevice ();
	}

	@Override
	protected void onResume () {
		super.onResume ();

		initializeCommunication ();
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

	private void loadComponents () {
		vibrator = (Vibrator) getSystemService (Context.VIBRATOR_SERVICE);

		tvDeviceName = findViewById (R.id.tvDeviceName);
		btnState = findViewById (R.id.btnState);

		this.btnState.setOnClickListener (view -> {
			// Check if we're able to send data
			if (socket == null || !socket.isConnected ()) {
				return;
			}

			ledState = !ledState;

			if (sendMessage ((byte) (ledState ? 0 : 1))) {
				if (ledState) {
					this.btnState.setBackgroundResource (R.drawable.shape_power_button_off);
				} else {
					this.btnState.setBackgroundResource (R.drawable.shape_power_button_on);
				}

				vibrator.vibrate (70);
			}
		});
	}

	private void initializeCommunication () {
		if (this.socket != null && this.socket.isConnected ()) return;

		final UUID CONNECTION_UUID = UUID.fromString ("00001101-0000-1000-8000-00805f9b34fb");
		this.device = getIntent ().getParcelableExtra ("device");
		this.tvDeviceName.setText (String.format ("%s - %s", this.device.getName (), this.device.getAddress ()));

		try {
			socket = device.createRfcommSocketToServiceRecord (CONNECTION_UUID);
			socket.connect ();

			if (sendMessage ((byte) 3)) {
				Log.d ("bluetooth_helper", "Connection stabilised with success!");
				this.listenForIncomingData ();
			} else throw new Exception ("Não foi possível conectar-se com o dispositivo!");
		} catch (Exception e) {
			e.printStackTrace ();

			disconnectFromDevice ();
			showError (e.getMessage ());

			// Schedulle a retry
			tvDeviceName.postDelayed (this::initializeCommunication, 1000);

		}
	}

	private void disconnectFromDevice () {
		if (socket != null && socket.isConnected ()) {
			try {
				// Notify disconnection
				sendMessage ((byte) 2);

				// Closes the socket communication
				socket.close ();
				this.tvDeviceName.setText ("Disconnected");
			} catch (IOException e) {
				e.printStackTrace ();
				showError (e.getMessage ());
			}
		}
	}

	private boolean sendMessage (byte message) {
		if (socket != null) {
			try {
				socket.getOutputStream ().write (message);
				return true;
			} catch (Exception e) {
				e.printStackTrace ();

				this.disconnectFromDevice ();

				showError ("Conexão perdida!");
				Log.e ("bluetooth_helper", "Ending connection because an exception was thrown...");
			}
		}

		return false;
	}

	private void handleMessage (String message) {
		if (message.startsWith ("led")) {
			switch (message.charAt (3)) {
				case '0':
					this.ledState = true;
					this.btnState.setBackgroundResource (R.drawable.shape_power_button_on);
					break;
				case '1':
					this.ledState = false;
					this.btnState.setBackgroundResource (R.drawable.shape_power_button_off);
					break;
			}
		}
	}

	private void listenForIncomingData () {
		new Thread (() -> {
			try {
				StringBuilder messageBuffer = new StringBuilder ();
				InputStream inputStream = socket.getInputStream ();
				byte[] chunk = new byte[256];
				int bytes;

				Log.d ("bluetooth_helper", "Listening for incoming data...");
				while (socket.isConnected ()) {
					try {
						bytes = inputStream.read (chunk);
						// Appends the incoming chunk to the message buffer
						messageBuffer.append (new String (chunk, 0, bytes));
						// Check if we've got a valid message
						int newLineIndex = messageBuffer.indexOf ("\n");
						if (newLineIndex != -1) {
							// Treat the message
							String message = messageBuffer.substring (0, newLineIndex);
							runOnUiThread (() -> handleMessage (message));
							Log.d ("bluetooth_helper", "Received '" + message + "' message from device!");

							// Remove the message from the buffer
							if (newLineIndex + 1 >= messageBuffer.length ()) {
								messageBuffer.setLength (0);
							} else {
								messageBuffer = new StringBuilder (message.substring (newLineIndex + 1));
							}
						}
					} catch (Exception e) {
						e.printStackTrace ();
						Log.e ("bluetooth_helper", "Ending connection because an exception was thrown...");
						this.disconnectFromDevice ();

						showError ("Conexão perdida!");
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace ();
				Log.e ("bluetooth_helper", "Ending connection because an exception was thrown...");
				this.disconnectFromDevice ();

				showError ("Conexão perdida!");
			}
		}).start ();
	}
}
