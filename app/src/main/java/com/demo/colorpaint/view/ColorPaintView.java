package com.demo.colorpaint.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.demo.colorpaint.ColorPaintApplication;
import com.demo.colorpaint.bean.RegionInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * 填色View
 *
 * @date: 2020-01-27
 * @author: 山千
 */
public class ColorPaintView extends VectorImageView implements PhotoViewAttacher.OnPhotoTapListener, PhotoViewAttacher.OnScaleChangeListener, VectorImageView.OnImageCallbackListener, PhotoViewAttacher.OnMatrixChangedListener {

    private PhotoViewAttacher photoViewAttacher;
    private ColorPaintView colorPaintView;

    private Context mContext;

    private Paint mPaint;
    //当前要填的颜色
    private String mCurrentColor = "#FFFFFF";

    private int prevColor = -1;

    private Matrix curMatrix;
    private SvgLoadListener mLoadListener;
    //当前色块
    private RegionInfo mCurrentRegion;
    //色块填色完成的回调
    private RegionFillListener mRegionFillListener;
    //缩放回调
    private PhotoViewAttacher.OnScaleChangeListener mScaleChangeListener;

    public ColorPaintView(Context context) {
        super(context);
        this.mContext = context;
        initThis();
    }

    public ColorPaintView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        initThis();
    }

    public ColorPaintView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        initThis();
    }

    public void setLoadListener(SvgLoadListener listener) {
        this.mLoadListener = listener;
    }

    public void setRegionFillListener(RegionFillListener listener) {
        this.mRegionFillListener = listener;
    }

    @Override
    public void onSvgStart(@NonNull Canvas canvas, @Nullable RectF bounds) {
        super.onSvgStart(canvas, bounds);
        if (mLoadListener != null) {
            mLoadListener.onLoadStart();
        }
    }

    public void setOnScaleChangeListener(PhotoViewAttacher.OnScaleChangeListener listener) {
        mScaleChangeListener = listener;
    }

    @Override
    public void onSvgEnd(@NonNull Canvas canvas, @Nullable RectF bounds) {
        super.onSvgEnd(canvas, bounds);
    }

    @Override
    public void onSvgDrawableReady() {
        super.onSvgDrawableReady();
        if (mLoadListener != null) {
            mLoadListener.onLoadEnd();
        }
    }

    public void cleanup() {
        photoViewAttacher.cleanup();
    }

    @Override
    public void loadAsset(String string) {
        super.loadAsset(string);

        curMatrix = new Matrix();
        photoViewAttacher = new PhotoViewAttacher(colorPaintView);
        photoViewAttacher.getDisplayMatrix(curMatrix);
        photoViewAttacher.setMaximumScale(36);
        photoViewAttacher.setMediumScale(6);
        photoViewAttacher.setOnPhotoTapListener(colorPaintView);
        photoViewAttacher.setOnMatrixChangeListener(colorPaintView);
        photoViewAttacher.setOnScaleChangeListener(colorPaintView);

        mPaint = new Paint();
        mPaint.setStrokeWidth(1);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
    }

    @Override
    public void initThis() {
        colorPaintView = this;
    }

    public List<RegionInfo> getRegionColors() {
        return super.getRegionColors();
    }

    @Override
    public void onPhotoTap(View view, float x, float y) {
        mCurrentRegion = getRegion(x, y);
        mCurrentColor = getOnImageCommandsListener().getCurrentColor();
        //非色块对应颜色，不进行填色
        if (mCurrentRegion == null || TextUtils.isEmpty(mCurrentColor) || !mCurrentColor.equals(mCurrentRegion.color)) {
            return;
        }
        //已经填过色的色块，不进行操作
        boolean hasColored = isRegionColored(mCurrentRegion.imageId, mCurrentRegion.number);
        if (hasColored) {
            return;
        }
        //填色块
        fillRegion(mCurrentRegion);
        colorPaintView.invalidate();
        if (mRegionFillListener != null) {
            mRegionFillListener.onRegionFilled(mCurrentRegion);
        }
    }

    public void autoFill(int position) {
        RegionInfo regionInfo = getNextFillRegion(position);
        if (regionInfo == null) {
            return;
        }
        mCurrentRegion=regionInfo;
        fillRegion(mCurrentRegion);
        colorPaintView.invalidate();
        if (mRegionFillListener != null) {
            mRegionFillListener.onRegionFilled(mCurrentRegion);
        }
    }

    @Override
    public void onOutsidePhotoTap() {
    }

    /**
     * 分享图片
     *
     * @return 图片url
     */
    public Uri doShare() {
        Uri uri = null;
        try {

            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File bmpFile = File.createTempFile("BigPhil", ".png", path);
            FileOutputStream out = new FileOutputStream(bmpFile);

            Bitmap bmp = getShareBitmap(colorPaintView.getDrawable());

            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);

            out.close();
            uri = Uri.fromFile(bmpFile);
            Toast.makeText(mContext, String.format("Extracted into: %s", bmpFile.getAbsolutePath()), Toast.LENGTH_LONG).show();

        } catch (Throwable t) {
            t.printStackTrace();
            Toast.makeText(mContext, "Error occured while extracting bitmap", Toast.LENGTH_SHORT).show();
        }
        return uri;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPaint.setColor(Color.parseColor(mCurrentColor));
        canvas.drawRect(
                0, 0,
                colorPaintView.getMeasuredWidth() - 1,
                colorPaintView.getMeasuredHeight() - 1,
                mPaint
        );
    }

    @Override
    public void imageCallback() {
        photoViewAttacher.update();
        mCurrentColor = getOnImageCommandsListener().getCurrentColor();
    }

    @Override
    public void onMatrixChanged(RectF rect) {
        photoViewAttacher.getDisplayMatrix(curMatrix);
    }

    @Override
    public void onScaleChange(float scaleFactor, float focusX, float focusY) {
        float scale = photoViewAttacher.getScale();
        colorPaintView.onScaleChanged(scale);
        //注意这里用的是scale，不是scaleFactor
        mScaleChangeListener.onScaleChange(scale, focusX, focusY);
    }

    /**
     * 突出显示一个颜色编号中下一个要填色的区块
     *
     * @param position 当前选中的颜色编号
     */
    public void showNextFillRegion(int position) {
        RegionInfo info = getNextFillRegion(position);
        if (info == null || info.path == null) {
            return;
        }
        //获取屏幕中心点位置
        WindowManager wm = (WindowManager) ColorPaintApplication.getAppContext()
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        //获取区块的中心位置
        Path path = info.path;
        RectF rectF = new RectF();
        path.computeBounds(rectF, true);
        float centerX = rectF.centerX() / actW * dm.widthPixels;
        float centerY = rectF.centerY() / actH * dm.heightPixels;
        photoViewAttacher.onScale(5.0f, centerX, centerY);
        colorPaintView.onScaleChanged(5.0f);
    }

    /**
     * SVG图片加载监听
     */
    public interface SvgLoadListener {
        //开始加载
        void onLoadStart();

        //加载完成
        void onLoadEnd();
    }

    /**
     * 色块填色回调
     */
    public interface RegionFillListener {
        void onRegionFilled(RegionInfo regionInfo);
    }
}