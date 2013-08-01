package com.vendsy.bartsy.model;

import org.json.JSONException;
import org.json.JSONObject;

import com.vendsy.bartsy.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Option {
		
	protected String name;		// name of the option
	protected double price; 	// addition price of the option
	protected String specials;	// specials for this option
	protected boolean selected = false; // is this option selected?
	protected String type;
	
	private View mView = null;
	
	Option (JSONObject json, String type) throws JSONException {
		if (json.has("name"))
			name = json.getString("name");
		if (json.has("specials"))
			specials = json.getString("specials");
		if (json.has("price"))
			price = Double.parseDouble(json.getString("price"));
		if (json.has("selected"))
			selected = json.getBoolean("selected");
		
		this.type = type;
	}
	
	public View inflateOrder(LayoutInflater inflater) {

		mView = inflater.inflate(R.layout.order_option, null);
		mView.setTag(this);
		CheckBox optionName = (CheckBox) mView.findViewById(R.id.view_order_option_name);
		
		if (name != null) {
			String viewName = name;
			if (price != 0) {
				if (OptionGroup.OPTION_CHOOSE.equals(type)) {
					viewName = name + " ($" + price + ")";
				} else if (OptionGroup.OPTION_ADD.equals(type)) {
					viewName = name + " (add $" + price + ")";
				}
			}
			optionName.setText(viewName);
		}
		
		if (price != 0)
			((TextView) mView.findViewById(R.id.view_order_option_base_amount)).setText(Double.toString(price));

		setChecked(mView, selected);
		optionName.setTag(this);
		
		// The listener here selects/deselects the option depending on the type
		optionName.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				
				LinearLayout options = (LinearLayout) mView.getParent();
				OptionGroup optionGroup = (OptionGroup) options.getTag();
				
				if (OptionGroup.OPTION_CHOOSE.equals(optionGroup.type)) {

					// If we have a choose option, clear all options first

					if (!selected) {
						for (int i = 0 ; i < options.getChildCount() ; i++) {
							Option option = (Option) options.getChildAt(i).getTag();
							option.setChecked(option.mView, false);
						}
					}
					setChecked(mView, true);
				} else if (OptionGroup.OPTION_ADD.equals(optionGroup.type)) {

					// Reverse the selection for add groups
					setChecked(mView, !selected);
				}
			}
		});
		
		return mView;
	}
	
	private void setChecked(View view, boolean selected) {
		
		this.selected = selected;
		
		// Set check box tick mark
		((CheckBox) mView.findViewById(R.id.view_order_option_name)).setChecked(selected);
		
		// Show price if selected
		if (selected)
			view.findViewById(R.id.view_order_option_price).setVisibility(View.VISIBLE);
		else
			view.findViewById(R.id.view_order_option_price).setVisibility(View.INVISIBLE);
	}
	
}