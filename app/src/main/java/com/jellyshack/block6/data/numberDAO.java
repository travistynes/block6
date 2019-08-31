package com.jellyshack.block6.data;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface numberDAO {
	@Query("select * from Number order by id desc")
	List<Number> getAll();

	@Query("select * from Number where id = :id")
	Number getById(int id);

	@Query("select * from Number where number = :number")
	Number getByNumber(String number);

	@Insert
	long insert(Number number);

	@Update
	int update(Number number);

	@Delete
	int delete(Number number);

	@Query("delete from Number")
	void clearTable();
}
