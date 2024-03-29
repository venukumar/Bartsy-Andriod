/**
 * 
 */
package com.vendsy.bartsy.dialog;

import java.text.DecimalFormat;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.vendsy.bartsy.BartsyApplication;
import com.vendsy.bartsy.CustomizeActivity;
import com.vendsy.bartsy.R;
import com.vendsy.bartsy.VenueActivity;
import com.vendsy.bartsy.adapter.ItemAdapter;
import com.vendsy.bartsy.model.Item;
import com.vendsy.bartsy.model.Order;
import com.vendsy.bartsy.model.UserProfile;
import com.vendsy.bartsy.utils.Constants;
import com.vendsy.bartsy.utils.WebServices;

/**
 * @author peterkellis
 * 
 */
public class DrinkDialogFragment extends SherlockDialogFragment implements DialogInterface.OnClickListener, OnClickListener, OnTouchListener {

	private static final int REQUEST_CODE_CUSTOM_DRINK = 2340;

	private static final String TAG = "DrinkDialogFragment";

	BartsyApplication mApp;
	ItemAdapter mItemAdapter;
	
	// Inputs/outputs
	public Order mOrder;
	
	// Local
	private View view;
	private DecimalFormat df = new DecimalFormat();

	private VenueActivity mActivity;

	/**
	 * Constructors
	 */

	public DrinkDialogFragment (BartsyApplication app, Order order) {
		this.mOrder = order;
		this.mApp = app;
	}
	
	
	/*
	 * The activity that creates an instance of this dialog fragment must
	 * implement this interface in order to receive event callbacks. Each method
	 * passes the DialogFragment in case the host needs to query it.
	 */
	public interface OrderDialogListener {
		public void onOrderDialogPositiveClick(DrinkDialogFragment dialog);
		public void onOrderDialogNegativeClick(DrinkDialogFragment dialog);
	}

	// Use this instance of the interface to deliver action events
	OrderDialogListener mListener;

