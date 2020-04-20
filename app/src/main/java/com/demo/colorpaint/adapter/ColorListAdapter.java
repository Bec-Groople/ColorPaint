package com.demo.colorpaint.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.demo.colorpaint.R;
import com.demo.colorpaint.bean.ColorInfo;
import com.demo.colorpaint.bean.RegionInfo;
import com.demo.colorpaint.view.PaintProcessView;

import java.util.List;

/**
 * 颜色列表适配器
 *
 * @date: 2020-01-28
 * @author: 山千
 */
public class ColorListAdapter extends RecyclerView.Adapter<ColorListAdapter.ViewHolder> {
    private LayoutInflater mInflater;
    private List<ColorInfo> mData;
    private int mSelectedPosition = -1;

    //私有属性
    private OnItemClickListener mOnItemClickListener;

    //颜色填完回调
    private OnColorFilledListener mColorFilledListener;

    public ColorListAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
    }


    public void setData(List<ColorInfo> list) {
        if (mData != null) {
            mData.clear();
        }
        mData = list;
        notifyDataSetChanged();
    }

    /**
     * 选中指定位置
     *
     * @param position 位置
     */
    public void setSelected(int position) {
        if (position < 0) {
            return;
        }
        notifyItemChanged(mSelectedPosition);
        mSelectedPosition = position;
        notifyItemChanged(mSelectedPosition);
        ColorInfo colorInfo = mData.get(position);
        mOnItemClickListener.onItemClick(colorInfo);
    }

    @Override
    public int getItemCount() {
        if (mData == null) {
            return 0;
        }
        return mData.size();
    }

    /**
     * 创建ViewHolder
     */
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = mInflater.inflate(R.layout.view_paint_item,
                viewGroup, false);
        ViewHolder viewHolder = new ViewHolder(view);
        viewHolder.processView = view.findViewById(R.id.processView);
        viewHolder.ivFinish = view.findViewById(R.id.ivFinish);
        viewHolder.rootLayout = view.findViewById(R.id.layoutRoot);
        return viewHolder;
    }

    public void setOnColorFilledListener(OnColorFilledListener listener) {
        mColorFilledListener = listener;
    }

    /**
     * 更新所有数据的状态，注意此方法只适用于重置或一键填色功能！！
     * @param isReset 是否是重置
     */
    public void updateAllDataStatus(boolean isReset) {
        if (mData == null) {
            return;
        }
        for (ColorInfo item : mData) {
            if(isReset) {
                item.finishedCount = 0;
            }else {
                item.finishedCount=item.totalCount;
            }
        }
        notifyDataSetChanged();
    }

    /**
     * 设置值
     */
    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {
        if (mData == null || mData.size() <= 0) {
            return;
        }
        viewHolder.itemView.setSelected(mSelectedPosition == position);
        if (mSelectedPosition == position) {
            viewHolder.rootLayout.setBackgroundResource(R.color.colorGreen);
        } else {
            viewHolder.rootLayout.setBackgroundResource(R.color.colorBackground);
        }
        ColorInfo colorInfo = mData.get(position);
        if (colorInfo == null) {
            return;
        }
        if (colorInfo.finishedCount == colorInfo.totalCount) {
            viewHolder.ivFinish.setVisibility(View.VISIBLE);
        } else {
            viewHolder.ivFinish.setVisibility(View.GONE);
            viewHolder.processView.setValues(colorInfo.number, colorInfo.totalCount, colorInfo.finishedCount, colorInfo.color);
        }
        viewHolder.itemView.setOnClickListener(v -> {
            if (mOnItemClickListener != null) {
                notifyItemChanged(mSelectedPosition);//刷新上一个点击的item
                mSelectedPosition = position;
                notifyItemChanged(mSelectedPosition);//刷新当前点击item
                mOnItemClickListener.onItemClick(colorInfo);
            }
        });
    }

    public int getSelectedPosition(){
        return mSelectedPosition;
    }
    /**
     * 更新填色进度
     *
     * @param info RegionInfo
     */
    public void updateFillProcess(RegionInfo info) {
        if (mSelectedPosition < 0) {
            return;
        }
        ColorInfo colorInfo = mData.get(mSelectedPosition);
        if (colorInfo == null || info == null ) {
            return;
        }
        List<RegionInfo> regionInfos = colorInfo.regions;
        if (isExist(regionInfos, info.number)) {
            colorInfo.finishedCount++;
            notifyItemChanged(mSelectedPosition);
        }
        if (colorInfo.totalCount == colorInfo.finishedCount && mColorFilledListener != null) {
            //编号从1开始，List中的索引从0开始，需要减一
            mColorFilledListener.onColorFilled(colorInfo.number - 1);
        }
    }

    /**
     * 查询指定ID的色块是否存在
     *
     * @param list List<RegionInfo>
     * @param number   色块编号
     * @return
     */
    private boolean isExist(List<RegionInfo> list, int number) {
        if (list == null || list.size() <= 0) {
            return false;
        }
        for (RegionInfo item : list) {
            if (item == null ) {
                continue;
            }
            if (item.number==number) {
                return true;
            }
        }
        return false;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ViewHolder(View arg0) {
            super(arg0);
        }

        PaintProcessView processView;
        ImageView ivFinish;
        RelativeLayout rootLayout;
    }

    //setter方法
    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }

    //回调接口
    public interface OnItemClickListener {
        void onItemClick(ColorInfo colorInfo);
    }

    //某一种颜色都已填完的回调接口
    public interface OnColorFilledListener {
        void onColorFilled(int position);
    }
}