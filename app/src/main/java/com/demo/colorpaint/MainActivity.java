package com.demo.colorpaint;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.GridView;
import android.widget.LinearLayout;

import com.demo.colorpaint.adapter.ImageListAdapter;
import com.demo.colorpaint.bean.ImageInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private GridView mGridView;
    private ImageListAdapter mAdapter;
    private LinearLayout mRootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
    }

    private void initData() {
        List<ImageInfo> list = loadImages();
        mAdapter.setData(list);
    }

    /**
     * 加载assets目录下的SVG图
     *
     * @return List<ImageInfo>
     */
    private List<ImageInfo> loadImages() {
        AssetManager assetManager = getAssets();
        List<ImageInfo> list = new ArrayList<>();
        try {
            String[] paths = assetManager.list("svg");
            if (paths == null) {
                return null;
            }
            for (String item : paths) {
                if (TextUtils.isEmpty(item)) {
                    continue;
                }
                ImageInfo imageInfo = new ImageInfo();
                imageInfo.name = item;
                imageInfo.path = "svg/" + item;
                list.add(imageInfo);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    private void initView() {
        mGridView = findViewById(R.id.gridView);
        mRootLayout = findViewById(R.id.layoutRoot);
        mAdapter = new ImageListAdapter(this);
        mGridView.setAdapter(mAdapter);
    }

}
