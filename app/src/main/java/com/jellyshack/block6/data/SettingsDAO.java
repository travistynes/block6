package com.jellyshack.block6.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface SettingsDAO {
	@Query("select * from settings order by id desc limit 1")
	Settings getSettings();

	@Insert
	void insert(Settings settings);

	@Update
	int update(Settings settings);

	@Delete
	int delete(Settings settings);

	@Query("delete from settings")
	void clearTable();
}
