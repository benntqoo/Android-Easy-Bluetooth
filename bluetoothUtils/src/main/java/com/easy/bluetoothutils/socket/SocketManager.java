package com.easy.bluetoothutils.socket;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.easy.bluetoothutils.constants.BTSocket;
import com.easy.bluetoothutils.interfaces.BTSocketCallBack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static com.easy.bluetoothutils.constants.BTSocket.ERROR_CAN_NOT_GET_STREAM;
import static com.easy.bluetoothutils.constants.BTSocket.ERROR_CAN_NOT_WRITE;
import static com.easy.bluetoothutils.constants.BTSocket.ERROR_CON_NOT_READ;
import static com.easy.bluetoothutils.constants.BTSocket.ERROR_UUID_IS_NULL;

/**
 * Created by Jrtou on 2017/5/30.
 */

public class SocketManager {
    private static final String TAG = SocketManager.class.getSimpleName();

    private static SocketManager mInstanceSocket;
    private BluetoothDevice mDevice;
    private byte[] mBufferSize;
    private UUID mUUID;

    private SocketConnectThread mConnectThread;
    private SocketStreamThread mStreamThread;
    private BTSocketCallBack mBTSocketCallBack;

    private int mState;
    public static final int STATE_NONE = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    public static SocketManager newInstance(BluetoothDevice device) {
        if (null == mInstanceSocket) {
            mInstanceSocket = new SocketManager(device);
        }
        return mInstanceSocket;
    }

    private SocketManager(BluetoothDevice device) {
        mDevice = device;
        mState = STATE_NONE;
    }

    public void start() {
        close();
        mConnectThread = new SocketConnectThread(mDevice);
        mConnectThread.start();
    }

    public void connect(BluetoothSocket socket) {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mStreamThread != null) {
            mStreamThread.cancel();
            mStreamThread = null;
        }

        mStreamThread = new SocketStreamThread(socket);
        mStreamThread.start();
    }

    public void write(byte cmd) {
        SocketStreamThread writeThread;
        synchronized (this) {
            writeThread = mStreamThread;
        }
        writeThread.write(cmd);
    }

    public void close() {
        if (null != mStreamThread) {
            mStreamThread.cancel();
            mStreamThread = null;
        }
        if (null != mConnectThread) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
    }

    /**
     * Get socket
     */
    private class SocketConnectThread extends Thread {
        final BluetoothSocket mSocket;

        public SocketConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;

            if (null == mUUID) {
                mBTSocketCallBack.onError(ERROR_UUID_IS_NULL);
                Log.d(TAG, "SocketConnectThread: " + ERROR_UUID_IS_NULL.toString());
            }

            try {
                tmp = device.createRfcommSocketToServiceRecord(mUUID);
            } catch (IOException e) {
                e.printStackTrace();
                mState = STATE_NONE;
                mBTSocketCallBack.onError(BTSocket.ERROR_CAN_NOT_GET_DEVICE);
                Log.e(TAG, "SocketConnect: ", e);
            }
            mSocket = tmp;
            mState = STATE_CONNECTING;
            Log.i(TAG, "SocketConnectThread: mState="+STATE_CONNECTING);

        }

        @Override
        public void run() {
            try {
                mSocket.connect();
            } catch (IOException e) {
                e.printStackTrace();
                if (mBTSocketCallBack != null)
                    mBTSocketCallBack.onError(BTSocket.ERROR_CAN_NOT_CONNECT);
                mState = STATE_NONE;
                Log.e(TAG, "run: ", e);

                try {
                    mSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                    mState = STATE_NONE;
                }
                return;
            }

            // Reset the ConnectThread
            synchronized (this) {
                mConnectThread = null;
            }

            connect(mSocket);
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                mState = STATE_NONE;
            }
        }
    }

    private class SocketStreamThread extends Thread {
        final InputStream mInputStream;
        final OutputStream mOutputStream;

        public SocketStreamThread(BluetoothSocket socket) {
            InputStream inputTmp = null;
            OutputStream outputTmp = null;

            try {
                inputTmp = socket.getInputStream();
                outputTmp = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
                mBTSocketCallBack.onError(ERROR_CAN_NOT_GET_STREAM);
                Log.e(TAG, "SocketStreamThread: ", e);
                mState = STATE_NONE;
            }

            mInputStream = inputTmp;
            mOutputStream = outputTmp;
            mState = STATE_CONNECTED;
        }

        @Override
        public void run() {
            int bytes;
            while (mState == STATE_CONNECTED) {
                try {
                    bytes = mInputStream.read(mBufferSize);
                    if (mBTSocketCallBack != null)
                        mBTSocketCallBack.onRead(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                    if (null != mBTSocketCallBack)
                        mBTSocketCallBack.onError(ERROR_CON_NOT_READ);
                    mState = STATE_NONE;
                    Log.e(TAG, "run: ", e);
                }
            }
        }

        public void write(byte cmd) {
            if (mState == STATE_CONNECTED) {
                try {
                    mOutputStream.write(cmd);
                } catch (IOException e) {
                    e.printStackTrace();
                    if (null != mBTSocketCallBack)
                        mBTSocketCallBack.onError(ERROR_CAN_NOT_WRITE);
                    mState = STATE_NONE;
                    Log.e(TAG, "write: ", e);
                }
            }
        }

        public void cancel() {
            try {
                mOutputStream.close();
                mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                mState = STATE_NONE;
            }
        }
    }

    public SocketManager buffer(byte[] bufferSize) {
        this.mBufferSize = bufferSize;
        return this;
    }

    public SocketManager uuid(UUID uuid) {
        this.mUUID = uuid;
        return this;
    }

    public SocketManager setSocketCallBack(BTSocketCallBack callback) {
        this.mBTSocketCallBack = callback;
        return this;
    }
}
