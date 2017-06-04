package com.easy.bluetoothutils.interfaces;

import com.easy.bluetoothutils.BluetoothUtils;

import java.util.UUID;

/**
 * Created by Jrtou on 2017/5/30.
 */

public interface InitializeSetting {
    BluetoothUtils setUUID(UUID uuid);

    BluetoothUtils setUUID(String uuid);

    /**
     * default byte[1024]
     *
     * @param size
     */
    BluetoothUtils setBufferSize(byte[] size);

    /**
     * default 10sec
     *
     * @param millisecond
     */
    BluetoothUtils setScanTime(int millisecond);

    void initializeBT(BTStatusCallBack btStatusCallBack);
}
