package com.demo.colorpaint;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.demo.colorpaint.adapter.ColorListAdapter;
import com.demo.colorpaint.bean.ColorInfo;
import com.demo.colorpaint.manager.RegionManager;
import com.demo.colorpaint.view.ColorPaintView;
import com.demo.colorpaint.view.VectorImageView;

import java.util.List;

/**
 * 填色界面
 *
 * @date: 2020-01-27
 * @author: 山千
 */
public class ColorPaintActivity extends Activity implements VectorImageView.OnImageCommandsListener, View.OnClickListener {

    private ColorPaintView mColorPaintView;
    private RecyclerView mRecyclerView;

    private SharedPreferences sPref;
    private String mImagePath;
    private ColorListAdapter mAdapter;
    private ImageView mIvBack;
    private String mCurrentColor = "#FFFFFF";
    private RegionManager mRegionManager;
    private List<ColorInfo> mColorList;
    private Button mBtnReset, mBtnColorAll, mBtnTip, mBtnAutoColor;
    private RelativeLayout mLayoutMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_paint);
        initView();
        initData();
    }

    private void initData() {
        mRegionManager = new RegionManager();

        Intent intent = getIntent();
        if (intent == null) {
            return;
        }
        mImagePath = intent.getStringExtra("path");
        if (TextUtils.isEmpty(mImagePath)) {
            return;
        }

        //设置适配器
        mAdapter = new ColorListAdapter(getApplicationContext());
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setOnItemClickListener(info -> {
            if (info == null) {
                return;
            }
            mCurrentColor = info.color;
            mColorPaintView.displayPreFillRegions(info.regions);
        });
        mAdapter.setOnColorFilledListener(index -> {
            int position = getNextFillPosition(mColorList, index);
            //色块都已填完
            if (position == -1) {
                showFinishDialog();
            } else {
                //找到了下一个色块，接着提示
                mAdapter.setSelected(position);
                smoothToPosition(mRecyclerView, position);
            }
        });
        mColorPaintView.setLoadListener(new ColorPaintView.SvgLoadListener() {
            @Override
            public void onLoadStart() {
                //开始加载
            }

            @Override
            public void onLoadEnd() {
                runOnUiThread(() -> {
                    mColorList = mColorPaintView.getColorInfoList();
                    //更新色块的填色进度
                    mAdapter.setData(mColorList);
                    int selectedPosition = getNextFillPosition(mColorList, 0);
                    if (selectedPosition == -1) {
                        showFinishDialog();
                    } else {
                        mAdapter.setSelected(selectedPosition);
                        smoothToPosition(mRecyclerView, selectedPosition);
                    }
                });

            }
        });
        mColorPaintView.setOnScaleChangeListener((scale, focusX, focusY) -> mLayoutMenu.setVisibility(scale < 1.5 ? View.VISIBLE : View.GONE));
        mColorPaintView.loadAsset(mImagePath);
    }

    private void showFinishDialog() {
        new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("恭喜你，图片填色完成啦!")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).show();
    }

    /**
     * 滑动到指定位置
     */
    private void smoothToPosition(RecyclerView mRecyclerView, final int position) {
        if (position < 0) {
            return;
        }
        // 第一个可见位置
        int firstItem = mRecyclerView.getChildLayoutPosition(mRecyclerView.getChildAt(0));
        // 最后一个可见位置
        int lastItem = mRecyclerView.getChildLayoutPosition(mRecyclerView.getChildAt(mRecyclerView.getChildCount() - 1));
        if (position < firstItem) {
            // 第一种可能:跳转位置在第一个可见位置之前，使用smoothScrollToPosition
            mRecyclerView.smoothScrollToPosition(position);
        } else if (position <= lastItem) {
            // 第二种可能:跳转位置在第一个可见位置之后，最后一个可见项之前
            int movePosition = position - firstItem;
            if (movePosition >= 0 && movePosition < mRecyclerView.getChildCount()) {
                int top = mRecyclerView.getChildAt(movePosition).getTop();
                // smoothScrollToPosition 不会有效果，此时调用smoothScrollBy来滑动到指定位置
                mRecyclerView.smoothScrollBy(0, top);
            }
        } else {
            // 第三种可能:跳转位置在最后可见项之后，则先调用smoothScrollToPosition将要跳转的位置滚动到可见位置
            // 再通过onScrollStateChanged控制再次调用smoothMoveToPosition，执行上一个判断中的方法
            mRecyclerView.smoothScrollToPosition(position);
        }
    }

    /**
     * 获取下一个待填色的位置
     *
     * @return
     */
    private int getNextFillPosition(List<ColorInfo> list, int position) {
        if (list == null) {
            return -1;
        }
        int size = list.size();
        if (size <= 0) {
            return -1;
        }
        //先往后查待填色位置
        int afterPosition = getFillPosition(list, position, size);
        //后面的没找到，再往前查找待填色位置
        if (afterPosition == -1) {
            return getFillPosition(list, 0, position);
        }
        return afterPosition;
    }

    /**
     * 从指定索引处开始查找待填色的位置
     *
     * @param list          List<ColorInfo>
     * @param startPosition 开始检索的位置
     * @return 在颜色列表中的索引
     */
    private int getFillPosition(List<ColorInfo> list, int startPosition, int endPosition) {
        if (list == null || startPosition < 0 || endPosition < 0 || startPosition > endPosition) {
            return -1;
        }
        int size = list.size();
        if (startPosition >= size || endPosition > size) {
            return -1;
        }
        for (int i = startPosition; i < endPosition; i++) {
            ColorInfo colorInfo = list.get(i);
            if (colorInfo == null) {
                continue;
            }
            if (colorInfo.totalCount != colorInfo.finishedCount) {
                return i;
            }
        }
        return -1;
    }


    private void initView() {
        mRecyclerView = findViewById(R.id.recyclerView);
        mIvBack = findViewById(R.id.ivBack);
        mColorPaintView = findViewById(R.id.paintView);
        mBtnReset = findViewById(R.id.btnReset);
        mBtnTip = findViewById(R.id.btnTip);
        mBtnColorAll = findViewById(R.id.btnColorAll);
        mLayoutMenu = findViewById(R.id.layoutMenu);
        mBtnAutoColor = findViewById(R.id.btnColorAuto);

        mColorPaintView.setOnImageCommandsListener(this);
        mColorPaintView.setOnImageCallbackListener(mColorPaintView);
        mColorPaintView.setRegionFillListener(regionInfo -> {
            if (regionInfo == null) {
                return;
            }
            mAdapter.updateFillProcess(regionInfo);
        });

        mIvBack.setOnClickListener(this);
        mBtnReset.setOnClickListener(this);
        mBtnColorAll.setOnClickListener(this);
        mBtnTip.setOnClickListener(this);
        mBtnAutoColor.setOnClickListener(this);


        //设置布局管理器
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerView.setLayoutManager(linearLayoutManager);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mColorPaintView.cleanup();
    }


    @Override
    public String getCurrentColor() {
        return mCurrentColor;
    }

    /**
     * 分享
     */
    private void share() {
        Uri uri = mColorPaintView.doShare();
        if (uri != null) {
            Intent myShareIntent = new Intent(Intent.ACTION_SEND);
            myShareIntent.setType("image/png");
            myShareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(myShareIntent, "Share to ..."));
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ivBack:
                finish();
                break;
            case R.id.btnReset:
                mColorPaintView.reset();
                mAdapter.updateAllDataStatus(true);
                break;
            case R.id.btnColorAll:
                mColorPaintView.colorAll();
                mAdapter.updateAllDataStatus(false);
                break;
            case R.id.btnTip:
                int position = mAdapter.getSelectedPosition();
                mColorPaintView.showNextFillRegion(position);
                break;
            case R.id.btnColorAuto:
                int nextPosition = mAdapter.getSelectedPosition();
                mColorPaintView.autoFill(nextPosition);
                break;
        }
    }
}
