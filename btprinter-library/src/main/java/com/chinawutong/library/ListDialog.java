package com.chinawutong.library;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wenmin92 on 2018/1/10.
 * 搜索到的蓝牙设备列表
 */
public class ListDialog extends DialogFragment {

    static final String ARG_ADAPTER = "arg_adapter";
    private OnDismissListener mOnDismissListener;
    private OnItemSelectedListener mOnItemSelectedListener;
    private View mProgressBar, mTvSearching;

    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final ListAdapter listAdapter = (ListAdapter) getArguments().getSerializable(ARG_ADAPTER);
        if (listAdapter == null) {
            throw new IllegalStateException("需要传入 ListAdapter");
        }
        View titleView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_title, null);
        ((TextView) titleView.findViewById(R.id.tvTitle)).setText("请选择打印设备");
        mProgressBar = titleView.findViewById(R.id.progressBar);
        mTvSearching = titleView.findViewById(R.id.tvSearching);
        return new AlertDialog.Builder(getActivity())
                .setCustomTitle(titleView)
                .setAdapter(listAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mOnItemSelectedListener.onItemSelected(listAdapter.getItem(which));
                        dismiss();
                    }
                })
                .create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        mOnDismissListener.onDismiss(dialog);
    }

    void setProgressBarVisible(boolean visible) {
        mProgressBar.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        mTvSearching.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    @SuppressWarnings("unused")
    public void setmOnDismissListener(OnDismissListener mOnDismissListener) {
        this.mOnDismissListener = mOnDismissListener;
    }

    @SuppressWarnings("unused")
    public void setmOnItemSelectedListener(OnItemSelectedListener mOnItemSelectedListener) {
        this.mOnItemSelectedListener = mOnItemSelectedListener;
    }

    interface OnDismissListener {
        void onDismiss(DialogInterface dialog);
    }

    interface OnItemSelectedListener {
        void onItemSelected(BluetoothDevice btDevice);
    }

    static class ListAdapter extends BaseAdapter implements Serializable {

        private List<BluetoothDevice> mData;
        private Context mContext;

        ListAdapter(@NonNull Context context) {
            super();
            mContext = context;
            mData = new ArrayList<>();
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public BluetoothDevice getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            TextView textView = convertView.findViewById(android.R.id.text1);
            textView.setText(getItemDesc(position));
            return convertView;
        }

        private String getItemDesc(int position) {
            String name = getItem(position).getName();
            String address = getItem(position).getAddress();
            return String.format("%s (%s)", name == null ? "" : name, address);
        }

        void add(BluetoothDevice btDevice) {
            mData.add(btDevice);
            notifyDataSetChanged();
        }

        void clear() {
            mData.clear();
        }
    }

}
