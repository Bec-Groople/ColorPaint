package com.demo.colorpaint.bean;

import android.graphics.Color;
import android.graphics.Path;

import java.io.Serializable;
import java.util.List;

/**
 * 填色图片的信息
 *
 * @date: 2020-01-21
 * @author: 山千
 */
public class ImageInfo implements Serializable {
    //图片路径
    public String path;
    //图片名称
    public String name;
    //图片的填色信息
    public List<Color> colors;

}
