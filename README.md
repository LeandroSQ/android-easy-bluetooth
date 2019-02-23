# Android Easy Bluetooth
A library that makes Bluetooth communication in Android easy and simple!
API 17+
Java 1.8 with lambda needed (Just configure on your module settings)
Compatible with *Kotlin*

## Usage
This library is splitted in two main parts, the BluetoothScanner and BluetoothConnection.
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


* Important notice * - This automatically assumes the end of messages to be a '\n' character.
