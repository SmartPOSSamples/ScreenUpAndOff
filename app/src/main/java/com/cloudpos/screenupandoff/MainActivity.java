package com.cloudpos.screenupandoff;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.wizarpos.wizarviewagentassistant.aidl.ISystemExtApi;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ServiceConnection {

    private PowerManager.WakeLock mWakeLock;
    private PowerManager powerManager;
    private ISystemExtApi systemExtApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
        initParams();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindSystemExtService();
    }

    protected boolean bindSystemExtService() {
        Intent intent = new Intent();
        ComponentName comp = new ComponentName("com.wizarpos.wizarviewagentassistant",
                "com.wizarpos.wizarviewagentassistant.SystemExtApiService");
        intent.setPackage("com.wizarpos.wizarviewagentassistant");
        intent.setComponent(comp);
        boolean isSuccess = bindService(intent, this, Context.BIND_AUTO_CREATE);
        return isSuccess;
    }

    private void initUI() {
        Button mSleep = findViewById(R.id.sleep);
        Button wakeup = findViewById(R.id.locknow);
        mSleep.setOnClickListener(this);
        wakeup.setOnClickListener(this);
    }

    private void initParams() {
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sleep:
                setScreenOffTime();
                break;
            case R.id.locknow:
                goToLockNow();
                break;
        }
    }

    private void setScreenOffTime() {
        try {
            boolean result = systemExtApi.setScreenOffTimeout(15000);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void goToLockNow() {
        try {
            boolean result = systemExtApi.setDeviceOwner(this.getPackageName(), LockReceiver.class.getName());
            if (result) {
                DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                devicePolicyManager.lockNow();
                SystemClock.sleep(2000);
                mWakeLock = powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP
                        | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, getClass().getName());
                wakeUp();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Wake up screen
     */
    private void wakeUp() {
        mWakeLock.acquire();
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        try {
            systemExtApi = ISystemExtApi.Stub.asInterface(service);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
}