package com.easy.bluetoothutils.interfaces;

import android.bluetooth.BluetoothDevice;

import com.easy.bluetoothutils.constants.BTStatus;

import java.util.List;

/**
 * Created by Jrtou on 2017/5/21.
 */

public interface BTStatusCallBack {
    void onError(BTStatus status);

    void onConnected();

    void onDisconnected();

    void onDiscoveryStarted();

    void onDiscoveryFinished();

    void isOpenBT(boolean isBTEnable);

    void onPaired(List<BluetoothDevice> devices);

    void onFound(BluetoothDevice device);

    void onNotFound();

    void onBondStateChanged();
}
