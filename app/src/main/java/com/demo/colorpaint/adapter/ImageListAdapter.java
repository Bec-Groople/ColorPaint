package com.demo.colorpaint.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.drawable.VectorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.demo.colorpaint.ColorPaintActivity;
import com.demo.colorpaint.R;
import com.demo.colorpaint.bean.ImageInfo;
import com.pixplicity.sharp.Sharp;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @date: 2020-01-21
 * @author: 山千
 */
public class ImageListAdapter extends BaseAdapter {

    private Context mContext;

    private List<ImageInfo> mList;

    public ImageListAdapter(Context context) {
        mContext = context;
    }

    public void setData(List<ImageInfo> list) {
        if (mList != null) {
            mList.clear();
        }
        mList = list;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if (mList == null) {
            return 0;
        }
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.view_grid_list_item, parent, false);
            viewHolder = new ViewHolder();
            //通过ViewHolder获取实例
            viewHolder.tvName = convertView.findViewById(R.id.tvName);
            viewHolder.ivIcon = convertView.findViewById(R.id.ivIcon);
            //将ViewHolder存储在view中
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        final ImageInfo imageInfo = mList.get(position);
        if (imageInfo != null) {
            viewHolder.tvName.setText(imageInfo.name);
            AssetManager assetManager = mContext.getAssets();
            try {
                InputStream inputStream = assetManager.open(imageInfo.path);
                Sharp.loadInputStream(inputStream).into(viewHolder.ivIcon);

                VectorDrawable drawable=new VectorDrawable();
                drawable.getColorFilter();
            } catch (IOException e) {
                e.printStackTrace();
            }
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mContext, ColorPaintActivity.class);
                    intent.putExtra("path", imageInfo.path);
                    mContext.startActivity(intent);
                }
            });
        }
        return convertView;
    }

    class ViewHolder {
        public TextView tvName;
        public ImageView ivIcon;
    }
}
