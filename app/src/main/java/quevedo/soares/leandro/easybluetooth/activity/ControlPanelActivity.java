package quevedo.soares.leandro.easybluetooth.activity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.transition.Fade;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import quevedo.soares.leandro.easybluetooth.R;
import quevedo.soares.leandro.lib.BluetoothConnection;

public class ControlPanelActivity extends AppCompatActivity implements BluetoothConnection.BluetoothConnectionListener, BluetoothConnection.BluetoothConnectionProtocol {

	private final static int COMMAND_NOTIFY_CONNECTION = 3;
	private final static int COMMAND_NOTIFY_DISCONNECTION = 2;
	private final static int COMMAND_TURN_LED_ON = 1;
	private final static int COMMAND_TURN_LED_OFF = 0;

	private boolean ledState = false;
	private BluetoothDevice device;
	private BluetoothConnection bluetoothConnection;

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

		this.bluetoothConnection.disconnect ();
	}

	@Override
	protected void onResume () {
		super.onResume ();

		new Handler ().post (this::startBluetoothCommunication);
	}

	@Override
	public void onBackPressed () {
		startExitAnimations ();
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
		this.vibrator = (Vibrator) getSystemService (Context.VIBRATOR_SERVICE);

		this.tvDeviceName = findViewById (R.id.tvDeviceName);
		this.btnState = findViewById (R.id.btnState);

		this.btnState.setOnClickListener (view -> {
			// Check if we're able to send data
			if (!this.bluetoothConnection.isConnected ()) { return; }

			// Toggle the led on and off
			ledState = !ledState;
			// Get the command identifier
			int command = ledState ? COMMAND_TURN_LED_ON : COMMAND_TURN_LED_OFF;

			// Send the command to the device
			if (this.bluetoothConnection.send (command)) {
				// Change the button color
				if (ledState) {
					this.btnState.setBackgroundResource (R.drawable.shape_power_button_on);
				} else {
					this.btnState.setBackgroundResource (R.drawable.shape_power_button_off);
				}

				// Vibrate the cellphone
				vibrator.vibrate (70);
			}
		});

		this.device = getIntent ().getParcelableExtra ("device");

		this.startEnterAnimations ();
	}

	private void startEnterAnimations () {
		Fade fade =new Fade ();
		fade.excludeTarget (tvDeviceName, true);
		fade.excludeTarget (btnState, true);
		getWindow ().setEnterTransition (fade);

		ScaleAnimation animation = new ScaleAnimation (
				0,
				1,
				0,
				1f,
				Animation.RELATIVE_TO_SELF,
				0.5f,
				Animation.RELATIVE_TO_SELF,
				0.5f
		);
		animation.setInterpolator (new OvershootInterpolator ());
		animation.setDuration (300);

		this.btnState.startAnimation (animation);
	}

	private void startExitAnimations () {
		ScaleAnimation animation = new ScaleAnimation (
				1,
				0,
				1,
				0,
				Animation.RELATIVE_TO_SELF,
				0.5f,
				Animation.RELATIVE_TO_SELF,
				0.5f
		);
		animation.setInterpolator (new AnticipateInterpolator ());
		animation.setDuration (300);
		animation.setFillAfter (true);
		animation.setFillBefore (true);
		animation.setFillEnabled (true);
		animation.setAnimationListener (new Animation.AnimationListener () {
			@Override
			public void onAnimationStart (Animation animation) {

			}

			@Override
			public void onAnimationEnd (Animation animation) {
				btnState.postDelayed (ControlPanelActivity.super::onBackPressed, 75);
			}

			@Override
			public void onAnimationRepeat (Animation animation) {

			}
		});

		this.btnState.startAnimation (animation);
	}

	private void startBluetoothCommunication () {
		if (this.bluetoothConnection == null)
			this.bluetoothConnection = new BluetoothConnection (this.device, this, this);

		this.bluetoothConnection.connect ();
	}

	@Override
	public void onBluetoothError (String message, Exception exception) {
		showError (message);
	}

	@Override
	public void onBluetoothConnected () {
		this.tvDeviceName.setText (String.format ("%s - %s", this.device.getName (), this.device.getAddress ()));
		this.btnState.setAlpha (1f);
	}

	@Override
	public void onBluetoothDisconnected () {
		this.tvDeviceName.setText ("Desconectado!");
		this.btnState.setAlpha (0.1f);
	}

	@Override
	public void onBluetoothMessageReceived (String message) {
		if (message.startsWith ("led")) {
			switch (message.charAt (3)) {
				case '0':
					this.ledState = false;
					this.btnState.setBackgroundResource (R.drawable.shape_power_button_off);
					break;
				case '1':
					this.ledState = true;
					this.btnState.setBackgroundResource (R.drawable.shape_power_button_on);
					break;
			}
		}
	}

	@Override
	public boolean testBluetoothConnectionProtocol () {
		return this.bluetoothConnection.send (COMMAND_NOTIFY_CONNECTION);
	}

	@Override
	public boolean testBluetoothDisconnectionProtocol () {
		return this.bluetoothConnection.send (COMMAND_NOTIFY_DISCONNECTION);
	}
}
