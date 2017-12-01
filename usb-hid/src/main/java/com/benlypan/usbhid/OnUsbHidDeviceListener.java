package com.benlypan.usbhid;

/**
 * Created by PanGenfu on 2017/12/1.
 */

public interface OnUsbHidDeviceListener {
    void onUsbHidDeviceConnected(UsbHidDevice device);
    void onUsbHidDeviceConnectFailed(UsbHidDevice device);
}
