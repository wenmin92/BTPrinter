package com.chinawutong.btprinter;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.chinawutong.library.BtPrinter;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_print).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BtPrinter.getInstance()
                        .init(MainActivity.this)
                        .setPrintBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.order))
                        .setCancelable(true) // 是否可通过点击加载框外中断打印，默认为true
                        .setStateShowing(true) // 是否Toast显示打印进度，默认为true
                        .setTailBlankLines(3) // 打印最后的空白行数，用于将已打印的内容走出来，默认为3
                        .print();
            }
        });

        findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BtPrinter.getInstance().cancelPrint();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        BtPrinter.getInstance().onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BtPrinter.getInstance().onDestroy();
    }
}
