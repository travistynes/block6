package com.jellyshack.block6.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.jellyshack.block6.BlockedNumberAdapter;
import com.jellyshack.block6.R;
import com.jellyshack.block6.data.DB;
import com.jellyshack.block6.data.Number;
import com.jellyshack.block6.data.Settings;

import java.util.List;

public class BlockedNumbersActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_blocked_numbers);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		ListView blockedItemsList = findViewById(R.id.blockedItemsList);
		BlockedNumberAdapter adapter = new BlockedNumberAdapter(this);
		blockedItemsList.setAdapter(adapter);
	}

	@Override
	protected void onResume() {
		super.onResume();

		loadMessages();
	}

	private void loadMessages() {
		ListView blockedItemsList = findViewById(R.id.blockedItemsList);
		BlockedNumberAdapter adapter = (BlockedNumberAdapter)blockedItemsList.getAdapter();
		adapter.clear();

		AsyncTask.execute(() -> {
			Settings settings = DB.getInstance(this).settingsDAO().getSettings();

			if (settings == null) {
				settings = new Settings();
				settings.setDisplayCountsEnabled(true);

				DB.getInstance(this).settingsDAO().insert(settings);
			}

			boolean displayCountsEnabled = settings.getDisplayCountsEnabled();
			List<Number> numbers = DB.getInstance(this).numberDAO().getAll();

			runOnUiThread(() -> {
				if(!numbers.isEmpty()) {
					adapter.loadNumbers(numbers, displayCountsEnabled);
				}

				showOrHideBlockedNumbers();
			});
		});
	}

	public void showOrHideBlockedNumbers() {
		View blockedItemListHolder = findViewById(R.id.blockedItemListHolder);
		View noBlocks = findViewById(R.id.noBlocks);
		ListView blockedItemsList = findViewById(R.id.blockedItemsList);

		if(blockedItemsList.getAdapter().getCount() == 0) {
			blockedItemListHolder.setVisibility(View.GONE);
			noBlocks.setVisibility(View.VISIBLE);
		} else {
			noBlocks.setVisibility(View.GONE);
			blockedItemListHolder.setVisibility(View.VISIBLE);
		}
	}
}
