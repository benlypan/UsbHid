package com.benlypan.usbhid;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by PanGenfu on 2017/11/30.
 */

public class UsbHidDevice {
    private static final int INTERFACE_CLASS_HID = 3;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private UsbManager mUsbManager;
    private UsbDevice mUsbDevice;
    private UsbInterface mUsbInterface;
    private UsbDeviceConnection mConnection;
    private OnUsbHidDeviceListener mListener;
    private UsbEndpoint mInUsbEndpoint;
    private UsbEndpoint mOutUsbEndpoint;
    private Handler mHandler;

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                context.unregisterReceiver(this);
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) && device != null) {
                        openDevice();
                    } else {
                        onConnectFailed();
                    }
                }
            }
        }
    };

    public static UsbHidDevice[] enumerate(Context context, int vid, int pid) throws Exception {
        UsbManager usbManager = (UsbManager) context.getApplicationContext().getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            throw new Exception("no usb service");
        }

        Map<String, UsbDevice> devices = usbManager.getDeviceList();
        List<UsbHidDevice> usbHidDevices = new ArrayList<>();
        for (UsbDevice device : devices.values()) {
            if ((vid == 0 || device.getVendorId() == vid) && (pid == 0 || device.getProductId() == pid)) {
                for (int i = 0; i < device.getInterfaceCount(); i++) {
                    UsbInterface usbInterface = device.getInterface(i);
                    if (usbInterface.getInterfaceClass() == INTERFACE_CLASS_HID) {
                        UsbHidDevice hidDevice = new UsbHidDevice(device, usbInterface, usbManager);
                        usbHidDevices.add(hidDevice);
                    }
                }
            }
        }
        return usbHidDevices.toArray(new UsbHidDevice[usbHidDevices.size()]);
    }

    public static UsbHidDevice factory(Context context, int vid, int pid) {
        try {
            UsbHidDevice[] devices = enumerate(context, vid, pid);
            if (devices.length == 0) {
                return null;
            }
            return devices[0];
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static UsbHidDevice factory(Context context, int vid, int pid, String serialNumber) {
        try {
            UsbHidDevice[] devices = enumerate(context, vid, pid);
            for (UsbHidDevice device : devices) {
                if (device.getSerialNumber().equals(device.getSerialNumber())) {
                    return device;
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static UsbHidDevice factory(Context context, int vid, int pid, int deviceId) {
        try {
            UsbHidDevice[] devices = enumerate(context, vid, pid);
            for (UsbHidDevice device : devices) {
                if (device.getDeviceId() == deviceId) {
                    return device;
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private UsbHidDevice(UsbDevice usbDevice, UsbInterface usbInterface, UsbManager usbManager) {
        mUsbDevice = usbDevice;
        mUsbInterface = usbInterface;
        mUsbManager= usbManager;

        for (int i = 0; i < mUsbInterface.getEndpointCount(); i++) {
            UsbEndpoint endpoint = mUsbInterface.getEndpoint(i);
            int dir = endpoint.getDirection();
            int type = endpoint.getType();
            if (mInUsbEndpoint == null && dir == UsbConstants.USB_DIR_IN && type == UsbConstants.USB_ENDPOINT_XFER_INT) {
                mInUsbEndpoint = endpoint;
            }
            if (mOutUsbEndpoint == null && dir == UsbConstants.USB_DIR_OUT && type == UsbConstants.USB_ENDPOINT_XFER_INT) {
                mOutUsbEndpoint = endpoint;
            }
        }
    }

    public UsbDevice getUsbDevice() {
        return mUsbDevice;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public String getSerialNumber() {
        return mUsbDevice.getSerialNumber();
    }

    public int getDeviceId() {
        return mUsbDevice.getDeviceId();
    }

    public void open(Context context, OnUsbHidDeviceListener listener) {
        mListener = listener;
        mHandler = new Handler(context.getMainLooper());
        if (!mUsbManager.hasPermission(mUsbDevice)) {
            PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            context.registerReceiver(mUsbReceiver, filter);
            mUsbManager.requestPermission(mUsbDevice, permissionIntent);
        } else {
            openDevice();
        }
    }

    private void openDevice() {
        mConnection = mUsbManager.openDevice(mUsbDevice);
        if (mConnection == null) {
            onConnectFailed();
            return;
        }
        if (!mConnection.claimInterface(mUsbInterface, true)) {
            onConnectFailed();
            return;
        }
        if (Build.VERSION.SDK_INT >= 21) {
            mConnection.setInterface(mUsbInterface);
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onUsbHidDeviceConnected(UsbHidDevice.this);
                }
            }
        });
    }

    private void onConnectFailed() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onUsbHidDeviceConnectFailed(UsbHidDevice.this);
                }
            }
        });
    }

    public void close() {
        mConnection.close();
    }

    public void write(byte[] data) {
        write(data, 0, data.length);
    }

    public void write(byte[] data, int size) {
        write(data, 0, size);
    }

    public void write(byte[] data, int offset, int size) {
        if (offset != 0) {
            data = Arrays.copyOfRange(data, offset, size);
        }
        if (mOutUsbEndpoint == null) {
            // use the control endpoint

        } else {
            mConnection.bulkTransfer(mOutUsbEndpoint, data, size, 1000);
        }
    }

    public byte[] read(int size) {
        return read(size, -1);
    }

    public byte[] read(int size, int timeout) {
        byte[] buffer = new byte[size];
        int bytesRead = mConnection.bulkTransfer(mInUsbEndpoint, buffer, size, timeout);
        if (bytesRead < size) {
            buffer = Arrays.copyOfRange(buffer, 0, bytesRead);
        }
        return buffer;
    }
}
