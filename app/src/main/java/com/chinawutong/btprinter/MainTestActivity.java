package com.chinawutong.btprinter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.chinawutong.library.core.PrintDataMaker;
import com.chinawutong.library.core.PrintExecutor;
import com.chinawutong.library.core.PrintSocketHolder;
import com.chinawutong.library.core.PrinterWriter;
import com.chinawutong.library.core.PrinterWriter80mm;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainTestActivity extends AppCompatActivity implements ListDialog.OnDismissListener, ListDialog.OnItemSelectedListener, PrintSocketHolder.OnStateChangedListener, PrintExecutor.OnPrintResultListener {

    private static final int REQ_ENABLE_BT = 10001;
    public static final String SP_FILE_CUR_PRINTER = "cur_printer";
    public static final String SP_KEY_ADDRESS = "address";
    private BluetoothAdapter mBtAdapter;
    private ListDialog.ListAdapter mListAdapter;
    private ListDialog mListDialog;
    private TextView tvStatus;
    private BluetoothDevice mDevice;
    private BluetoothDevice mUsedDevice;
    private PrintExecutor executor;

    // Create a BroadcastReceiver for ACTION_FOUND.
    // 用于开启“发现设备”后，通知状态
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case BluetoothDevice.ACTION_FOUND:
                    // Discovery has found a device.
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.d("tag", "ACTION_FOUND, BluetoothDevice: " + device.getAddress());
                    mListAdapter.add(device);
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Log.d("tag", "ACTION_DISCOVERY_FINISHED, isDiscovering: " + mBtAdapter.isDiscovering());
                    mListDialog.setProgressBarVisible(false);
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    Log.d("tag", "ACTION_DISCOVERY_STARTED, isDiscovering: " + mBtAdapter.isDiscovering());
                    mListDialog.setProgressBarVisible(true);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_print).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the BluetoothAdapter.
                mBtAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBtAdapter == null) {
                    Toast.makeText(MainTestActivity.this, "该设备不支持蓝牙，无法使用蓝牙打印功能", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Enable Bluetooth.
                if (!mBtAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQ_ENABLE_BT);
                } else {
                    queryDevice();
                }
            }
        });

        mListAdapter = new ListDialog.ListAdapter(MainTestActivity.this);
        tvStatus = findViewById(R.id.tv_status);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_ENABLE_BT:
                if (resultCode != RESULT_OK) {
                    Toast.makeText(MainTestActivity.this, "未能成功打开蓝牙，无法使用蓝牙打印功能", Toast.LENGTH_SHORT).show();
                    return;
                }
                queryDevice();
        }
    }

    /**
     * 查找已经关联过的设备，如果没有找到，则开启“发现设备”
     */
    private void queryDevice() {
        // 如果当前实例已经关联设备，直接使用
        if (mUsedDevice != null) {
            mDevice = mUsedDevice;
            print();
            return;
        }
        // 从本地记录中取出保存的蓝牙设备，直接使用
        SharedPreferences preferences = getSharedPreferences(SP_FILE_CUR_PRINTER, MODE_PRIVATE);
        String savedAddress = preferences.getString(SP_KEY_ADDRESS, "");
        if (!TextUtils.isEmpty(savedAddress)) {
            Set<BluetoothDevice> bondedDevices = mBtAdapter.getBondedDevices();
            for (BluetoothDevice device : bondedDevices) {
                if (savedAddress.equals(device.getAddress())) {
                    mDevice = device;
                    print();
                    return;
                }
            }
        }
        // 上述都失败，开启“发现设备”
        discoveryDevices();
    }

    /**
     * 发现所有蓝牙设备
     */
    private void discoveryDevices() {
        mListAdapter.clear();
        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
        mBtAdapter.startDiscovery();
        showDevices();
    }

    /**
     * 列表展示所有搜索到的设备
     */
    private void showDevices() {
        mListDialog = new ListDialog();
        Bundle args = new Bundle();
        args.putSerializable(ListDialog.ARG_ADAPTER, mListAdapter);
        mListDialog.setArguments(args);
        mListDialog.show(getFragmentManager(), "bt_list");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mBtAdapter.cancelDiscovery();
    }

    @Override
    public void onItemSelected(BluetoothDevice btDevice) {
        mDevice = btDevice;
        print();
    }

    /**
     * 开始打印
     */
    private void print() {
        if (mDevice == null)
            return;
        if (executor == null) {
            executor = new PrintExecutor(mDevice, PrinterWriter80mm.TYPE_80);
            executor.setOnStateChangedListener(this);
            executor.setOnPrintResultListener(this);
        }
        executor.setDevice(mDevice);
        executor.doPrinterRequestAsync(new PrintDataMaker() {
            @Override
            public List<byte[]> getPrintData(int type) {
                ArrayList<byte[]> data = new ArrayList<>();
                try {
                    PrinterWriter printer;
                    printer = new PrinterWriter80mm(0, 500);
                    printer.setAlignCenter();
                    data.add(printer.getDataAndReset());

                    ArrayList<byte[]> image1 = printer.getImageByte(getResources(), R.drawable.programmer);
                    data.addAll(image1);
                    printer.printLineFeed();
                    printer.printLineFeed();
                    // printer.printLineFeed();
                    // printer.printLineFeed();
                    // printer.printLineFeed();
                    printer.feedPaperCutPartial();

                    data.add(printer.getDataAndClose());
                    return data;
                } catch (Exception e) {
                    return new ArrayList<>();
                }
            }
        });
    }

    /**
     * 打印状态回调
     */
    @Override
    public void onStateChanged(int state) {
        Log.d("tag", "onStateChanged() called with: state = [" + state + "]");
        switch (state) {
            case PrintSocketHolder.STATE_0:
                tvStatus.setText(R.string.printer_test_message_1);
                break;
            case PrintSocketHolder.STATE_1:
                tvStatus.setText(R.string.printer_test_message_2);
                break;
            case PrintSocketHolder.STATE_2:
                tvStatus.setText(R.string.printer_test_message_3);
                break;
            case PrintSocketHolder.STATE_3:
                tvStatus.setText(R.string.printer_test_message_4);
                break;
            case PrintSocketHolder.STATE_4: // 关闭输出流
                break;
        }
    }

    /**
     * 打印结果回调
     */
    @Override
    public void onResult(int errorCode) {
        Log.d("tag", "onResult() called with: errorCode = [" + errorCode + "]");
        switch (errorCode) {
            case PrintSocketHolder.ERROR_0:
                tvStatus.setText(R.string.printer_result_message_1);
                saveDevice();
                break;
            case PrintSocketHolder.ERROR_1:
                tvStatus.setText(R.string.printer_result_message_2);
                break;
            case PrintSocketHolder.ERROR_2:
                tvStatus.setText(getString(R.string.printer_result_message_3, mDevice.getName()));
                break;
            case PrintSocketHolder.ERROR_3:
                tvStatus.setText(R.string.printer_result_message_4);
                break;
            case PrintSocketHolder.ERROR_4:
                tvStatus.setText(R.string.printer_result_message_5);
                break;
            case PrintSocketHolder.ERROR_5:
                tvStatus.setText(R.string.printer_result_message_6);
                break;
            case PrintSocketHolder.ERROR_6:
                tvStatus.setText(R.string.printer_result_message_7);
                break;
            case PrintSocketHolder.ERROR_100:
                tvStatus.setText(R.string.printer_result_message_8);
                break;
        }
        if (executor != null) executor.closeSocket();
    }

    /**
     * 保存关联设备
     */
    private void saveDevice() {
        mUsedDevice = mDevice;
        SharedPreferences preferences = getSharedPreferences(SP_FILE_CUR_PRINTER, MODE_PRIVATE);
        if (!preferences.getString(SP_KEY_ADDRESS, "").equals(mDevice.getAddress())) {
            preferences.edit().putString(SP_KEY_ADDRESS, mDevice.getAddress()).apply();
        }
    }
}
