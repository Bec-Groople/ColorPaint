package com.demo.colorpaint.db;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.demo.colorpaint.ColorPaintApplication;
import com.demo.colorpaint.bean.RegionInfo;
import com.demo.colorpaint.db.dao.RegionDao;

/**
 * @date: 2020-01-30
 * @author: 山千
 */
@Database(entities = {RegionInfo.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    public abstract RegionDao regionDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance() {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = createDatabase();
                }
            }
        }
        return INSTANCE;
    }

    private static AppDatabase createDatabase() {
        return Room.databaseBuilder(ColorPaintApplication.getAppContext(), AppDatabase.class, "colorpaint.db")
                .allowMainThreadQueries()//允许在主线程查询数据
//                .addMigrations(MIGRATION_1_2)//迁移数据库使用
                .fallbackToDestructiveMigration()//迁移数据库如果发生错误，将会重新创建数据库，而不是发生崩溃
                .build();
    }

    /**
     * 数据库生级用，版本 1-2
     */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE region_info  ADD COLUMN name TEXT");
        }
    };
}
