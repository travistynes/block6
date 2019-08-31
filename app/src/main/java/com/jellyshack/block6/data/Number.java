package com.jellyshack.block6.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Number {
	@PrimaryKey(autoGenerate = true)
	private long id;
	private String number;
	private int blockedCount;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getNumber() {
		return this.number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public int getBlockedCount() {
		return this.blockedCount;
	}

	public void setBlockedCount(int blockedCount) {
		this.blockedCount = blockedCount;
	}
}
