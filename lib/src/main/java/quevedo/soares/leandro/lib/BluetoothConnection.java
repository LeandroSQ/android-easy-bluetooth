package quevedo.soares.leandro.lib;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @author Leandro Soares Quevedo
 * @since 23/02/2019
 * This class establishes a bluetooth connection with Master and Slave functionalities
 **/
public class BluetoothConnection {

	// Configuration
	private long connectionRetryTime = 1000L;
	private String commandTerminator = "\r\n";
	private UUID connectionUuid = UUID.fromString ("00001101-0000-1000-8000-00805f9b34fb");

	// Internal variables
	private BluetoothDevice bluetoothDevice;
	private BluetoothSocket socket;

	// Listeners
	private BluetoothConnectionProtocol connectionProtocol;
	private BluetoothConnectionListener listener;

	//<editor-fold defaultstate="collapsed" desc="Constructors">
	public BluetoothConnection (BluetoothDevice bluetoothDevice, BluetoothConnectionListener listener) {
		this.bluetoothDevice = bluetoothDevice;
		this.listener = listener;
	}

	public BluetoothConnection (BluetoothDevice bluetoothDevice, BluetoothConnectionProtocol connectionProtocol, BluetoothConnectionListener listener) {
		this.bluetoothDevice = bluetoothDevice;
		this.connectionProtocol = connectionProtocol;
		this.listener = listener;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Connection methods">
	/**
	 * This methods establishes a connection with the specified bluetooth device
	 *
	 * @return True if the operation was successful
	 **/
	public boolean connect () {
		if (this.isConnected ()) return false;

		Log.e ("bluetooth_connection", "Connecting to device...");
		try {
			// Establish a connection with the device
			this.socket = bluetoothDevice.createRfcommSocketToServiceRecord (this.connectionUuid);
			this.socket.connect ();

			// Check for custom connection protocols
			if (this.connectionProtocol != null) {
				if (this.connectionProtocol.testBluetoothConnectionProtocol ()) {
					// The connection was established
					if (this.listener != null) this.listener.onBluetoothConnected ();
					Log.d ("bluetooth_connection", "The device has passed the custom protocol tests!");

					this.listenForIncomingData ();
				} else {
					// The connection was unsuccessful
					// Disconnect from the device
					this.disconnect ();
					// Try again in the specified time
					new android.os.Handler ().postDelayed (this::connect, this.connectionRetryTime);

					Log.e ("bluetooth_connection", "The device has failed in the custom protocol tests!");
				}
			} else {
				// Otherwise a successful connection wast established
				if (this.listener != null) this.listener.onBluetoothConnected ();
				Log.d ("bluetooth_connection", "The device has been connected successfully");

				this.listenForIncomingData ();
			}
		} catch (Exception e) {
			// An error has occurred, show error message
			e.printStackTrace ();
			this.disconnect ();

			this.callListenerError ("Não foi possível estabelecer conexão com o dispositivo", e);

			// Try again in the specified time
			new android.os.Handler ().postDelayed (this::connect, connectionRetryTime);
		}

		return false;
	}

	/**
	 * This methods disconnects the bluetooth device
	 *
	 * @return True if the operation was successful
	 **/
	public boolean disconnect () {
		Log.d ("bluetooth_connection", "Disconnecting from device...");

		if (this.isConnected ()) {
			try {
				if (this.connectionProtocol != null) {
					if (this.connectionProtocol.testBluetoothDisconnectionProtocol ()) {
						Log.d ("bluetooth_connection", "The device has passed the custom protocol tests!");
					} else {
						Log.e ("bluetooth_connection", "The device has failed in the custom protocol tests!");
					}
				}

				this.socket.close ();
				this.socket = null;

				if (this.listener != null) this.listener.onBluetoothDisconnected ();

				return true;
			} catch (IOException e) {
				// An error has occurred, show error message
				e.printStackTrace ();

				this.callListenerError ("Não foi possível desconectar o dispositivo", e);
			}
		}

		return false;
	}

	/**
	 * @return The bluetooth connection state
	 **/
	public boolean isConnected () {
		return this.socket != null && this.socket.isConnected ();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Message sending methods">

	/**
	 * This methods send a single byte message to the bluetooth device
	 *
	 * @param message The specified message
	 * @return True if the operation was successful
	 **/
	public boolean send (byte message) {
		Log.d ("bluetooth_connection", "Sending message to device...");

		try {
			if (this.isConnected ()) {
				// Send the message bytes
				this.socket.getOutputStream ().write (message);
				// Send the command terminator
				this.socket.getOutputStream ().write (commandTerminator.getBytes (StandardCharsets.UTF_8));
				return true;
			}
		} catch (Exception e) {
			// An error has occurred, show error message
			Log.e ("bluetooth_connection", "Lost connection with message: " + e.getMessage ());
			e.printStackTrace ();

			this.disconnect ();
			this.callListenerError ("Conexão perdida!", e);
		}

		return false;
	}

	/**
	 * This methods send an array of bytes to the bluetooth device
	 *
	 * @param message The specified message
	 * @return True if the operation was successful
	 **/
	public boolean send (byte[] message) {
		Log.d ("bluetooth_connection", "Sending message to device...");

		try {
			if (this.isConnected ()) {
				// Send the message bytes
				this.socket.getOutputStream ().write (message);
				// Send the command terminator
				this.socket.getOutputStream ().write (commandTerminator.getBytes (StandardCharsets.UTF_8));
				return true;
			}
		} catch (Exception e) {
			// An error has occurred, show error message
			Log.e ("bluetooth_connection", "Lost connection with message: " + e.getMessage ());
			e.printStackTrace ();

			this.disconnect ();
			this.callListenerError ("Conexão perdida!", e);
		}

		return false;
	}

	/**
	 * This methods send a single 32bit integer to the bluetooth device
	 *
	 * @param message The specified message
	 * @return True if the operation was successful
	 **/
	public boolean send (int message) {
		return this.send ((byte) message);
	}

	/**
	 * This methods send a single character to the bluetooth device
	 *
	 * @param message The specified message
	 * @return True if the operation was successful
	 **/
	public boolean send (char message) {
		return this.send ((byte) message);
	}

	/**
	 * This methods entire String message to the bluetooth device
	 *
	 * @param message The specified message
	 * @return True if the operation was successful
	 **/
	public boolean send (String message) {
		return this.send (message.getBytes (StandardCharsets.UTF_8));
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Message receiving methods">
	private void listenForIncomingData () {
		new Thread (() -> {
			try {
				StringBuilder messageBuffer = new StringBuilder ();
				InputStream inputStream = socket.getInputStream ();
				byte[] chunk = new byte[256];
				int chunkByteCount;

				Log.d ("bluetooth_connection", "Listening for incoming data...");
				while (this.isConnected ()) {
					try {
						// Read the incoming message part
						chunkByteCount = inputStream.read (chunk);

						// Appends the incoming chunk to the message buffer
						messageBuffer.append (new String (chunk, 0, chunkByteCount));

						// Check if we've got a valid message
						int terminatorIndex = messageBuffer.indexOf (this.commandTerminator);
						if (terminatorIndex != -1) {
							// Treat the message
							String message = messageBuffer.substring (0, terminatorIndex);
							// Call the listener
							Log.d ("bluetooth_connection", "Received '" + message + "' message from device!");
							this.callListenerMessageReceived (message);

							// Remove the message from the buffer
							if (terminatorIndex + this.commandTerminator.length () >= messageBuffer.length ()) {
								messageBuffer.setLength (0);
							} else {
								messageBuffer = new StringBuilder (message.substring (terminatorIndex + 1));
							}
						}

					} catch (Exception e) {
						Log.e ("bluetooth_connection", "Lost connection with message: " + e.getMessage ());
						e.printStackTrace ();

						if (this.isConnected ()) {
							this.disconnect ();

							this.callListenerError ("Conexão perdida!", e);
						}

						break;
					}
				}
			} catch (Exception e) {
				Log.e ("bluetooth_connection", "Lost connection with message: " + e.getMessage ());
				e.printStackTrace ();

				if (this.isConnected ()) {
					this.disconnect ();

					this.callListenerError ("Conexão perdida!", e);
				}
			}
		}).start ();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Internal utils">

	/**
	 * This methods uses the main thread to run code
	 * Because if we just execute them, we won't be able to manipulate views or anything related to the UI Thread
	 **/
	private void callListenerError (String message, Exception e) {
		if (this.listener != null) {
			new android.os.Handler (Looper.getMainLooper ()).post (() -> {
				this.listener.onBluetoothError (message, e);
			});
		}
	}

	/**
	 * This methods uses the main thread to run code
	 * Because if we just execute them, we won't be able to manipulate views or anything related to the UI Thread
	 **/
	private void callListenerMessageReceived (String message) {
		if (this.listener != null) {
			new android.os.Handler (Looper.getMainLooper ()).post (() -> {
				this.listener.onBluetoothMessageReceived (message);
			});
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters and setters">
	public long getConnectionRetryTime () {
		return connectionRetryTime;
	}

	public void setConnectionRetryTime (long connectionRetryTime) {
		this.connectionRetryTime = connectionRetryTime;
	}

	public String getCommandTerminator () {
		return commandTerminator;
	}

	public void setCommandTerminator (String commandTerminator) {
		this.commandTerminator = commandTerminator;
	}

	public UUID getConnectionUuid () {
		return connectionUuid;
	}

	public void setConnectionUuid (UUID connectionUuid) {
		this.connectionUuid = connectionUuid;
	}

	public BluetoothDevice getBluetoothDevice () {
		return bluetoothDevice;
	}

	//</editor-fold>

	public interface BluetoothConnectionListener {
		void onBluetoothError (String message, Exception exception);

		void onBluetoothConnected ();

		void onBluetoothDisconnected ();

		void onBluetoothMessageReceived (String message);
	}

	public interface BluetoothConnectionProtocol {
		boolean testBluetoothConnectionProtocol ();

		boolean testBluetoothDisconnectionProtocol ();
	}

}
