# BTPrinter
> 封装了通过蓝牙打印小票的功能。	


 - 内部处理蓝牙的开启、设备的搜索、连接等蓝牙相关的操作，无须额外编写蓝牙相关的操作。	
- 目前仅支持打印图片，只需设置要打印的`Bitmap`即可。	

 #### 使用非常简单，只需要3步：	
1. 添加依赖	
  ```gradle	
  compile 'com.chinawutong:bt-printer:1.0.2'	
  ```	
2. 调用	
  ```java	
  BtPrinter.getInstance()	
        .init(MainActivity.this)	
        .setPrintBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.order))	
        //.setCancelable(true)   // 是否可通过点击加载框外中断打印，默认为true	
        //.setStateShowing(true) // 是否Toast显示打印进度，默认为true	
        //.setTailBlankLines(3)  // 打印最后的空白行数，用于将已打印的内容走出来，默认为3	
        .print();	
  ```	
3. 让`BtPrinter`处理`onActivityResult`和`onDestroy`回调	
  ```java	
  @Override	
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {	
      BtPrinter.getInstance().onActivityResult(requestCode, resultCode, data);	
      super.onActivityResult(requestCode, resultCode, data);	
  }	
   @Override	
  protected void onDestroy() {	
      BtPrinter.getInstance().onDestroy();	
      super.onDestroy();	
  }	
  ```	


 # 上传bintray操作步骤	
1. bintray上的操作	
  1. 先注册bintray账号	
  2. 新建一个仓库	
2. 本地配置	
  1. 在项目根目录下的`build.gradle`文件中，添加`dependencies`:	
    ```gradle	
    classpath 'com.github.dcendents:android-maven-gradle-plugin:2.0'	
    classpath 'com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.0'	
    ```	
  2. 在库的`build.gradle`文件中，添加`ext`:	
    ```gradle	
    ext {	
        bintrayRepo = 'library'	
        bintrayName = 'bt-printer'	
    	
        publishedGroupId = 'com.chinawutong'	
        libraryName = 'BTPrinter'	
        artifact = 'bt-printer'	
    	
        libraryDescription = '封装蓝牙打印小票功能'	
    	
        siteUrl = 'https://github.com/wenmin92/BTPrinter'	
        gitUrl = 'https://github.com/wenmin92/BTPrinter.git'	
    	
        libraryVersion = '1.0.0'	
    	
        developerId = 'wenmin92'	
        developerName = 'Changzhu Zhao'	
        developerEmail = 'wenmin92@gmail.com'	
    	
        licenseName = 'The Apache Software License, Version 2.0'	
        licenseUrl = 'http://www.apache.org/licenses/LICENSE-2.0.txt'	
        allLicenses = ["Apache-2.0"]	
    }	
    ```
