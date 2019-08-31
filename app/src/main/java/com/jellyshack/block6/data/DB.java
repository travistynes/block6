package com.jellyshack.block6.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(version = 3, entities = {Settings.class, Number.class}, exportSchema = false)
public abstract class DB extends RoomDatabase {
	private static DB instance;
	public static final String DB_NAME = "block6.db";

	public abstract SettingsDAO settingsDAO();
	public abstract numberDAO numberDAO();

	public static DB getInstance(Context context) {
		if(instance == null) {
			instance = Room.databaseBuilder(context.getApplicationContext(), DB.class, DB.DB_NAME).fallbackToDestructiveMigration().build();
		}

		return instance;
	}
}