package com.easy.bluetoothutils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

import com.easy.bluetoothutils.interfaces.BTReceiverCallBack;
import com.easy.bluetoothutils.interfaces.BTSocketCallBack;
import com.easy.bluetoothutils.interfaces.BTStatusCallBack;
import com.easy.bluetoothutils.interfaces.InitializeSetting;
import com.easy.bluetoothutils.socket.SocketManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED;
import static com.easy.bluetoothutils.constants.BTStatus.NOT_SUPPORT_BT;
import static com.easy.bluetoothutils.constants.BTStatus.UUID_NULL;

/**
 * Created by Jrtou on 2017/5/21.
 */

public class BluetoothUtils extends BroadcastReceiver implements InitializeSetting {
    private static final String TAG = BluetoothUtils.class.getSimpleName();

    private static BluetoothUtils mBluetoothUtils;
    private Context mContext;
    private Handler mHandler = new Handler();

    private BluetoothAdapter mBluetoothAdapter = null;

    private int mScanTime = 10000;// Default:10sec  unit ms
    private byte[] mBufferSize = new byte[1024];// inputStream default 1024
    public UUID mUUID = null;

    private boolean isFoundDevice = false;

    private BTStatusCallBack mBTStatusCallBack;
    private Thread mBTStatusThread;

    private SocketManager mSocketManager;

    private BTSocketCallBack mBTSocketCallBack;
    private BTReceiverCallBack mBTReceiverCallBackCallBack;


    public static BluetoothUtils newInstance(Context context) {
        if (null == mBluetoothUtils) {
            mBluetoothUtils = new BluetoothUtils(context.getApplicationContext());
        }
        return mBluetoothUtils;
    }

    private BluetoothUtils(Context context) {
        this.mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (null == mBluetoothAdapter) {
            mBTStatusCallBack.onError(NOT_SUPPORT_BT);
        }
    }

    public boolean isBTEnable() {
        return mBluetoothAdapter.isEnabled();
    }

    public void enableBT() {
        if (!mBluetoothAdapter.isEnabled())
            mBluetoothAdapter.enable();
    }

    public void disableBT() {
        if (mBluetoothAdapter.isDiscovering())
            mBluetoothAdapter.cancelDiscovery();
        mBluetoothAdapter.disable();
    }

    /**
     * 註冊 BroadcastReceiver
     */
    private void registerReceiver() {
        //註冊藍芽 BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);//搜索狀態改變
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);//若藍芽設備狀態改變(ex:開→關)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);//搜索
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);//搜索完畢
        filter.addAction(ACTION_BOND_STATE_CHANGED);//配對狀態的改變
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);//連接裝置
        filter.addAction(BluetoothDevice.ACTION_FOUND);//找到裝置
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);//段開裝置
        mContext.registerReceiver(this, filter);//註冊這個功能
    }

    public void unregisterReceiver() {
        mContext.unregisterReceiver(this);
    }

    /**
     * default scan time 10 sec
     */
    public void onScan() {
        onScan(mScanTime);
    }

    /**
     * @param scanTime unit millisecond
     */
    public void onScan(int scanTime) {
        this.mScanTime = scanTime;

        if (mBluetoothAdapter.isDiscovering())
            mBluetoothAdapter.cancelDiscovery();

        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        final List<BluetoothDevice> deviceList = new ArrayList<>();
        for (BluetoothDevice device : devices) {
            deviceList.add(device);
        }

        if (deviceList.size() > 0) {
            isFoundDevice = true;
            mBTStatusCallBack.onPaired(deviceList);
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothAdapter.cancelDiscovery();
            }
        }, scanTime);

        mBluetoothAdapter.startDiscovery();
    }

    public void onConnect(final BluetoothDevice device) {
        Log.d(TAG, "onConnect: ");
        if (null == mUUID) {
            Log.e(TAG, "onConnect: " + UUID_NULL);
            mBTStatusCallBack.onError(UUID_NULL);
            return;
        }

        if (mBluetoothAdapter.isDiscovering())
            mBluetoothAdapter.cancelDiscovery();

        mSocketManager = SocketManager.newInstance(device);
        mSocketManager.buffer(mBufferSize)
                .uuid(mUUID)
                .setSocketCallBack(mBTSocketCallBack)
                .start();
    }

    public void write(byte cmd) {
        mSocketManager.write(cmd);
    }

    public void close() {
        if (mBTStatusThread != null) {
            mBTStatusThread.interrupt();
            mBTStatusThread = null;
        }

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (null != mBTReceiverCallBackCallBack)
            mBTReceiverCallBackCallBack.onReceiver(context, intent);

        switch (intent.getAction()) {
            case BluetoothDevice.ACTION_FOUND:
                Log.d(TAG, "onReceive: ACTION_FOUND");
                isFoundDevice = true;
                mBTStatusCallBack.onFound((BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
                break;
            case BluetoothDevice.ACTION_ACL_CONNECTED:
                Log.d(TAG, "onReceive: ACTION_ACL_CONNECTED");
                mBTStatusCallBack.onConnected();
                break;
            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                Log.d(TAG, "onReceive: ACTION_ACL_DISCONNECTED");
                mBTStatusCallBack.onDisconnected();
                break;
            case BluetoothAdapter.ACTION_STATE_CHANGED:
                Log.d(TAG, "onReceive: ACTION_STATE_CHANGED");
                mBTStatusCallBack.isOpenBT(mBluetoothAdapter.isEnabled());
                break;
            case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                Log.d(TAG, "onReceive: ACTION_DISCOVERY_STARTED");
                mBTStatusCallBack.onDiscoveryStarted();
                break;
            case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                Log.d(TAG, "onReceive: ACTION_DISCOVERY_FINISHED");

                if (!isFoundDevice) {
                    mBTStatusCallBack.onNotFound();
                }

                mBTStatusCallBack.onDiscoveryFinished();
                isFoundDevice = false;
                break;
            case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                Log.d(TAG, "onReceive: ACTION_BOND_STATE_CHANGED");
                mBTStatusCallBack.onBondStateChanged();
                break;
        }

    }

    public void setBTReceiverCallBackCallBack(BTReceiverCallBack btReceiverCallBackCallBack) {
        this.mBTReceiverCallBackCallBack = btReceiverCallBackCallBack;
    }

    public void setBTSocketCallBack(BTSocketCallBack btSocketCallBackCallBack) {
        this.mBTSocketCallBack = btSocketCallBackCallBack;
    }

    @Override
    public BluetoothUtils setUUID(UUID uuid) {
        mUUID = uuid;
        return this;
    }

    @Override
    public BluetoothUtils setUUID(String uuid) {
        mUUID = UUID.fromString(uuid);
        return this;
    }

    @Override
    public BluetoothUtils setBufferSize(byte[] size) {
        mBufferSize = size;
        return this;
    }

    @Override
    public BluetoothUtils setScanTime(int millisecond) {
        mScanTime = millisecond;
        return this;
    }

    @Override
    public void initializeBT(BTStatusCallBack btStatusCallBack) {
        registerReceiver();
        mBTStatusCallBack = btStatusCallBack;
    }
}
