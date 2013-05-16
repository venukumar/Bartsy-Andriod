/**
 * 
 */
package com.vendsy.bartsy.view;

import com.vendsy.bartsy.R;
import com.vendsy.bartsy.BartsyApplication;
import com.vendsy.bartsy.VenueActivity;
import com.vendsy.bartsy.model.Order;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

/**
 * @author peterkellis
 *
 */
public class OrdersSectionFragment extends Fragment implements OnClickListener {

	private View mRootView = null;
	public LinearLayout mOrderListView = null;
	LayoutInflater mInflater = null;
	ViewGroup mContainer = null;
	public BartsyApplication mApp = null;
	
//	private String mDBText = "";

	/*
	 * Creates a map view, which is for now a mock image. Listen for clicks on the image
	 * and toggle the bar details image
	 */ 
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		Log.d("Bartsy", "OrdersSectionFragment.onCreateView()");

		mInflater = inflater;
		mContainer = container;
		mRootView = mInflater.inflate(R.layout.orders_main, mContainer, false);
		mOrderListView = (LinearLayout) mRootView.findViewById(R.id.order_list);
		
		mApp = (BartsyApplication) getActivity().getApplication();
		
		updateOrdersView();
		
		return mRootView;

	}

	public void updateOrdersView() {
		
		Log.i("Bartsy", "About to add orders list to the View");

		// Make sure the list view exists and is empty
		if (mOrderListView == null ) return;
		mOrderListView.removeAllViews();

		// Add any existing orders in the layout, one by one
		Log.i("Bartsy", "mApp.mOrders list size = " + mApp.mOrders.size());

		for (Order barOrder : mApp.mOrders) {
			Log.d("Bartsy", "Adding an item to the layout");
			barOrder.view = (View) mInflater.inflate(R.layout.order_item, mContainer, false);
			barOrder.updateView();
			barOrder.view.findViewById(R.id.view_order_button_positive).setOnClickListener(this);
			barOrder.view.findViewById(R.id.view_order_button_negative).setOnClickListener(this);

			mOrderListView.addView(barOrder.view);
		}
	}
	
	
	@Override 
	public void onDestroyView() {
		super.onDestroyView();

		Log.d("Bartsy", "OrdersSectionFragment.onDestroyView()");
		
		mRootView = null;
		mOrderListView = null;
		mInflater = null;
		mContainer = null;

	}
	
	@Override 
	public void onDestroy() {
		super.onDestroy();

		Log.i("Bartsy", "OrdersSectionFragment.onDestroy()");

		// Because the fragment may be destroyed while the activity persists, remove pointer from activity
		((VenueActivity) getActivity()).mOrdersFragment = null;
	}
		
	
	@Override
	public void onClick(View v) {

		Log.i("Bartsy", "Click event");

		switch (v.getId()) {
		case R.id.view_order_button_positive:
			Order order = (Order) v.getTag();
			Log.i("Bartsy", "Clicked on order positive button");

			// Update the order status locally and send the update to the remote
			order.nextPositiveState();
			((VenueActivity) getActivity()).sendOrderStatusChanged(order);
			
			if (order.status == Order.ORDER_STATUS_COMPLETE) {
				// Trash the order for now (later save it to log of past orders)
				removeOrder(order);
			} else {
				order.updateView();
			}
			break;
		}
	}
	
	public void removeOrder(Order order) {
		if (mOrderListView != null)
			mOrderListView.removeView(order.view);
//		((ViewGroup) order.view.getParent()).removeView(order.view);
		order.view = null;
		mApp.mOrders.remove(order);
	}

	
}
