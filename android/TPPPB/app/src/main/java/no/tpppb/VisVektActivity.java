package no.tpppb;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class VisVektActivity extends AppCompatActivity {

    private TextView mTextViewKg;
    private TextView mTextViewInfo;
    private TextView mTextViewStatus;
    private Button mButtonEnd;

    int runner = 0;

    BluetoothDevice mMyBluetoothDevice = null;
    private MyBluetoothClass mMyBluetoothClass = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_vis_vekt);
        mTextViewKg = findViewById(R.id.textViewKg);
        mTextViewInfo = findViewById(R.id.textView2);
        mTextViewStatus = findViewById(R.id.textViewStatus);
        mButtonEnd = findViewById(R.id.buttonEnd);

        mButtonEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        createMyBluetoothClass();
        if (mMyBluetoothClass != null)
            mMyBluetoothClass.start();
        else {
            finish();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mMyBluetoothClass != null) {
            mMyBluetoothClass.close();
            mMyBluetoothClass = null;
        }
    }

    private void createMyBluetoothClass() {

        BluetoothAdapter mBluetoothAdapter;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null)
        {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
        }
        if (!mBluetoothAdapter.isEnabled())
        {
            Toast.makeText(this, "Bluetooth is not enabled", Toast.LENGTH_LONG).show();
            finish();
        }

        String name = "Tommys dings nr 1";
        String mac = "98:D3:51:FD:A2:79";

        Set<BluetoothDevice> list = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice item:list) {
            String n = item.getName();
            String m = item.getAddress();
            if (n.compareTo(name) == 0 && m.compareTo(mac) == 0) {
                mMyBluetoothDevice = item;
                break;
            }
        }

        if (mMyBluetoothDevice == null) {
            BluetoothAdapter.getDefaultAdapter().startDiscovery();
            Toast.makeText(this, "TPPPB er ikke 'bounded' med 'Tommys dings nr 1' ... :-|", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(VisVektActivity.this, MainActivity.class);
            startActivity(intent);
        } else {
            mMyBluetoothClass = new MyBluetoothClass(mMyBluetoothDevice);
        }

    }

    //----------------------------------------------------------------------------------------------
    // Handler stuff start
    //

    private static final int STATE_CONNECTING         = 1;
    private static final int STATE_CONNECTED          = 2;
    private static final int STATE_CONNECTION_FAILED  = 3;
    private static final int STATE_MESSAGE_RECEIVED   = 4;


    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what)
            {
                case STATE_CONNECTING:
                    mTextViewStatus.setText("Connecting ...");
                    break;
                case STATE_CONNECTED:
                    mTextViewStatus.setText("Connected");
                    break;
                case STATE_CONNECTION_FAILED:
                    mTextViewStatus.setText("Connecting Failed");
                    break;
                case STATE_MESSAGE_RECEIVED: {
                    updateKgDisplay((String)msg.obj);
                    break;
                }

                    default:
                    //nop
            }
        }

    };

    //
    // Handler stuff end
    //----------------------------------------------------------------------------------------------


    private void updateKgDisplay(String msg) {
        mTextViewKg.setText(msg);

        String r = "---";
        if (runner == 0) {
            r = "---";
        }else if(runner == 1) {
            r = " \\";
        }else if(runner == 2) {
            r = " |";
        }else if(runner == 3) {
            r = " /";
            runner = -1;
        }
        mTextViewInfo.setText(r);
        runner++;
    }

    /**
     * My Bluetooth SendReceive class.
     *
     */
    private class SendReceiveClass extends Thread
    {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private String mData;

        public SendReceiveClass(BluetoothSocket _socket)
        {
            socket = _socket;
            InputStream tmpIn = null;
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream = tmpIn;

            mData = "";
        }

        @Override
        public void run() {
            super.run();

            byte[] buffer = new byte[1024];
            int bytes;

            while(true)
            {
                try {
                    bytes = inputStream.read(buffer);
                    makeMessage(buffer,bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        private void makeMessage(byte[] data, int length) {
            if (length != -1)
            {
                mData += new String(data,0,length);
                if (mData.contains("[") && mData.contains("]")) {
                    int a = mData.indexOf("[");
                    int b = mData.indexOf("]");
                    if (a<b) {
                        String str = mData.substring(a+1, b);
                        mHandler.obtainMessage(STATE_MESSAGE_RECEIVED, -1, -1, str).sendToTarget();
                    }
                    mData = "";
                }
            } else {
                mData = "";
            }
        }


        public void close() {
            try {
                inputStream.close();
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * My Bluetooth client class.
     *
     */
    private class MyBluetoothClass extends Thread {

        private BluetoothSocket socket = null;
        private BluetoothDevice device = null;
        private SendReceiveClass mSendReceiveClass = null;

        public MyBluetoothClass(BluetoothDevice _device) {
            device = _device;
            try {
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                socket = device.createRfcommSocketToServiceRecord(uuid);
                mSendReceiveClass = new SendReceiveClass(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            try {

                Message msgA = Message.obtain();
                msgA.what = STATE_CONNECTING;
                mHandler.sendMessage(msgA);

                socket.connect();
                mSendReceiveClass.start();

                Message msgB = Message.obtain();
                msgB.what = STATE_CONNECTED;
                mHandler.sendMessage(msgB);

            } catch (IOException e) {
                e.printStackTrace();

                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                mHandler.sendMessage(message);

            }
        }

        public void close() {

            if (mSendReceiveClass != null)
                mSendReceiveClass.close();

            if (socket != null)
            {
                try {
                    socket.close();
                    socket = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Thread.currentThread().interrupt();
        }

    }

}