	// Override the Fragment.onAttach() method to instantiate the
	// NoticeDialogListener
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// Verify that the host activity implements the callback interface
		try {
			// Instantiate the NoticeDialogListener so we can send events to the
			// host
			mListener = (OrderDialogListener) activity;
		} catch (ClassCastException e) {
			// The activity doesn't implement the interface, throw exception
			throw new ClassCastException(activity.toString() + " must implement NoticeDialogListener");
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		df.setMaximumFractionDigits(2);
		df.setMinimumFractionDigits(2);

		// Create dialog and set animation styles
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		// Get the layout inflater
		LayoutInflater inflater = getActivity().getLayoutInflater();
		mActivity = (VenueActivity) getActivity();

		// Inflate and set the layout for the dialog
		// Pass null as the parent view because its going in the dialog layout
		view = inflater.inflate(R.layout.order_dialog, null);

		// Show the total, tax and tip amounts
		((EditText) view.findViewById(R.id.view_dialog_drink_tip_amount)).setText(df.format(mOrder.tipAmount));
		((TextView) view.findViewById(R.id.view_dialog_drink_tax_amount)).setText(df.format(mOrder.taxAmount));
		((TextView) view.findViewById(R.id.view_dialog_drink_total_amount)).setText(df.format(mOrder.totalAmount));
		
		// If we already have an open order add its items to the layout
		ListView itemList = (ListView) view.findViewById(R.id.item_list);
		mItemAdapter = new ItemAdapter(getActivity(), R.layout.item_order, mOrder.items);
		itemList.setAdapter(mItemAdapter);
		itemList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Item item = mOrder.items.get(arg2);
				item.setOrder(mOrder);	// This item is part of an order. this will tell customize activity to show different text for the buttons
				CustomizeActivity.setInput(mApp, item);
				Intent intent = new Intent(getActivity(), CustomizeActivity.class);
				startActivityForResult(intent, REQUEST_CODE_CUSTOM_DRINK);
			}
		});
		
		// Show profile information by default
		if (mOrder.orderRecipient != null) updateProfileView(mOrder.orderRecipient);
		
		// Setup up title and buttons
		builder.setView(view);
		if (mOrder.items.isEmpty()) {
			// No items in this open order. Don't allow to place the order
		} else {
			builder.setPositiveButton("Place order", this);
		}
		builder.setNegativeButton("Add more items", this);
		builder.setTitle("Review your order");
		
		// Set radio button listeners
		view.findViewById(R.id.view_dialog_order_tip_10).setOnClickListener(this);
		view.findViewById(R.id.view_dialog_order_tip_15).setOnClickListener(this);
		view.findViewById(R.id.view_dialog_order_tip_20).setOnClickListener(this);


		// Set the  edit text listener which unselects radio buttons when the tip is entered manually
		((EditText) view.findViewById(R.id.view_dialog_drink_tip_amount)).setOnTouchListener(this);
		
		
		// Create dialog and set up animation
		return builder.create();
	}

	@Override
	public void onActivityResult(int requestCode, int responseCode, Intent data) {
		
		super.onActivityResult(requestCode, responseCode, data);


		Log.v(TAG, "Activity result for request: " + requestCode + " with response: " + responseCode);

		switch (requestCode) {
		case REQUEST_CODE_CUSTOM_DRINK:
			
			Item item = CustomizeActivity.getOutput(mApp);
			Order order = item.getOrder();
			item.setOrder(null);


			switch (responseCode) {
			case CustomizeActivity.RESULT_OK:

				// Update the item list
				updateTotals();
				mItemAdapter.notifyDataSetChanged();
				Toast.makeText(mApp, "Item updated", Toast.LENGTH_SHORT).show();
				
				break;
				
			case CustomizeActivity.RESULT_FIRST_USER:

				// Figure out if we are adding the item to the active order or creating a new order
				order.items.remove(item);
				
				// Close activity if there are no more items
				if (order.items.size() == 0) {
					mListener.onOrderDialogNegativeClick(DrinkDialogFragment.this);
					Toast.makeText(mApp, "Order cancelled", Toast.LENGTH_SHORT).show();
					mApp.setActiveOrder(null);
					dismiss();
				} else {
					mItemAdapter.notifyDataSetChanged();
					updateTotals();
					Toast.makeText(mApp, "Item removed", Toast.LENGTH_SHORT).show();
				}
				break;
			}
			break;
		}
		
	}
	
	private void updateProfileView(UserProfile profile) {
		ImageView profileImageView = ((ImageView)view.findViewById(R.id.view_user_dialog_image_resource));
		
		if (!profile.hasImage()) {
			WebServices.downloadImage(profile, profileImageView, mActivity.mImageCache);
		} else {
			profileImageView.setImageBitmap(profile.getImage());
		}
	
		// Show the username of the recipient
		((TextView) view.findViewById(R.id.view_user_dialog_info)).setText(profile.getNickname());	
		
		// To pick more user profiles when user pressed on the image view
		profileImageView.setOnClickListener(this);
		
	}


	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
		case R.id.view_user_dialog_image_resource:
			PeopleSectionFragmentDialog dialog = new PeopleSectionFragmentDialog(){
				@Override
				protected void selectedProfile(UserProfile userProfile) {
					// Update profile with new selected profile
					mOrder.orderRecipient = userProfile;
					mOrder.recipientId = userProfile.getBartsyId();
					updateProfileView(userProfile);
					super.selectedProfile(userProfile);
				}
			};
			dialog.show(getActivity().getSupportFragmentManager(),"PeopleSectionDialog");
			break;
			
		case R.id.view_dialog_order_tip_10:
		case R.id.view_dialog_order_tip_15:
		case R.id.view_dialog_order_tip_20:
			
			double percent = 0;
			
			if (v.getId() == R.id.view_dialog_order_tip_10)
				percent = (double) 0.10;
			else if (v.getId() == R.id.view_dialog_order_tip_15)
				percent = (double) 0.15;
			else if (v.getId() == R.id.view_dialog_order_tip_20)
				percent = (double) 0.20;
			
			// Set the tip and total amount based on the radio button selected
			mOrder.tipAmount = mOrder.baseAmount * percent;
			mOrder.totalAmount = mOrder.tipAmount + mOrder.taxAmount + mOrder.baseAmount;
			((EditText) view.findViewById(R.id.view_dialog_drink_tip_amount)).setText(df.format(mOrder.tipAmount));
			((TextView) view.findViewById(R.id.view_dialog_drink_total_amount)).setText(df.format(mOrder.totalAmount));

			break;
		}
	}


	@Override
	public void onClick(DialogInterface dialog, int id) {
		
		updateTotals();

		// Send the event to the calling activity
		switch (id) {
		case DialogInterface.BUTTON_POSITIVE:
				mListener.onOrderDialogPositiveClick(DrinkDialogFragment.this);
				break;
		case DialogInterface.BUTTON_NEGATIVE:
			mListener.onOrderDialogNegativeClick(DrinkDialogFragment.this);
			break;
		}
	}

	/* 
	 * Update and show price fields
	 */
	void updateTotals() {

		// Update order base amount
		mOrder.baseAmount = 0;
		for (Item item : mOrder.items)
			mOrder.baseAmount += item.getOrderPrice();
		
		// Update tip
		RadioGroup tipPercentage = (RadioGroup) view.findViewById(R.id.view_dialog_drink_tip);
		EditText percentage = (EditText) view.findViewById(R.id.view_dialog_drink_tip_amount);
		int selected = tipPercentage.getCheckedRadioButtonId();
		RadioButton b = (RadioButton) tipPercentage.findViewById(selected);
		if (b == null || b.getText().toString().trim().length() == 0) {
			String tip="0";
			if (percentage.getText()!= null)
				tip = percentage.getText().toString();
			mOrder.tipAmount = Double.parseDouble(tip) ;
		} else {
			mOrder.tipAmount = Double.parseDouble(b.getText().toString().replace("%", "")) / (double) 100 * mOrder.baseAmount;
		}
		
		// Update total
		mOrder.totalAmount = mOrder.tipAmount + mOrder.taxAmount + mOrder.baseAmount;
		
		// Show the total, tax and tip amounts
		((EditText) view.findViewById(R.id.view_dialog_drink_tip_amount)).setText(df.format(mOrder.tipAmount));
		((TextView) view.findViewById(R.id.view_dialog_drink_tax_amount)).setText(df.format(mOrder.taxAmount));
		((TextView) view.findViewById(R.id.view_dialog_drink_total_amount)).setText(df.format(mOrder.totalAmount));
	}
	
	@Override
	public boolean onTouch(View arg0, MotionEvent arg1) {
		switch (arg0.getId()) {
		case R.id.view_dialog_drink_tip_amount:
			((RadioGroup) view.findViewById(R.id.view_dialog_drink_tip)).clearCheck();
			break;
		}
		return false;
	}

}
