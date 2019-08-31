package com.jellyshack.block6.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Settings {
	@PrimaryKey(autoGenerate = true)
	private int id;
	private boolean displayCountsEnabled = true;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public boolean getDisplayCountsEnabled() {
		return this.displayCountsEnabled;
	}

	public void setDisplayCountsEnabled(boolean displayCountsEnabled) {
		this.displayCountsEnabled = displayCountsEnabled;
	}
}
