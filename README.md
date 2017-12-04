# UsbHid
The custom USB HID host library for Android. It supports SDK Version above than or equals to 12

# Usage
```
compile "com.benlypan:UsbHid:0.1.0"
```

# QuickStart
```
UsbHidDevice device = UsbHidDevice.factory(context, vid, pid);
device.open(this, new OnUsbHidDeviceListener() {
    @Override
    public void onUsbHidDeviceConnected(UsbHidDevice device) {
        byte[] sendBuffer = new byte[64];
        sendBuffer[0] = 0x01;
        device.write(sendBuffer);
        byte[] readBuffer = device.read(64);
    }

    @Override
    public void onUsbHidDeviceConnectFailed(UsbHidDevice device) {

    }
});
```
For detail, please read the source code.

# License
MIT