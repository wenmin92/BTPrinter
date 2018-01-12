package com.chinawutong.library;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import java.io.IOException;
import java.util.ArrayList;

import am.util.printer.PrinterUtils;
import am.util.printer.PrinterWriter80mm;

/**
 * Created by wenmin92 on 2018/1/11.
 * 自定义PrinterWriter，解决Bitmap缩放不正确问题
 */
public class PrinterWriter80mmCustom extends PrinterWriter80mm {

    PrinterWriter80mmCustom(int parting, int width) throws IOException {
        super(parting, width);
    }


    /**
     * 获取图片数据流
     *
     * @param image 图片
     * @return 数据流
     */
    public ArrayList<byte[]> getImageByte(Bitmap image) {
        int maxWidth = getDrawableMaxWidth();
        Bitmap scalingImage = scalingBitmap(image, maxWidth);
        if (scalingImage == null)
            return null;
        ArrayList<byte[]> data = PrinterUtils.decodeBitmapToDataList(scalingImage, getHeightParting());
        scalingImage.recycle();
        return data;
    }

    /**
     * 缩放图片
     *
     * @param image    图片
     * @param maxWidth 最大宽
     * @return 缩放后的图片
     */
    private Bitmap scalingBitmap(Bitmap image, int maxWidth) {
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0)
            return null;
        try {
            final int width = image.getWidth();
            final int height = image.getHeight();
            // 精确缩放
            if (maxWidth <= 0 || width <= maxWidth) {
                return image;
            }
            float scale = maxWidth / (float) width;
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);
            Bitmap resizeImage = Bitmap.createBitmap(image, 0, 0, width, height, matrix, true);
            image.recycle();
            return resizeImage;
        } catch (OutOfMemoryError e) {
            return null;
        }
    }
}
