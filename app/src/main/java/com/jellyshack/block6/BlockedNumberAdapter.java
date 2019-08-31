package com.jellyshack.block6;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.jellyshack.block6.activity.BlockedNumbersActivity;
import com.jellyshack.block6.activity.MainActivity;
import com.jellyshack.block6.data.DB;
import com.jellyshack.block6.data.Number;

import java.util.ArrayList;
import java.util.List;

public class BlockedNumberAdapter extends BaseAdapter {
	private Context context;
	private LayoutInflater layoutInflater;
	private List<Number> numbers = new ArrayList<>();
	private boolean displayCountsEnabled;

	public BlockedNumberAdapter(Context context) {
		this.context = context;
		this.layoutInflater = LayoutInflater.from(context);
	}

	@Override
	public View getView(int position, View item, ViewGroup container) {
		if(item == null) {
			item = layoutInflater.inflate(R.layout.blocked_number_item, container, false);
		}

		Number number = this.numbers.get(position);

		TextView phoneNumber = item.findViewById(R.id.phoneNumber);
		phoneNumber.setText(number.getNumber());

		TextView blockedCount = item.findViewById(R.id.blockedCount);
		blockedCount.setText(String.valueOf(number.getBlockedCount()));

		if(!displayCountsEnabled) {
			blockedCount.setVisibility(View.GONE);
		} else {
			blockedCount.setVisibility(View.VISIBLE);
		}

		ImageButton trashButton = item.findViewById(R.id.trashButton);

		trashButton.setOnClickListener(button -> {
			button.playSoundEffect(android.view.SoundEffectConstants.CLICK);

			this.numbers.remove(position);
			this.notifyDataSetChanged();

			// Update the UI.
			((BlockedNumbersActivity)context).showOrHideBlockedNumbers();

			// Remove numbers from database.
			AsyncTask.execute(() -> {
				DB.getInstance(context).numberDAO().delete(number);
			});
		});

		return item;
	}

	public void loadNumbers(List<Number> numbers, boolean displayCountsEnabled) {
		this.numbers = numbers;
		this.displayCountsEnabled = displayCountsEnabled;

		this.notifyDataSetChanged();
	}

	public void clear() {
		this.numbers.clear();

		this.notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return this.numbers.size();
	}

	@Override
	public View getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return numbers.get(position).getId();
	}
}
