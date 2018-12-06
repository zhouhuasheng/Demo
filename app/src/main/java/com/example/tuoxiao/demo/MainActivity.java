package com.example.tuoxiao.demo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private  BluetoothAdapter bTAdatper;
//    private BlueToothDeviceAdapter adapter;
    private TextView text_state;
    private TextView text_msg;
    private ListView listView;
    private static final UUID BT_UUID = UUID.fromString("02001101-0001-1000-8080-00805F9BA9BA");

    private ConnectThread connectThread;
    private final int BUFFER_SIZE = 1024;
    private static final String NAME = "BT_DEMO";
    private ListenerThread listenerThread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bTAdatper = BluetoothAdapter.getDefaultAdapter();
        initview();
        initReceiver();
        listenerThread = new ListenerThread();
        listenerThread.start();

    }

    private void initReceiver() {
        //注册广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//        registerReceiver(mReceiver, filter);
    }

    private void initview() {
        findViewById(R.id.bt_opeble).setOnClickListener(this);
        text_state = (TextView) findViewById(R.id.text_state);
        text_msg = (TextView) findViewById(R.id.text_msg);

        listView = (ListView) findViewById(R.id.listView);

//        adapter= new BlueToothDeviceAdapter(getApplicationContext(), R.layout.demo);
//        listView.setAdapter(adapter);
//        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                if (bTAdatper.isDiscovering()){
//                    bTAdatper.cancelDiscovery();
//                }
//                BluetoothDevice device=(BluetoothDevice) adapter.getItem(i);
//                connectDevice(device);
//            }
//        });
    }

    private void connectDevice(BluetoothDevice device) {
        text_state.setText("连接中。。。");
        try {
            BluetoothSocket socket=device.createRfcommSocketToServiceRecord(BT_UUID);
            connectThread=new ConnectThread(socket,true);
            connectThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.bt_opeble:
                openBlueTooth();
                break;
            case R.id.btn_search:
                searchDevices();
                break;
        }
    }

    /**
     * 开启蓝牙
     */

    private void openBlueTooth() {
        if (bTAdatper==null){
            Toast.makeText(this, "当前设备不支持蓝牙功能", Toast.LENGTH_SHORT).show();
        }if(!bTAdatper.isEnabled()){
           Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(i);
//            bTAdatper.enable();
        }
        //开启被其它蓝牙设备发现的功能
        if (bTAdatper!=null){
            if (!bTAdatper.isEnabled()){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        bTAdatper.enable();
                    }
                }).start();
            }else {
                if (!bTAdatper.isDiscovering()){
                    bTAdatper.startDiscovery();
                }else {
                    Toast.makeText(this, "正在扫描", Toast.LENGTH_SHORT).show();
                }
            }
            Toast.makeText(this, "当前设备没有找到蓝牙功能", Toast.LENGTH_SHORT).show();
        }
//        if (bTAdatper.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
//            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//            //设置为一直开启
//            i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, true);
//            startActivity(i)  ;
//        }
    }
//    private final BroadcastReceiver mReceiver=new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action=intent.getAction();
//            if (BluetoothDevice.ACTION_FOUND.equals(action)){
//                    BluetoothDevice device=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                    if (device.getBondState()!=BluetoothDevice.BOND_BONDED){
//                        adapter.add(device);
//                        adapter.notifyDataSetChanged();
//                    }
//            }else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
//                Toast.makeText(context, "开始搜索设备", Toast.LENGTH_SHORT).show();
//            }else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
//                Toast.makeText(context, "搜索完毕", Toast.LENGTH_SHORT).show();
//            }
//        }
//    };
    /**
     * 搜索蓝牙设备
     */
    private void searchDevices() {
        if (bTAdatper.isDiscovering()) {
            bTAdatper.cancelDiscovery();
        }
        getBoundedDevices();
        bTAdatper.startDiscovery();
    }

    /**
     * 获取已经配对过的设备
     */
    private void getBoundedDevices() {
        //获取已经配对过的设备
        Set<BluetoothDevice> pairedDevices = bTAdatper.getBondedDevices();
        //将其添加到设备列表中
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
//                adapter.add(device);
            }
        }
    }

    private class ConnectThread extends Thread {

        private BluetoothSocket socket;
        private boolean activeConnect;
        InputStream inputStream;
        OutputStream outputStream;

        private ConnectThread(BluetoothSocket socket, boolean connect){
            this.socket=socket;
            this.activeConnect=connect;
        }

        @Override
        public void run() {
            try {
                if (activeConnect){
                    socket.connect();
                }
                text_state.post(new Runnable() {
                    @Override
                    public void run() {
                        text_state.setText("连接成功");
                    }
                });
                inputStream=socket.getInputStream();
                outputStream=socket.getOutputStream();
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytes;
                while (true)    {
                    //读取数据
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        final byte[] data = new byte[bytes];
                        System.arraycopy(buffer, 0, data, 0, bytes);
                        text_msg.post(new Runnable() {
                            @Override
                            public void run() {
                                text_msg.setText("获取到数据:"+new String(data));
                            }
                        });
                    }
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private class ListenerThread extends  Thread{
        private BluetoothServerSocket serverSocket;
        private BluetoothSocket socket;

        @Override
        public void run() {
            try {
                    serverSocket=bTAdatper.listenUsingRfcommWithServiceRecord(NAME,BT_UUID);
                    while (true){
                        socket=serverSocket.accept();
                        text_state.post(new Runnable() {
                            @Override
                            public void run() {
                                text_state.setText("连接中");
                            }
                        });
                        connectThread = new ConnectThread(socket, false);
                        connectThread.start();
                    }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}
