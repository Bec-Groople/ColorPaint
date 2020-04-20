package com.demo.colorpaint.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.demo.colorpaint.ColorPaintApplication;
import com.demo.colorpaint.R;
import com.demo.colorpaint.bean.ColorInfo;
import com.demo.colorpaint.bean.RegionInfo;
import com.demo.colorpaint.manager.RegionManager;
import com.demo.colorpaint.utils.Md5Util;
import com.pixplicity.sharp.OnSvgElementListener;
import com.pixplicity.sharp.Sharp;
import com.pixplicity.sharp.SharpDrawable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * SVG绘制
 *
 * @author: 山千
 */
public abstract class VectorImageView extends AppCompatImageView implements OnSvgElementListener {

    private PictureDrawable sharpDrawable;

    private VectorImageView vectorImageView;

    private OnImageCommandsListener onImageCommandsListener;
    private OnImageCallbackListener onImageCallbackListener;

    private Bitmap bitmapMap;

    public int actW;
    public int actH;


    private ArrayList<Path> sectorsPaths;
    //所有的色块集合
    private List<RegionInfo> mRegionList;
    //线条集合
    private List<Path> mLineList;
    //已填色完成的色块集合
    private List<RegionInfo> mFinishedList;
    //提示的待预填色块
    private List<RegionInfo> mPreFillList;
    //整理后的色块集合
    private List<ColorInfo> mColorInfoList;
    //色块编号
    private int mCount = 0;
    private RegionManager mRegionManager;
    //图片路径
    private String mImagePath;
    //生成的图片ID
    private String mImageId;
    //是否显示数字
    private boolean mIsShowNumber;
    //待填色区域的显示图案资源ID
    private int mPrefillDrawableId;

    public VectorImageView(Context context) {
        super(context);
        init();
    }

