package com.demo.colorpaint.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.demo.colorpaint.bean.RegionInfo;

import java.util.List;

/**
 * RegionInfo Dao
 *
 * @date: 2020-01-30
 * @author: 山千
 */
@Dao
public interface RegionDao {


    @Insert
    void insert(RegionInfo... infos);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    int update(RegionInfo... infos);

    @Delete
    void delete(RegionInfo... infos);

    @Query("SELECT * FROM region_info where imageId=:imageId and number=:number")
    RegionInfo queryRegion(String imageId,int number);

    @Query("SELECT * FROM region_info where imageId=:imageId")
    List<RegionInfo> queryByImageId(String imageId);
}
