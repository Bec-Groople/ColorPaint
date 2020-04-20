package com.demo.colorpaint.bean;

import android.graphics.Path;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;

/**
 * 每一个色块的颜色信息
 *
 * @date: 2020-01-29
 * @author: 山千
 */
@Entity(tableName = "region_info")
public class RegionInfo implements Serializable {

    //自增主键
    @NonNull
    @PrimaryKey(autoGenerate = true)
    private int id;

    //色块对应的图片ID，以图片路径MD5生成
    @ColumnInfo(name = "imageId")
    public String imageId;

    //色块ID
    @ColumnInfo(name = "regionId")
    public String regionId;

    //色块内填的颜色
    @ColumnInfo(name = "color")
    public String color;

    //色块编号
    @ColumnInfo(name = "number")
    public int number;

    //填色区域
    @Ignore
    public Path path;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