    public VectorImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VectorImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        vectorImageView = this;
        mRegionManager = new RegionManager();
    }

    public void loadAsset(String path) {
        sectorsPaths = new ArrayList<>();
        mRegionList = new ArrayList<>();
        mLineList = new ArrayList<>();
        mFinishedList = new ArrayList<>();
        mColorInfoList = new ArrayList<>();

        mImagePath = path;
        mImageId = Md5Util.md5(mImagePath);
        Log.e("ZGP", "imageId:" + mImageId);
        Sharp mSharp = Sharp.loadAsset(ColorPaintApplication.getAppContext().getAssets(), path);
        mSharp.setOnElementListener(vectorImageView);

        mSharp.getDrawable(vectorImageView, new Sharp.DrawableCallback() {
            @Override
            public void onDrawableReady(SharpDrawable sd) {
                sharpDrawable = sd;
                vectorImageView.setImageDrawable(sharpDrawable);

                if (onImageCallbackListener != null)
                    onImageCallbackListener.imageCallback();

                createMap();
                refreshPicture();
                onSvgDrawableReady();
            }
        });
    }

    @Override
    public void onSvgStart(@NonNull Canvas canvas, @Nullable RectF bounds) {
        mFinishedList.clear();
        sectorsPaths.clear();
    }

    /**
     * 因数据库无法存储Path属性，查找到记录后，使用现有色块数据
     */
    private void generateFinishedList() {
        if (mRegionManager == null) {
            mRegionManager = new RegionManager();
        }
        List<RegionInfo> list = mRegionManager.getFinishedRegions(mImagePath);
        if (list == null || list.size() <= 0) {
            return;
        }
        for (RegionInfo item : list) {
            RegionInfo existRegion = getExistRegion(mRegionList, item.number);
            if (existRegion != null) {
                mFinishedList.add(existRegion);
            }
        }
    }

    /**
     * 色块是否被填过色
     *
     * @param imageId 图片Id
     * @param number  区块编号
     * @return
     */
    public boolean isRegionColored(String imageId, int number) {
        RegionInfo info = mRegionManager.queryRegion(imageId, number);
        return info != null;
    }

    @Override
    public void onSvgEnd(@NonNull Canvas canvas, @Nullable RectF bounds) {
        //整理色块
        mColorInfoList = generateColorData(mRegionList);
        //读取已填色的色块
        generateFinishedList();
    }

    /**
     * 获取整理后的颜色列表
     *
     * @return
     */
    public List<ColorInfo> getColorInfoList() {
        return mColorInfoList;
    }

    @Override
    public void onSvgDrawableReady() {

    }

    @Override
    public <T> T onSvgElement(@Nullable String id, @NonNull T element, @Nullable RectF
            elementBounds, @Nullable Sharp.Properties props, @NonNull Canvas canvas, @Nullable RectF canvasBounds, @Nullable Paint paint) {
        if (paint != null && (element instanceof Path)) {
            int color = Color.WHITE;
            //绘制的是轮廓线条
            if (id == null||id.equals("0.00_0.00_0.00_0.00")) {
                color = paint.getColor();
                mLineList.add((Path) element);
            } else {
                //绘制的是色块
                RegionInfo info = new RegionInfo();
                info.regionId = id;
                String fillColor;
                if (props != null) {
                    fillColor = props.getColorStr("fill");
                    info.color = fillColor;
                    try {
                        color = Color.parseColor(fillColor);
                    } catch (Exception e) {
                        color = Color.WHITE;
                    }
                }
                info.path = (Path) element;
                info.number = mCount;
                info.imageId = mImageId;
                mCount++;
                mRegionList.add(info);
                sectorsPaths.add((Path) element);

            }
            paint.setColor(color);
        }
        //return element;
        return null;
    }

    @Override
    public <T> void onSvgElementDrawn(@Nullable String id, @NonNull T element, @NonNull Canvas
            canvas, @Nullable Paint paint) {
    }

    /**
     * 将色块按颜色值进行分类整理
     *
     * @param list 色块列表
     * @return
     */
    private List<ColorInfo> generateColorData(List<RegionInfo> list) {
        if (list == null || list.size() <= 0) {
            return null;
        }
        int count = 1;
        List<ColorInfo> result = new ArrayList<>();
        for (RegionInfo info : list) {
            if (info == null || TextUtils.isEmpty(info.color)) {
                continue;
            }
            String itemColor = info.color;
            ColorInfo existInfo = getInfoByColor(result, itemColor);
            boolean hasColored = isRegionColored(info.imageId, info.number);
            Log.e("ZGP",info.imageId+"---"+info.number+",色块是否填过色："+hasColored);
            //不存在的颜色，新增
            if (existInfo == null) {
                existInfo = new ColorInfo();
                existInfo.color = itemColor;
                existInfo.totalCount = 1;
                existInfo.number = count;
                if (hasColored) {
                    existInfo.finishedCount++;
                }
                List<RegionInfo> regions = new ArrayList<>();
                regions.add(info);
                existInfo.regions = regions;
                result.add(existInfo);
                count++;
            } else {
                //已存在的色块，更新对应数量
                List<RegionInfo> regions = existInfo.regions;
                if (regions == null) {
                    regions = new ArrayList<>();
                }
                if (hasColored) {
                    existInfo.finishedCount++;
                }
                existInfo.totalCount++;
                regions.add(info);
            }
        }
        return result;
    }

    /**
     * 获取指定色值的颜色信息
     *
     * @param list  颜色信息列表
     * @param color 色值
     * @return ColorInfo
     */
    private ColorInfo getInfoByColor(List<ColorInfo> list, String color) {
        if (TextUtils.isEmpty(color) || list == null || list.size() <= 0) {
            return null;
        }
        for (ColorInfo info : list) {
            if (info == null || TextUtils.isEmpty(info.color)) {
                continue;
            }
            String itemColor = info.color;
            if (itemColor.equals(color)) {
                return info;
            }
        }
        return null;
    }

    /**
     * 根据点击位置获取色块编号
     *
     * @param x x坐标
     * @param y y坐标
     * @return 编号
     */
    public int getRegionNumber(float x, float y) {
        int lX = Math.round(x * actW);
        int lY = Math.round(y * actH);
        int curSector;
        if (lX >= 0 && lY < bitmapMap.getHeight() && lX < bitmapMap.getWidth() && lY >= 0) {
            curSector = ((bitmapMap.getPixel(lX, lY) << 16) >>> 16) - 1;
            return curSector;
        }
        curSector = 0xFFFFFFFF;
        return curSector;
    }

    /**
     * 根据编号获取色块
     *
     * @param x x坐标
     * @param y y坐标
     * @return RegionInfo
     */
    public RegionInfo getRegion(float x, float y) {
        int number = getRegionNumber(x, y);
        if (mRegionList == null || mRegionList.size() <= 0) {
            return null;
        }
        for (RegionInfo item : mRegionList) {
            if (item == null) {
                continue;
            }
            if (item.number == number) {
                return item;
            }
        }
        return null;
    }


    public void setOnImageCommandsListener(OnImageCommandsListener onImageCommandsListener) {
        vectorImageView.onImageCommandsListener = onImageCommandsListener;
    }

    OnImageCommandsListener getOnImageCommandsListener() {
        return vectorImageView.onImageCommandsListener;
    }

    /**
     * 获取区块颜色配置表
     *
     * @return List<RegionInfo>
     */
    protected List<RegionInfo> getRegionColors() {
        return mRegionList;
    }

    /**
     * 获取同一个颜色编号中下一个待填色的位置
     *
     * @param colorPosition 颜色编号的位置
     * @return
     */
    public RegionInfo getNextFillRegion(int colorPosition) {
        ColorInfo colorInfo = mColorInfoList.get(colorPosition);
        if (colorInfo == null) {
            return null;
        }
        List<RegionInfo> regions = colorInfo.regions;
        if (regions == null) {
            return null;
        }
        RegionInfo nextRegion = null;
        for (RegionInfo item : regions) {
            RegionInfo existRegion = getExistRegion(mFinishedList, item.number);
            if (existRegion == null) {
                nextRegion = item;
                break;
            }
        }
        return nextRegion;
    }

    public interface OnImageCommandsListener {
        String getCurrentColor();
    }

    public void setOnImageCallbackListener(OnImageCallbackListener onImageCallbackListener) {
        this.onImageCallbackListener = onImageCallbackListener;
    }

    interface OnImageCallbackListener {
        void imageCallback();
    }

    private void createMap() {

        actW = sharpDrawable.getPicture().getWidth();

        if (onImageCallbackListener != null) {

            actH = sharpDrawable.getPicture().getHeight();

            Paint paint = new Paint();
            paint.setAntiAlias(false);

            Canvas canvas = sharpDrawable.getPicture().beginRecording(actW, actH);

            for (int i = 0; i < sectorsPaths.size(); i++) {
                paint.setColor(i + 1);
                paint.setAlpha(0xFF);
                canvas.drawPath(sectorsPaths.get(i), paint);
            }

            sharpDrawable.getPicture().endRecording();

            bitmapMap = Bitmap.createBitmap(actW, actH, Bitmap.Config.ARGB_8888);
            bitmapMap.eraseColor(0x00000000);

            Canvas bitmapCanvas = new Canvas(bitmapMap);
            sharpDrawable.draw(bitmapCanvas);

        }
    }

    public abstract void initThis();

    public Bitmap getShareBitmap(Drawable drawable) {
        int w = getResources().getDimensionPixelSize(R.dimen.share_image_width_px);
        int iw = drawable.getIntrinsicWidth();
        int ih = drawable.getIntrinsicHeight();
        float ar = (float) iw / w;
        int ah = (int) (ih / ar);
        int aw = (int) (iw / ar);

        Bitmap btm = Bitmap.createBitmap(aw, ah, Bitmap.Config.ARGB_8888);
        btm.eraseColor(0xFFFFFFFF);
        Canvas canvas = new Canvas(btm);
        int p = getResources().getDimensionPixelSize(R.dimen.share_image_padding_px);
        drawable.setBounds(p, p, aw - p, ah - p);
        drawable.draw(canvas);
        return btm;
    }


    /**
     * 刷新图片
     */
    public void refreshPicture() {

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Canvas canvas = sharpDrawable.getPicture().beginRecording(
                sharpDrawable.getPicture().getWidth(),
                sharpDrawable.getPicture().getHeight()
        );
        drawNumber(canvas);
        //绘制线条
        if (mLineList != null && mLineList.size() > 0) {
            paint.setColor(Color.BLACK);
            for (Path path : mLineList) {
                canvas.drawPath(path, paint);
            }
        }
        //绘制所有的色块
        for (RegionInfo info : mRegionList) {
            if (info == null || info.path == null) {
                continue;
            }
            paint.setColor(Color.WHITE);
            canvas.drawPath(info.path, paint);
        }
        //绘制已填色的色块
        for (RegionInfo info : mFinishedList) {
            if (info == null || info.path == null) {
                continue;
            }
            int color = Color.WHITE;
            try {
                color = Color.parseColor(info.color);
            } catch (Exception e) {
                e.printStackTrace();
                ;
            }
            paint.setColor(color);
            canvas.drawPath(info.path, paint);
        }
        sharpDrawable.getPicture().endRecording();
        vectorImageView.invalidate();
    }

    /**
     * 显示预填色块
     *
     * @param regions List<RegionInfo>
     */
    public void displayPreFillRegions(List<RegionInfo> regions) {
        if (regions == null || regions.size() <= 0) {
            return;
        }
        mPreFillList = regions;
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        if (sharpDrawable == null) {
            return;
        }
        Canvas canvas = sharpDrawable.getPicture().beginRecording(
                sharpDrawable.getPicture().getWidth(),
                sharpDrawable.getPicture().getHeight()
        );
        //绘制提示色块
        for (RegionInfo info : regions) {
            if (info == null || info.path == null) {
                continue;
            }
            canvas.drawPath(info.path, paint);
        }
        //绘制提示图案
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.canvas_texture_x).copy(Bitmap.Config.ARGB_8888, true);
        Paint bgPaint = new Paint();
        bgPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        bgPaint.setAntiAlias(true);
        canvas.drawBitmap(bitmap, 0, 0, bgPaint);

        drawNumber(canvas);
        //绘制线条
        if (mLineList != null && mLineList.size() > 0) {
            paint.setColor(Color.BLACK);
            for (Path path : mLineList) {
                canvas.drawPath(path, paint);
            }
        }
        //绘制已填色的色块
        for (RegionInfo info : mFinishedList) {
            if (info == null || info.path == null) {
                continue;
            }
            paint.setColor(Color.parseColor(info.color));
            canvas.drawPath(info.path, paint);
        }
        sharpDrawable.getPicture().endRecording();
        vectorImageView.invalidate();
    }

    /**
     * 绘制数字
     *
     * @param canvas 画布
     */
    private void drawNumber(Canvas canvas) {
        if (!mIsShowNumber || mRegionList == null || mRegionList.size() <= 0) {
            return;
        }
        Paint numberPaint = new Paint();
        numberPaint.setColor(Color.BLACK);
        numberPaint.setAntiAlias(true);
        numberPaint.setTextAlign(Paint.Align.CENTER);
        for (RegionInfo info : mRegionList) {
            if (info == null || TextUtils.isEmpty(info.regionId)) {
                continue;
            }
            ColorInfo colorInfo = getInfoByColor(mColorInfoList, info.color);
            if (colorInfo == null) {
                continue;
            }
            //绘制数字
            String[] location = info.regionId.split("_");
            if (location.length == 4) {
                float x = Float.parseFloat(location[0]);
                float y = Float.parseFloat(location[1]);
                float radius = Float.parseFloat(location[2]);
                numberPaint.setTextSize(radius);

                RectF rect = new RectF(x, y, x + radius, y + radius);
                Paint.FontMetricsInt fontMetrics = numberPaint.getFontMetricsInt();
                float baseline = (rect.bottom + rect.top - fontMetrics.bottom - fontMetrics.top) / 2;
                String content = String.valueOf(colorInfo.number);
                canvas.drawText(content, rect.centerX(), baseline, numberPaint);
            }

        }
    }


    /**
     * 对指定色块进行填色
     *
     * @param regionInfo RegionInfo
     */
    public void fillRegion(RegionInfo regionInfo) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        Canvas canvas = sharpDrawable.getPicture().beginRecording(
                sharpDrawable.getPicture().getWidth(),
                sharpDrawable.getPicture().getHeight()
        );
        //绘制提示色块
        for (RegionInfo info : mPreFillList) {
            if (info == null || info.path == null) {
                continue;
            }
            canvas.drawPath(info.path, paint);
        }
        //绘制提示图案
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.canvas_texture_x).copy(Bitmap.Config.ARGB_8888, true);
        Paint bgPaint = new Paint();
        bgPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        bgPaint.setAntiAlias(true);
        canvas.drawBitmap(bitmap, 0, 0, bgPaint);

        drawNumber(canvas);
        //绘制线条
        if (mLineList != null && mLineList.size() > 0) {
            paint.setColor(Color.BLACK);
            for (Path path : mLineList) {
                canvas.drawPath(path, paint);
            }
        }
        updateColoredRegion(regionInfo);
        //绘制已填色的色块
        for (RegionInfo info : mFinishedList) {
            if (info == null || info.path == null) {
                continue;
            }
            paint.setColor(Color.parseColor(info.color));
            canvas.drawPath(info.path, paint);
        }


        sharpDrawable.getPicture().endRecording();
        vectorImageView.invalidate();
    }

    /**
     * 更新已填色完成的色块
     *
     * @param info RegionInfo
     */
    public void updateColoredRegion(RegionInfo info) {
        if (info == null) {
            return;
        }

        RegionInfo existRegion = getExistRegion(mFinishedList, info.number);
        if (existRegion == null) {
            mFinishedList.add(info);
            mRegionManager.updateRegion(info);
        }
    }

    public void onScaleChanged(float scale) {
        boolean isShowNumber = (scale >= 1.5);
        if (isShowNumber != mIsShowNumber) {
            mIsShowNumber = isShowNumber;
            displayPreFillRegions(mPreFillList);
        }
    }

    /**
     * 获取已存在的色块
     *
     * @param list   List<RegionInfo>
     * @param number 色块编号
     * @return
     */
    private RegionInfo getExistRegion(List<RegionInfo> list, int number) {
        if (list == null || list.size() <= 0) {
            return null;
        }
        for (RegionInfo item : list) {
            if (item == null) {
                continue;
            }
            if (item.number == number) {
                return item;
            }
        }
        return null;
    }

    /**
     * 重置
     */
    public void reset() {
        Iterator<RegionInfo> iterator = mFinishedList.iterator();
        boolean hasDelete = false;
        while (iterator.hasNext()) {
            RegionInfo item = iterator.next();
            if (!hasDelete && !TextUtils.isEmpty(item.imageId)) {
                mRegionManager.deleteImage(item.imageId);
                hasDelete = true;
            }
            iterator.remove();
        }
        refreshPicture();
    }

    /**
     * 一键预填
     */
    public void colorAll() {
        for (RegionInfo item : mRegionList) {
            if (!mFinishedList.contains(item)) {
                mFinishedList.add(item);
            }
        }
        for (RegionInfo item : mRegionList) {
            mRegionManager.updateRegion(item);
        }
        refreshPicture();
    }
}