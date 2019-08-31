package com.jellyshack.block6.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.room.Room;

import com.jellyshack.block6.R;
import com.jellyshack.block6.data.DB;

public class SettingsActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
	}

	public void manageBlockedNumbersButtonClick(View v) {
		Intent intent = new Intent(this, BlockedNumbersActivity.class);
		startActivity(intent);
	}

	public void resetButtonClick(View v) {
		AlertDialog alert = new AlertDialog.Builder(this)
				.setTitle("Warning")
				.setMessage("Are you sure you want to delete all blocked numbers?")
				.setPositiveButton("Yes", (dialog, id) -> {
					AsyncTask.execute(() -> {
						DB.getInstance(this).numberDAO().clearTable();
						DB.getInstance(this).settingsDAO().clearTable();
					});

					Toast.makeText(this, "Your settings have been reset.", Toast.LENGTH_LONG).show();

					// Exit this activity (go back).
					this.finish();
				})
				.setNegativeButton("Cancel", (dialog, id) -> {
					// Do nothing.
				})
				.create();

		alert.show();
	}
}
