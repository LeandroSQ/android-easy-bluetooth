# Android Easy Bluetooth
A library that makes Bluetooth communication in Android easy and simple!  
API 17+  
Java 1.8 with lambda needed (Just configure on your module settings)  
Compatible with *Kotlin*

[![Release](https://jitpack.io/v/LeandroSQ/android-easy-bluetooth.svg)](https://jitpack.io/#LeandroSQ/android-easy-bluetooth)

Tutorial on how to install it via Gradle:
[Here](https://jitpack.io/#LeandroSQ/android-easy-bluetooth)

## Usage
This library is splitted in two main parts, the BluetoothScanner and BluetoothConnection.  
You can check this example [App](https://github.com/LeandroSQ/android-easy-bluetooth/tree/master/app/src/main)

### BluetoothScanner
This class scans for near Bluetooth devices, handles Bluetooth permissions and enabling.
```java
// Setting the listeners
BluetoothScanner scanner = new BluetoothScaner (MainActivity.this, new BluetoothScanner.BluetoothScanListener () { 
  // Use this if you want to display a device list in realtime
  public void onBluetoothDeviceFound (BluetoothDevice device, boolean paired) {	}

  // This retrieves all the available devices
  public void onBluetoothScanEnd (ArrayList<BluetoothDevice> deviceList) {	}
  
  // Use this to show error messages, alert dialogs, and so on...
  public void onBluetoothError (String message) {	}
});

// Starting the scan
scanner.requestAvailableDevices ();

// Stops the scan
scanner.stopDiscovery ();
```

And for the Permissions and Bluetooth hardware automatic enabling
```java
protected void onActivityResult (int requestCode, int resultCode, @Nullable Intent data) {
  if (!this.bluetoothScanner.handleActivityResult (requestCode, resultCode, data)) {
    // Another results handling here
    super.onActivityResult (requestCode, resultCode, data);
  }
}

public void onRequestPermissionsResult (int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
  if (!this.bluetoothScanner.handlePermissionsResult (requestCode, permissions, grantResults)) {
    // Another permissions results handling here
    super.onRequestPermissionsResult (requestCode, permissions, grantResults);
  }
}
```
### BluetoothConnection
This class manages and establishes a connection between an Android device and any Bluetooth adapter, also decodes, receives and send data.
```java
BluetoothConnection btManager = new BluetoothConnection (<bluetooth_device_here>, MainActivity.this, new BluetoothConnection.BluetoothConnectionListener () {
  // Use this to show error messages, alert dialogs, and so on...
  public void onBluetoothError (String message, Exception exception) { }
  // Called when the connection has been successful established
  public void onBluetoothConnected () { }
  // Called when the device has disconnected
  public void onBluetoothDisconnected () { }
  // Use this to treat data when received valid input
  public void onBluetoothMessageReceived (String message) { }
});

// For connecting with the device
btManager.connect ();

// For disconnecting the device
btManager.disconnect ();

// For sending data to the device
btManager.send ("Hello World");// For Strings
// Or
btManager.send (0); // For Ints, Bytes, Byte[] and Single char
```

#### Configuration
This library allows you to:
- Configure custom connection validation
- Configure custom disconnection validation
- Configure command terminators (Default: "\r\n" aka NewLine)
- Configure connection retry time (Default: 1000ms)
- Configure the connection UUID (Default: "00001101-0000-1000-8000-00805f9b34fb")

##### Custom connection and disconnection validation
If you, like me, used an Arduino module that doesn't has a way to know if either conected or not. You can use a custom protocol to validate the connection. Like sending a message for connection and one for disconnection, so the Arduino board can keep track of the Bluetooth connection state.  
Also this could be used for security, only accept devices that send you a password or whatever.
```java
@Override
public boolean testBluetoothConnectionProtocol () {
  return this.bluetoothConnection.send (COMMAND_NOTIFY_CONNECTION);// Test if the message has been delivered successfully
}

@Override
public boolean testBluetoothDisconnectionProtocol () {
  return this.bluetoothConnection.send (COMMAND_NOTIFY_DISCONNECTION);// Test if the message has been delivered successfully
}
```

##### Custom command terminator
```java
btManager.setCommandTerminator ("\r\n");// It will be used for receiving and send data, be aware of that!
```

##### Custom connection retry time
```java
btManager.setConnetionRetryTime (250L);// In milliseconds
```

##### Custom connection UUID
```java
btManager.setConnectionUuid (UUID.fromString ("<custom_uuid_here>"));
```
