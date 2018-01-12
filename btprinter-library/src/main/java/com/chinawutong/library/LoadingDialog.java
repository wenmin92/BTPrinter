package com.chinawutong.library;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * Created by wenmin92 on 2017/11/27.
 * 加载框
 */
@SuppressWarnings("unused")
public class LoadingDialog extends Dialog {

    @SuppressWarnings("unused")
    public LoadingDialog(Context context) {
        super(context);
    }

    @SuppressWarnings("WeakerAccess")
    public LoadingDialog(Context context, int themeResId) {
        super(context, themeResId);
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static class Builder {

        private Context context;
        private String message;
        private boolean isShowMessage = true;
        private boolean isCancelable = false;
        private boolean isCancelOutside = false;


        public Builder(Context context) {
            this.context = context;
        }

        /**
         * 设置提示信息
         */
        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        /**
         * 设置是否显示提示信息
         */
        public Builder setShowMessage(boolean isShowMessage) {
            this.isShowMessage = isShowMessage;
            return this;
        }

        /**
         * 设置是否可以按返回键取消
         */
        public Builder setCancelable(boolean isCancelable) {
            this.isCancelable = isCancelable;
            return this;
        }

        /**
         * 设置是否可以取消
         */
        public Builder setCancelOutside(boolean isCancelOutside) {
            this.isCancelOutside = isCancelOutside;
            return this;
        }

        @SuppressLint("InflateParams")
        public LoadingDialog create() {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.dialog_loading, null);
            LoadingDialog loadingDailog = new LoadingDialog(context, R.style.MyDialogStyle);
            TextView msgText = view.findViewById(R.id.tvTip);
            if (isShowMessage) {
                msgText.setText(message);
            } else {
                msgText.setVisibility(View.GONE);
            }
            loadingDailog.setContentView(view);
            loadingDailog.setCancelable(isCancelable);
            loadingDailog.setCanceledOnTouchOutside(isCancelOutside);
            return loadingDailog;

        }


    }
}
