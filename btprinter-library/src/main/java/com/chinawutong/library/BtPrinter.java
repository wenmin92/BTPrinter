package com.chinawutong.library;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import am.util.printer.PrintDataMaker;
import am.util.printer.PrintExecutor;
import am.util.printer.PrintSocketHolder;
import am.util.printer.PrinterWriter;
import am.util.printer.PrinterWriter80mm;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;

/**
 * Created by wenmin92 on 2018/1/11.
 * 蓝牙打印机
 * NOTE：目前打印库有一些Bug，比如：
 * 1. 打印图片时，几个打印图片的方法在缩放处理上不一致
 * 2. 针对OOM做了屏蔽处理，这种方式肯定不行
 * 3. 无法中断打印过程，强行关闭流会导致空指针
 * 4. 使用图片打印时，对图片的处理时间过长
 */
@SuppressWarnings("unused")
public class BtPrinter implements ListDialog.OnDismissListener, ListDialog.OnItemSelectedListener,
        PrintSocketHolder.OnStateChangedListener, PrintExecutor.OnPrintResultListener {

    private static final int REQ_ENABLE_BT = 10001;
    private static final String SP_FILE_CUR_PRINTER = "cur_printer";
    private static final String SP_KEY_ADDRESS = "address";
    private BluetoothAdapter mBtAdapter;
    private ListDialog.ListAdapter mListAdapter;
    private ListDialog mListDialog;
    private BluetoothDevice mDevice;
    private BluetoothDevice mUsedDevice;
    private PrintExecutor executor;
    private Bitmap mPrintBitmap;
    private WeakReference<Activity> mActivityRef;
    private Toast mToast;
    private boolean mStateShowing = true;
    private int mTailBlankLines = 3;
    private LoadingDialog mLoadingDialog;
    private boolean isCanceled;
    private boolean cancelable = true;
    private int mPrintState;

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

    private BtPrinter() {}

    public static BtPrinter getInstance() {
        return BtPrinterHolder.INSTANCE;
    }

    private static class BtPrinterHolder {
        private static final BtPrinter INSTANCE = new BtPrinter();
    }

    /**
     * 初始化
     *
     * @param activity 调用的Activity实例
     */
    @SuppressWarnings("unused")
    public BtPrinter init(Activity activity) {
        mActivityRef = new WeakReference<>(activity);
        return this;
    }

    /**
     * 设置要打印的图片
     *
     * @param bitmap 要打印的图片
     */
    public BtPrinter setPrintBitmap(Bitmap bitmap) {
        mPrintBitmap = bitmap;
        return this;
    }

    /**
     * 打印最后的空白行数，用于将已打印的内容走出来，默认为3
     */
    @SuppressWarnings({"unused", "SameParameterValue"})
    public BtPrinter setTailBlankLines(int blankLines) {
        mTailBlankLines = blankLines;
        return this;
    }

    /**
     * 打印
     */
    @SuppressWarnings("unused")
    public void print() {
        isCanceled = false;
        showTip(R.string.state_received_command);

        // Get the BluetoothAdapter.
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            showTip(R.string.failed_not_support_bluetooth);
            return;
        }

        // Enable Bluetooth.
        if (!mBtAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mActivityRef.get().startActivityForResult(enableBtIntent, REQ_ENABLE_BT);
        } else {
            queryDevice();
        }

        mListAdapter = new ListDialog.ListAdapter(mActivityRef.get());
    }

    /**
     * 代理onActivityResult回调，用于处理开启蓝牙，用户必须使用此方法。
     */
    @SuppressWarnings("unused")
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQ_ENABLE_BT:
                if (resultCode != RESULT_OK) {
                    showTip(R.string.failed_could_not_open_bluetooth);
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
            showTip(R.string.state_init_print_sequence);
            doPrint();
            return;
        }
        // 从本地记录中取出保存的蓝牙设备，直接使用
        SharedPreferences preferences = mActivityRef.get().getSharedPreferences(SP_FILE_CUR_PRINTER, MODE_PRIVATE);
        String savedAddress = preferences.getString(SP_KEY_ADDRESS, "");
        if (!TextUtils.isEmpty(savedAddress)) {
            Set<BluetoothDevice> bondedDevices = mBtAdapter.getBondedDevices();
            for (BluetoothDevice device : bondedDevices) {
                if (savedAddress.equals(device.getAddress())) {
                    mDevice = device;
                    showTip(R.string.state_init_print_sequence);
                    doPrint();
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
        mActivityRef.get().registerReceiver(mReceiver, filter);
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
        mListDialog.show(mActivityRef.get().getFragmentManager(), "bt_list");
    }

    /**
     * 代理onDestroy回调，用户取消发现设备广播监听，用户必须使用此方法。
     */
    @SuppressWarnings("unused")
    public void onDestroy() {
        mActivityRef.get().unregisterReceiver(mReceiver);
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) mLoadingDialog.dismiss();
    }

    /**
     * 发现设备对话框消失，用户不应该调用此方法。
     */
    @Override
    public void onDismiss(DialogInterface dialog) {
        mBtAdapter.cancelDiscovery();
    }

    /**
     * 用户从发现设备列表选择了一个蓝牙设备，用户不应该调用此方法。
     */
    @Override
    public void onItemSelected(BluetoothDevice btDevice) {
        mDevice = btDevice;
        doPrint();
    }

    /**
     * 开始打印
     */
    private void doPrint() {
        if (mDevice == null)
            return;
        if (executor == null) {
            executor = new PrintExecutor(mDevice, PrinterWriter80mm.TYPE_80);
            executor.setOnStateChangedListener(this);
            executor.setOnPrintResultListener(this);
        }
        executor.doPrinterRequestAsync(new PrintDataMaker() {
            @Override
            public List<byte[]> getPrintData(int type) {
                ArrayList<byte[]> data = new ArrayList<>();
                try {
                    PrinterWriter printer = new PrinterWriter80mmCustom(10, 554);
                    printer.setAlignCenter();
                    data.add(printer.getDataAndReset());

                    ArrayList<byte[]> image1 = printer.getImageByte(mPrintBitmap);
                    if (isCanceled) return null;

                    data.addAll(image1);

                    for (int i = 0; i < mTailBlankLines; i++) {
                        printer.printLineFeed();
                    }
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
     * 是否Toast显示打印进度，默认为true
     */
    @SuppressWarnings({"unused", "SameParameterValue"})
    public BtPrinter setStateShowing(boolean show) {
        mStateShowing = show;
        return this;
    }

    /**
     * 打印状态回调
     */
    @Override
    public void onStateChanged(int state) {
        Log.d("tag", "onStateChanged() called with: state = [" + state + "]");
        mPrintState = state;
        if (!mStateShowing) return;
        switch (state) {
            case PrintSocketHolder.STATE_0:
                showLoading();
                if (!isCanceled) showTip(R.string.printer_state_message_1);
                break;
            case PrintSocketHolder.STATE_1:
                if (!isCanceled) showTip(R.string.printer_state_message_2);
                break;
            case PrintSocketHolder.STATE_2:
                if (!isCanceled) showTip(R.string.printer_state_message_3);
                break;
            case PrintSocketHolder.STATE_3:
                if (isCanceled && executor != null) executor.closeSocket();
                else showTip(R.string.printer_state_message_4);
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
        if (executor != null) executor.closeSocket();
        dismissLoading();
        if (isCanceled) {
            return;
        }
        switch (errorCode) {
            case PrintSocketHolder.ERROR_0:
                showTip(R.string.printer_result_message_1);
                saveDevice();
                break;
            case PrintSocketHolder.ERROR_1:
                showTip(R.string.printer_result_message_2);
                break;
            case PrintSocketHolder.ERROR_2:
                showTip(mActivityRef.get().getString(R.string.printer_result_message_3, mDevice.getName()));
                break;
            case PrintSocketHolder.ERROR_3:
                showTip(R.string.printer_result_message_4);
                break;
            case PrintSocketHolder.ERROR_4:
                showTip(R.string.printer_result_message_5);
                break;
            case PrintSocketHolder.ERROR_5:
                showTip(R.string.printer_result_message_6);
                break;
            case PrintSocketHolder.ERROR_6:
                showTip(R.string.printer_result_message_7);
                break;
            case PrintSocketHolder.ERROR_100:
                showTip(R.string.printer_result_message_8);
                break;
        }
    }

    /**
     * 保存关联设备
     */
    private void saveDevice() {
        mUsedDevice = mDevice;
        SharedPreferences preferences = mActivityRef.get().getSharedPreferences(SP_FILE_CUR_PRINTER, MODE_PRIVATE);
        if (!preferences.getString(SP_KEY_ADDRESS, "").equals(mDevice.getAddress())) {
            preferences.edit().putString(SP_KEY_ADDRESS, mDevice.getAddress()).apply();
        }
    }

    /**
     * 显示Loading
     */
    private void showLoading() {
        if (mLoadingDialog == null) {
            mLoadingDialog = new LoadingDialog.Builder(mActivityRef.get())
                    .setMessage("正在执行打印任务...")
                    .setCancelable(cancelable)
                    .setCancelOutside(cancelable)
                    .create();
            mLoadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    cancelPrint();
                }
            });
        }
        mLoadingDialog.show();
    }

    /**
     * 关闭Loading
     */
    private void dismissLoading() {
        if (mLoadingDialog == null) {
            return;
        }
        if (mLoadingDialog.isShowing()) mLoadingDialog.dismiss();
    }

    /**
     * Toast
     */
    private void showTip(CharSequence message) {
        if (mToast == null) mToast = Toast.makeText(mActivityRef.get(), "", Toast.LENGTH_LONG);
        mToast.setText(message);
        mToast.show();
    }

    /**
     * Toast
     */
    private void showTip(int messageRes) {
        showTip(mActivityRef.get().getString(messageRes));
    }

    /**
     * 取消打印
     */
    @SuppressWarnings("unused")
    public void cancelPrint() {
        isCanceled = true;
        showTip(R.string.canceled);
        if (mPrintState == PrintSocketHolder.STATE_3) executor.closeSocket();
    }

    /**
     * 是否可通过点击加载框外中断打印，默认为true
     */
    @SuppressWarnings({"unused", "SameParameterValue"})
    public BtPrinter setCancelable(boolean cancelable) {
        this.cancelable = cancelable;
        return this;
    }
}
