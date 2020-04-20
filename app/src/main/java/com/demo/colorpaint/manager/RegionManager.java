package com.demo.colorpaint.manager;

import android.text.TextUtils;

import com.demo.colorpaint.bean.RegionInfo;
import com.demo.colorpaint.db.AppDatabase;
import com.demo.colorpaint.db.dao.RegionDao;
import com.demo.colorpaint.utils.Md5Util;

import java.util.List;

/**
 * 色块数据管理器
 *
 * @date: 2020-01-30
 * @author: 山千
 */
public class RegionManager {
    private RegionDao mRegionDao;

    public RegionManager() {
        mRegionDao = AppDatabase.getInstance().regionDao();
    }

    /**
     * 获取图片中已填色的色块
     *
     * @param imagePath 图片路径
     * @return List<RegionInfo>
     */
    public List<RegionInfo> getFinishedRegions(String imagePath) {
        if (TextUtils.isEmpty(imagePath)) {
            return null;
        }
        String imageId = Md5Util.md5(imagePath);
        return mRegionDao.queryByImageId(imageId);
    }

    public RegionInfo queryRegion(String imageId, int number) {
        if (TextUtils.isEmpty(imageId) || number<0) {
            return null;
        }
        return mRegionDao.queryRegion(imageId, number);
    }

    /**
     * 插入一条色块填色记录
     *
     * @param regionInfo RegionInfo
     */
    public void updateRegion(RegionInfo regionInfo) {
        if (regionInfo == null) {
            return;
        }
        RegionInfo existInfo = mRegionDao.queryRegion(regionInfo.imageId, regionInfo.number);
        if (existInfo == null) {
            mRegionDao.insert(regionInfo);
        } else {
            mRegionDao.update(regionInfo);
        }
    }

    /**
     * 删除整张图片
     *
     * @param imageId
     */
    public void deleteImage(String imageId) {
        if (TextUtils.isEmpty(imageId)) {
            return;
        }
        List<RegionInfo> list = mRegionDao.queryByImageId(imageId);
        if (list == null) {
            return;
        }
        for (RegionInfo item : list) {
            mRegionDao.delete(item);
        }
    }
}
