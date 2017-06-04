package com.easy.bluetoothutils.constants;

/**
 * Created by Jrtou on 2017/5/30.
 */

public enum BTSocket {
    ERROR_UUID_IS_NULL, //uuid is null
    ERROR_CAN_NOT_GET_DEVICE,//can not create Rf comm Socket To Service Record device
    ERROR_CAN_NOT_CONNECT, //socket can not connect
    ERROR_CAN_NOT_GET_STREAM,//can not get socket stream
    ERROR_CON_NOT_READ,//input stream read error
    ERROR_CAN_NOT_WRITE//output stream write error
}
