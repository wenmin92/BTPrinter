# BTPrinter
> 封装了通过蓝牙打印小票的功能

#### 使用非常简单，只需要3步：
1. 添加依赖
  ```gradle
  compile 'com.chinawutong:bt-printer:1.0.0'
  ```
2. 调用
  ```java
  BtPrinter.getInstance()
        .init(MainActivity.this)
        .setPrintBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.order))
        .setCancelable(true) // 是否可通过点击加载框外中断打印，默认为true
        .setStateShowing(true) // 是否Toast显示打印进度，默认为true
        .setTailBlankLines(3) // 打印最后的空白行数，用于将已打印的内容走出来，默认为3
        .print();
  ```
3. 让`BtPrinter`处理`onActivityResult`和`onDestroy`回调
  ```java
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
  ```
