package com.demo.colorpaint.bean;

import java.io.Serializable;
import java.util.List;

/**
 * 需要填色的颜色信息
 * @date: 2020-01-29
 * @author: 山千
 */
public class ColorInfo implements Serializable {

    //色块内填的颜色
    public String color;

    //颜色编号
    public int number;

    //需要填色的总数
    public int totalCount;

    //已填色的数量
    public int finishedCount;

    //对应的色块信息
    public List<RegionInfo> regions;
}
