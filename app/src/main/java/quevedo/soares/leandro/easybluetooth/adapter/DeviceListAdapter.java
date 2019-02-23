package quevedo.soares.leandro.easybluetooth.adapter;

import android.bluetooth.BluetoothDevice;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;

import java.util.ArrayList;

import quevedo.soares.leandro.easybluetooth.R;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {

	private RecyclerView recyclerView;
	private ArrayList<Item> deviceList;
	private OnBluetoothDeviceClickListener listener;

	public DeviceListAdapter (RecyclerView recyclerView) {
		this.recyclerView = recyclerView;
		this.recyclerView.setAdapter (this);
		//this.recyclerView.setHasFixedSize (true);
		this.recyclerView.setItemAnimator (new SimpleItemAnimator () {
			@Override
			public boolean animateRemove (RecyclerView.ViewHolder holder) {
				return false;
			}

			@Override
			public boolean animateAdd (RecyclerView.ViewHolder holder) {
				AnimationSet set = new AnimationSet (false);
				TranslateAnimation translateAnimation = new TranslateAnimation (
						Animation.RELATIVE_TO_SELF, 0f,
						Animation.RELATIVE_TO_SELF, 0f,
						Animation.RELATIVE_TO_SELF, 1f,
						Animation.RELATIVE_TO_SELF, 0f);
				translateAnimation.setDuration (250);
				translateAnimation.setInterpolator (new DecelerateInterpolator ());
				set.addAnimation (translateAnimation);

				AlphaAnimation alphaAnimation = new AlphaAnimation (0f, 1f);
				alphaAnimation.setDuration (100);
				set.addAnimation (alphaAnimation);

				holder.itemView.startAnimation (set);
				return true;
			}

			@Override
			public boolean animateMove (RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY) {
				AlphaAnimation alphaAnimation = new AlphaAnimation (1f, 0f);
				alphaAnimation.setDuration (250);

				holder.itemView.startAnimation (alphaAnimation);
				return false;
			}

			@Override
			public boolean animateChange (RecyclerView.ViewHolder oldHolder, RecyclerView.ViewHolder newHolder, int fromLeft, int fromTop, int toLeft, int toTop) {
				return false;
			}

			@Override
			public void runPendingAnimations () {

			}

			@Override
			public void endAnimation (RecyclerView.ViewHolder item) {

			}

			@Override
			public void endAnimations () {

			}

			@Override
			public boolean isRunning () {
				return false;
			}
		});

		this.deviceList = new ArrayList<> ();
	}

	public void setListener (OnBluetoothDeviceClickListener listener) {
		this.listener = listener;
	}

	public void addDevice (Item device) {
		this.recyclerView.post (() -> {
			this.deviceList.add (device);
			this.notifyItemInserted (this.deviceList.size () - 1);
		});
	}

	public void clearList () {
		this.recyclerView.post (() -> {
			int previousSize = this.deviceList.size ();
			this.deviceList.clear ();
			this.notifyItemRangeRemoved (0, previousSize);
		});
	}

	public void notifyDevicePaired (BluetoothDevice device) {
		for (int i = 0; i < deviceList.size (); i++) {
			Item item = deviceList.get (i);
			if (item.getDevice () == device) {
				item.setPaired (true);
				notifyItemChanged (i);
				return;
			}
		}
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder (@NonNull ViewGroup viewGroup, int i) {
		LayoutInflater inflater = LayoutInflater.from (this.recyclerView.getContext ());
		View view = inflater.inflate (R.layout.item_device, viewGroup, false);

		if (i == 0) {
			view.setBackgroundColor (Color.parseColor ("#ecf0f1"));
		} else if (i == 1) {
			view.setBackgroundColor (Color.parseColor ("#F9FAFB"));
		}

		return new ViewHolder (view);
	}

	@Override
	public void onBindViewHolder (@NonNull ViewHolder viewHolder, int i) {
		Item item = deviceList.get (i);

		int deviceColor = item.isPaired () ? Color.parseColor ("#16a085") : Color.parseColor ("#2980b9");

		// Set the item icon color
		Drawable drawable = viewHolder.tvDeviceName.getCompoundDrawablesRelative ()[0].mutate ();
		DrawableCompat.setTint (drawable, deviceColor);
		viewHolder.tvDeviceName.setCompoundDrawablesRelativeWithIntrinsicBounds (drawable, null, null, null);

		// Set the item text color
		viewHolder.tvDeviceName.setTextColor (deviceColor);
		viewHolder.tvDeviceName.setText (String.format ("%s - %s", item.getDevice ().getName (), item.getDevice ().getAddress ()));

		// Sets the onClickListener
		viewHolder.itemView.setOnClickListener ((view -> listener.onBluetoothDeviceSelected (item.getDevice ())));
	}

	@Override
	public int getItemViewType (int position) {
		return position % 2 == 0 ? 0 : 1;
	}

	@Override
	public int getItemCount () {
		return deviceList.size ();
	}

	public boolean isEmpty () {
		return this.deviceList.isEmpty ();
	}

	public static class Item {
		private BluetoothDevice device;
		private boolean paired;

		public Item (BluetoothDevice device, boolean paired) {
			this.device = device;
			this.paired = paired;
		}

		public BluetoothDevice getDevice () {
			return device;
		}

		public void setDevice (BluetoothDevice device) {
			this.device = device;
		}

		public boolean isPaired () {
			return paired;
		}

		public void setPaired (boolean paired) {
			this.paired = paired;
		}
	}

	class ViewHolder extends RecyclerView.ViewHolder {

		private TextView tvDeviceName;

		public ViewHolder (@NonNull View itemView) {
			super (itemView);

			tvDeviceName = itemView.findViewById (R.id.tvDeviceName);
		}
	}

	public interface OnBluetoothDeviceClickListener {
		void onBluetoothDeviceSelected (BluetoothDevice device);
	}

}
