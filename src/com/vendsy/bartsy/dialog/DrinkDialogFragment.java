/**
 * 
 */
package com.vendsy.bartsy.dialog;

import java.text.DecimalFormat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.vendsy.bartsy.BartsyApplication;
import com.vendsy.bartsy.R;
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

	public Item item;
	public UserProfile profile;
	public float tipAmount;
	public float baseAmount;
	public float taxAmount;
	public float totalAmount;
	private View view;
	private BartsyApplication mApp;
    DecimalFormat df = new DecimalFormat();


	/*
	 * The activity that creates an instance of this dialog fragment must
	 * implement this interface in order to receive event callbacks. Each method
	 * passes the DialogFragment in case the host needs to query it.
	 */
	public interface OrderDialogListener {
		public void onDialogPositiveClick(DrinkDialogFragment dialog);
		public void onDialogNegativeClick(DrinkDialogFragment dialog);
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
		
		mApp = (BartsyApplication) getActivity().getApplication();
		
	}


	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		df.setMaximumFractionDigits(2);
		df.setMinimumFractionDigits(2);

		// Create dialog and set animation styles
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		// Get the layout inflater
		LayoutInflater inflater = getActivity().getLayoutInflater();

		// Inflate and set the layout for the dialog
		// Pass null as the parent view because its going in the dialog layout
		view = inflater.inflate(R.layout.dialog_drink_order, null);

		// Figure out totals
		if (item == null) {
			baseAmount = mApp.getActiveOrder().baseAmount;
			profile = mApp.mProfile;
		} else if (mApp.hasActiveOrder()) 
			baseAmount = mApp.getActiveOrder().baseAmount + item.getPrice();
		else
			baseAmount = item.getPrice();
		taxAmount = baseAmount * Constants.taxRate;
		tipAmount = baseAmount * (float) 20 / (float) 100; //  based on the radio button selected (default is 20%)
		totalAmount = baseAmount + taxAmount + tipAmount;

		// Set the total, tax and tip amounts
		((EditText) view.findViewById(R.id.view_dialog_drink_tip_amount)).setText(df.format(tipAmount));
		((TextView) view.findViewById(R.id.view_dialog_drink_tax_amount)).setText(df.format(taxAmount));
		((TextView) view.findViewById(R.id.view_dialog_drink_total_amount)).setText(df.format(totalAmount));

		
		// If we already have an open order add its items to the layout
		if (mApp.hasActiveOrder()) {
			for (Item item : mApp.getActiveOrder().items) {

				// Create item view
				View itemView = inflater.inflate(R.layout.item, null);				
				((TextView) itemView.findViewById(R.id.view_dialog_drink_title)).setText(item.getTitle());
				if (item.getDescription() != null && !item.getDescription().equalsIgnoreCase(""))
					((TextView) itemView.findViewById(R.id.view_dialog_drink_description)).setText(item.getDescription());
				else
					((TextView) itemView.findViewById(R.id.view_dialog_drink_description)).setVisibility(View.GONE);
				((TextView) itemView.findViewById(R.id.view_dialog_drink_price)).setText(df.format(item.getPrice()));
				
				// Add the item to the order view
				((LinearLayout) view.findViewById(R.id.view_dialog_drink_items)).addView(itemView);
			}
		}
		
		// Create the view for the item we may be adding and add it to the layout
		if (item != null) {
			View itemView = inflater.inflate(R.layout.item, null);				
			((TextView) itemView.findViewById(R.id.view_dialog_drink_title)).setText(item.getTitle());
			if (item.getDescription() != null && !item.getDescription().equalsIgnoreCase(""))
				((TextView) itemView.findViewById(R.id.view_dialog_drink_description)).setText(item.getDescription());
			else
				((TextView) itemView.findViewById(R.id.view_dialog_drink_description)).setVisibility(View.GONE);
			((TextView) itemView.findViewById(R.id.view_dialog_drink_price)).setText(df.format(item.getPrice()));
			((LinearLayout) view.findViewById(R.id.view_dialog_drink_items)).addView(itemView);
		}

		// Show profile information by default
		if (profile != null) updateProfileView(profile);
		
		// Setup up title and buttons
		builder.setView(view).setPositiveButton("Place order", this)
			.setNegativeButton("Add more items", this);
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

	private void updateProfileView(UserProfile profile) {
		ImageView profileImageView = ((ImageView)view.findViewById(R.id.view_user_dialog_image_resource));
		
		if (!profile.hasImage()) {
			WebServices.downloadImage(profile, profileImageView);
		} else {
			profileImageView.setImageBitmap(profile.getImage());
		}
	
		// Show the username of the recipient
		if (profile.getBartsyId() == mApp.mProfile.getBartsyId())
			((TextView) view.findViewById(R.id.view_user_dialog_info)).setText("You");	
		else
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
					DrinkDialogFragment.this.profile = userProfile;
					updateProfileView(userProfile);
					super.selectedProfile(userProfile);
				}
			};
			dialog.show(getActivity().getSupportFragmentManager(),"PeopleSectionDialog");
			break;
			
		case R.id.view_dialog_order_tip_10:
		case R.id.view_dialog_order_tip_15:
		case R.id.view_dialog_order_tip_20:
			
			float percent = 0;
			
			if (v.getId() == R.id.view_dialog_order_tip_10)
				percent = (float) 0.10;
			else if (v.getId() == R.id.view_dialog_order_tip_15)
				percent = (float) 0.15;
			else if (v.getId() == R.id.view_dialog_order_tip_20)
				percent = (float) 0.20;
			
			// Set the tip and total amount based on the radio button selected
			tipAmount = baseAmount * percent;
			totalAmount = tipAmount + taxAmount + baseAmount;
			((EditText) view.findViewById(R.id.view_dialog_drink_tip_amount)).setText(df.format(tipAmount));
			((TextView) view.findViewById(R.id.view_dialog_drink_total_amount)).setText(df.format(totalAmount));

			break;
		}
	}


	@Override
	public void onClick(DialogInterface dialog, int id) {
		
		RadioGroup tipPercentage = (RadioGroup) view.findViewById(R.id.view_dialog_drink_tip);
		EditText percentage = (EditText) view.findViewById(R.id.view_dialog_drink_tip_amount);
		
		// Send the positive button event back to the host activity

		int selected = tipPercentage.getCheckedRadioButtonId();

		// Gets a reference to our "selected" radio button
		RadioButton b = (RadioButton) tipPercentage.findViewById(selected);
		if (b == null || b.getText().toString().trim().length() == 0) {
			String tip="0";
			if (percentage.getText()!= null)
				tip = percentage.getText().toString();
			tipAmount = Float.parseFloat(tip) ;
		} else 
			tipAmount = Float.parseFloat(b.getText().toString().replace("%", "")) / (float) 100 * baseAmount;
		totalAmount = tipAmount + taxAmount + baseAmount;

		// Send the event to the calling activity
		switch (id) {
		case DialogInterface.BUTTON_POSITIVE:
				mListener.onDialogPositiveClick(DrinkDialogFragment.this);
				break;
		case DialogInterface.BUTTON_NEGATIVE:
			mListener.onDialogNegativeClick(DrinkDialogFragment.this);
			break;
		}
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
