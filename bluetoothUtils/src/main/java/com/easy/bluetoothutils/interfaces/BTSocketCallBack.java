package com.easy.bluetoothutils.interfaces;

import com.easy.bluetoothutils.constants.BTSocket;

/**
 * Created by Jrtou on 2017/5/27.
 */

public interface BTSocketCallBack {
    void onRead(Object readByte);

    void onError(BTSocket error);
}
