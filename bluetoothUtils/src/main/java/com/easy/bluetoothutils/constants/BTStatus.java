package com.easy.bluetoothutils.constants;

/**
 * Created by Jrtou on 2017/5/22.
 * NOT_SUPPORT_BT: 不支持/無 藍芽裝置
 */

public enum BTStatus {
    NOT_SUPPORT_BT(100), UUID_NULL(110), CONNECT_ERROR(120);

    private int value;

    BTStatus(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
