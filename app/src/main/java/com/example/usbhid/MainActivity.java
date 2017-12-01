package com.example.usbhid;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.benlypan.usbhid.OnUsbHidDeviceListener;
import com.benlypan.usbhid.UsbHidDevice;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.button_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                test();
            }
        });
    }

    private void test() {
        UsbHidDevice device = UsbHidDevice.factory(this, 0x0680, 0x0180);
        if (device == null) {
            return;
        }
        device.open(this, new OnUsbHidDeviceListener() {
            @Override
            public void onUsbHidDeviceConnected(UsbHidDevice device) {
                byte[] sendBuffer = new byte[64];
                sendBuffer[0] = 0x01;
                sendBuffer[1] = 0x03;
                device.write(sendBuffer);
                byte[] result = device.read(64);
            }

            @Override
            public void onUsbHidDeviceConnectFailed(UsbHidDevice device) {

            }
        });
    }
}
