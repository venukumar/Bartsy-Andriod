package com.vendsy.bartsy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.gms.plus.model.people.Person;
import com.paypal.android.MEP.PayPal;
import com.paypal.android.MEP.PayPalActivity;
import com.paypal.android.MEP.PayPalPayment;
import com.vendsy.bartsy.db.DatabaseManager;
import com.vendsy.bartsy.dialog.DrinkDialogFragment;
import com.vendsy.bartsy.dialog.PeopleDialogFragment;
import com.vendsy.bartsy.model.AppObservable;
import com.vendsy.bartsy.model.MenuDrink;
import com.vendsy.bartsy.model.Order;
import com.vendsy.bartsy.model.Profile;
import com.vendsy.bartsy.model.Section;
import com.vendsy.bartsy.utils.CommandParser;
import com.vendsy.bartsy.utils.CommandParser.BartsyCommand;
import com.vendsy.bartsy.utils.Constants;
import com.vendsy.bartsy.utils.Utilities;
import com.vendsy.bartsy.utils.WebServices;
import com.vendsy.bartsy.view.AppObserver;
import com.vendsy.bartsy.view.DrinksSectionFragment;
import com.vendsy.bartsy.view.OrdersSectionFragment;
import com.vendsy.bartsy.view.PeopleSectionFragment;

public class VenueActivity extends FragmentActivity implements
		ActionBar.TabListener, DrinkDialogFragment.NoticeDialogListener,
		PeopleDialogFragment.UserDialogListener, AppObserver {

	/****************
	 * 
	 * 
	 * TODO - global variables
	 * 
	 */

	public static final String TAG = "Bartsy";
	public DrinksSectionFragment mDrinksFragment = null;
	public OrdersSectionFragment mOrdersFragment = null; // make sure to set
															// this to null when
															// fragment is
															// destroyed
	public PeopleSectionFragment mPeopleFragment = null; // make sure to set
															// this to null when
															// fragment is
															// destroyed
	private Handler handler = new Handler();

	public void appendStatus(String status) {
		Log.d(TAG, status);
	}

	// A pointer to the parent application. In the MVC model, the parent
	// application is the Model
	// that this observe changes and observes

	public BartsyApplication mApp = null;

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	private static final int HANDLE_APPLICATION_QUIT_EVENT = 0;
	private static final int HANDLE_HISTORY_CHANGED_EVENT = 1;
	private static final int HANDLE_USE_CHANNEL_STATE_CHANGED_EVENT = 2;
	private static final int HANDLE_ALLJOYN_ERROR_EVENT = 3;
	private static final int HANDLE_ORDERS_UPDATED_EVENT = 4;
	private static final int HANDLE_PEOPLE_UPDATED_EVENT = 5;

	/**************************************
	 * 
	 * 
	 * TODO - Save/restore state
	 * 
	 * 
	 */
	/*
	 * static final String STATE_SCORE = "playerScore"; static final String
	 * STATE_LEVEL = "playerLevel"; ...
	 * 
	 * @Override public void onSaveInstanceState(Bundle savedInstanceState) { //
	 * Save the user's current game state savedInstanceState.putInt(STATE_SCORE,
	 * mCurrentScore); savedInstanceState.putInt(STATE_LEVEL, mCurrentLevel);
	 * savedInstanceState.
	 * 
	 * // Always call the superclass so it can save the view hierarchy state
	 * super.onSaveInstanceState(savedInstanceState); }
	 * 
	 * 
	 * public void onRestoreInstanceState(Bundle savedInstanceState) { // Always
	 * call the superclass so it can restore the view hierarchy
	 * super.onRestoreInstanceState(savedInstanceState);
	 * 
	 * // Restore state members from saved instance mCurrentScore =
	 * savedInstanceState.getInt(STATE_SCORE); mCurrentLevel =
	 * savedInstanceState.getInt(STATE_LEVEL); }
	 */

	/**********************
	 * 
	 * 
	 * TODO - Activity lifecycle management
	 * 
	 * 
	 **********************/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set base view for the activity
		setContentView(R.layout.activity_main);

		// Setup application pointer
		mApp = (BartsyApplication) getApplication();

		initializeFragments();

		// Log function call
		appendStatus(this.toString() + "onCreate()");

		// Set up the action bar custom view
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);

		// Create the adapter that will return a fragment for each of the
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager
				.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						actionBar.setSelectedNavigationItem(position);
					}
				});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the callback (listener) for when
			// this tab is selected.
			actionBar.addTab(actionBar.newTab()
					.setText(mSectionsPagerAdapter.getPageTitle(i))
					.setTabListener(this));
		}

		/*
		 * Now that we're all ready to go, we are ready to accept notifications
		 * from other components.
		 */
		mApp.addObserver(this);

	}

	/**
	 * Initialize the fragments
	 */
	private void initializeFragments() {

		Log.i(TAG, "MainActivity.initializeFragments()");

		// Initialize bartender fragment - the fragment may still exist even
		// though the activity has restarted
		OrdersSectionFragment f = (OrdersSectionFragment) getSupportFragmentManager().findFragmentById(R.string.title_drink_orders);
		if (f == null) {
			Log.i(TAG, "Bartender fragment not found. Creating one.");
			mOrdersFragment = new OrdersSectionFragment();
		} else {
			Log.i(TAG, "Bartender fragment found.");
			mOrdersFragment = f;
		}

		// Initialize people fragment - reuse the fragment if it's already in memory
		PeopleSectionFragment p = (PeopleSectionFragment) getSupportFragmentManager().findFragmentById(R.string.title_people);
		if (mPeopleFragment == null) {
			Log.i(TAG, "People fragment not found. Creating one.");
			mPeopleFragment = new PeopleSectionFragment();
		} else {
			Log.i(TAG, "People fragment found.");
			mPeopleFragment = p;
		}

		// Initialize people fragment - reuse the fragment if it's already in memory
		DrinksSectionFragment d = (DrinksSectionFragment) getSupportFragmentManager().findFragmentById(R.string.title_drinks);
		if (mDrinksFragment == null) {
			Log.i(TAG, "Drinks fragment not found. Creating one.");
			mDrinksFragment = new DrinksSectionFragment();
			mDrinksFragment.mActivity = this;
			mDrinksFragment.mApp = (BartsyApplication) getApplication();
			
			// Already Start loading the menu in the background so by the time the OS creates the view of this fragment
			// we've done some work
			mDrinksFragment.loadMenu();
		} else {
			Log.i(TAG, "Drinks fragment found.");
			mDrinksFragment = d;
		}
	}


	@Override
	public void onStart() {
		super.onStart();
		EasyTracker.getInstance().activityStart(this); // Add this method.

		// Log function call
		appendStatus(this.toString() + "onCreate()");

		/*
		 * Keep a pointer to the Android Application class around. We use this
		 * as the Model for our MVC-based application. Whenever we are started
		 * we need to "check in" with the application so it can ensure that our
		 * required services are running.
		 */

		mApp.checkin();

		/*
		 * update the state of the action bar depending on our connection state.
		 */
		updateActionBarStatus();

	}

	public void onStop() {
		super.onStop();
		Log.i(TAG, "onStop()");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "VenueActivity().onDestroy()");

		// Only stop listening to messages from the application when we're
		// killed (keep
		// listening while in the background with no active view)
		mApp.deleteObserver(this);

	}

	/******
	 * 
	 * 
	 * TODO - Action bar (menu) helper functions
	 * 
	 */

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);

		// Calling super after populating the menu is necessary here to ensure
		// that the
		// action bar helpers have a chance to handle this event.
		boolean retValue = super.onCreateOptionsMenu(menu);

		/*
		 * Set up Action buttons
		 */
		/*
		 * MenuItem item ; View menuItem; LayoutInflater inflater =
		 * (LayoutInflater)
		 * getActionBar().getThemedContext().getSystemService(Context
		 * .LAYOUT_INFLATER_SERVICE);
		 * 
		 * 
		 * // Setup tab button item = menu.findItem(R.id.action_tab); //
		 * ((TextView
		 * )mConnectedView.findViewById(R.id.actionBarConnectedText)).
		 * setText("(1 customer)");
		 * item.setActionView(inflater.inflate(R.layout.actionbar_tab, null));
		 * menuItem = item.getActionView().findViewById(R.id.button_tab);
		 * menuItem.setOnClickListener(new OnClickListener() {
		 * 
		 * @Override public void onClick(View v) { Intent activity = new
		 * Intent(getBaseContext(), NotificationsActivity.class);
		 * startActivity(activity); }}); item.expandActionView();
		 * 
		 * // Set requests action item = menu.findItem(R.id.action_requests); //
		 * ((TextView
		 * )mConnectedView.findViewById(R.id.actionBarConnectedText)).
		 * setText("(1 customer)");
		 * item.setActionView(inflater.inflate(R.layout.actionbar_requests,
		 * null));
		 * 
		 * 
		 * // Set messages action item = menu.findItem(R.id.action_messages); //
		 * ((TextView
		 * )mConnectedView.findViewById(R.id.actionBarConnectedText)).
		 * setText("(1 customer)"); menuItem =
		 * inflater.inflate(R.layout.actionbar_messages, null);
		 * item.setActionView(menuItem);
		 * 
		 * // Set notifications action item =
		 * menu.findItem(R.id.action_notifications); menuItem =
		 * inflater.inflate(R.layout.actionbar_notifications, null); //
		 * ((TextView
		 * )mConnectedView.findViewById(R.id.actionBarConnectedText)).
		 * setText("(1 customer)"); item.setActionView(menuItem); menuItem =
		 * item.getActionView(); menuItem.setOnClickListener(new
		 * OnClickListener() {
		 * 
		 * @Override public void onClick(View v) { Intent activity = new
		 * Intent(getBaseContext(), NotificationsActivity.class);
		 * startActivity(activity); }}); item.expandActionView();
		 */

		return retValue;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// app icon in action bar clicked; go home
			Intent intent = new Intent(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;

			/*
			 * case R.id.action_messages:
			 * item.getActionView().findViewById(R.id.
			 * view_action_bar_messages).setBackgroundColor(0xaaaaee); break;
			 * 
			 * <item android:id="@+id/menu_refresh"
			 * android:title="@string/menu_refresh"
			 * android:icon="@android:drawable/ic_popup_sync"
			 * android:showAsAction="always" />
			 * 
			 * // case R.id.menu_refresh: Toast.makeText(this,
			 * "Restarting P2P...", Toast.LENGTH_SHORT).show();
			 * 
			 * // Restart WiFi Direct discovery restartP2P(); break;
			 */
		case R.id.action_settings:
			Intent settingsActivity = new Intent(getBaseContext(),
					SettingsActivity.class);
			startActivity(settingsActivity);
			break;

		case R.id.action_quit:
			mApp.quit();
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void updateActionBarStatus() {

		SharedPreferences sharedPref = getSharedPreferences(getResources()
				.getString(R.string.config_shared_preferences_name),
				Context.MODE_PRIVATE);
		// To get the application resources
		Resources r = getResources();
		// To get venue id from shared preferences
		String venueId = sharedPref.getString(r.getString(R.string.venueId),
				"0");
		// If Venue id and name doesn't exits in shared preference we saving into shared preferences.
		if (venueId.equalsIgnoreCase("0") && mApp.activeVenue != null) {
			Utilities.saveVenueDetails(VenueActivity.this, mApp.activeVenue);
		}

		Log.i(TAG, "updateChannelState()");

		String name;

		if (mApp.activeVenue == null) {
			// Not checked in

			appendStatus("Channel iddle");

			// For now simply delete any open orders from the list
			mApp.mOrders.clear();
			if (mOrdersFragment != null
					&& mOrdersFragment.mOrderListView != null)
				mOrdersFragment.updateOrdersView();

			// For now, also simply delete the list of people present
			mApp.mPeople.clear();
			if (mPeopleFragment != null
					&& mPeopleFragment.mPeopleListView != null)
				mPeopleFragment.mPeopleListView.removeAllViews();

			// Set app title as not checked in for now. In the future this
			// should be an illegal state
			name = "Not checked in!";

		} else {
			// Checked-in

			name = mApp.activeVenue.getName();
		}

		getActionBar().setTitle(name);

		// Update the tab titles
		updateOrdersCount();
		updatePeopleCount();
	}

	/**
	 * Updates the action bar tab with the number of open orders
	 */

	public void updateOrdersCount() {
		// Update tab title with the number of orders - for now hardcode the tab
		// at the right position
		getActionBar().getTabAt(2).setText(
				"Orders (" + mApp.mOrders.size() + ")");
	}

	/*
	 * Updates the action bar tab with the number of open orders
	 */

	public void updatePeopleCount() {
		// Update tab title with the number of orders - for now hardcode the tab
		// at the right position
		getActionBar().getTabAt(1).setText(
				"People (" + mApp.mPeople.size() + ")");
	}

	/***********
	 * 
	 * TODO - Views management
	 * 
	 */

	@Override
	public void onTabSelected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());

		// upon selecting the people tab we want to update the list of people
		// from the server
		if (mTabs[tab.getPosition()] == R.string.title_people) {
			mPeopleFragment.updatePeopleView();
		}
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	VenueActivity main_activity = this;

	private int mTabs[] = { R.string.title_drinks, R.string.title_people,
			R.string.title_drink_orders };

	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch (mTabs[position]) {
			case R.string.title_drink_orders: // The order tab (for bar owners)
				return (mOrdersFragment);
			case R.string.title_drinks: // The drinks tab allows to order drinks
										// from previous orders, favorites, menu
										// items, drink guides or completely
										// custom.
				return (mDrinksFragment);
			case R.string.title_people: // The people tab shows who's local,
										// allows to send them a drink or a chat
										// request if they're available and
										// allows to leave comments for others
										// on the venue
				return (mPeopleFragment);
			default:
				return null;
			}
		}

		@Override
		public int getCount() {
			// Show total pages.
			return mTabs.length;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();

			return getString(mTabs[position]);
		}
	}

	void createNotification(String title, String text) {

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this).setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(title).setContentText(text);
		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(this, VenueActivity.class);

		// The stack builder object will contain an artificial back stack for
		// the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(VenueActivity.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
				PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		mNotificationManager.notify(0, mBuilder.build());

	}

	/*********************
	 * 
	 * 
	 * TODO - Bartsy protocol command handling and order management TODO - TODO
	 * - General command parsing/second TODO - Order command TODO - Order reply
	 * command TODO - Profile command TODO - User interaction commands.
	 * 
	 * 
	 */

	public synchronized void update(AppObservable o, Object arg) {
		Log.i(TAG, "update(" + arg + ")");
		String qualifier = (String) arg;

		if (qualifier.equals(BartsyApplication.APPLICATION_QUIT_EVENT)) {
			Message message = mHandler
					.obtainMessage(HANDLE_APPLICATION_QUIT_EVENT);
			mHandler.sendMessage(message);
		} else if (qualifier.equals(BartsyApplication.HISTORY_CHANGED_EVENT)) {
			Message message = mHandler
					.obtainMessage(HANDLE_HISTORY_CHANGED_EVENT);
			mHandler.sendMessage(message);
		} else if (qualifier
				.equals(BartsyApplication.USE_CHANNEL_STATE_CHANGED_EVENT)) {
			Message message = mHandler
					.obtainMessage(HANDLE_USE_CHANNEL_STATE_CHANGED_EVENT);
			mHandler.sendMessage(message);
		} else if (qualifier.equals(BartsyApplication.ALLJOYN_ERROR_EVENT)) {
			Message message = mHandler
					.obtainMessage(HANDLE_ALLJOYN_ERROR_EVENT);
			mHandler.sendMessage(message);
		} else if (qualifier.equals(BartsyApplication.ORDERS_UPDATED)) {
			Message message = mHandler
					.obtainMessage(HANDLE_ORDERS_UPDATED_EVENT);
			mHandler.sendMessage(message);
		} else if (qualifier.equals(BartsyApplication.PEOPLE_UPDATED)) {
			Message message = mHandler
					.obtainMessage(HANDLE_PEOPLE_UPDATED_EVENT);
			mHandler.sendMessage(message);
		}
	}

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HANDLE_APPLICATION_QUIT_EVENT:
				Log.i(TAG,
						"BartsyActivity.mhandler.handleMessage(): HANDLE_APPLICATION_QUIT_EVENT");
				finish();
				break;
			case HANDLE_USE_CHANNEL_STATE_CHANGED_EVENT:
				Log.i(TAG,
						"BartsyActivity.mhandler.handleMessage(): HANDLE_USE_CHANNEL_STATE_CHANGED_EVENT");
				updateActionBarStatus();
				break;
			case HANDLE_HISTORY_CHANGED_EVENT: {
				Log.i(TAG,
						"BartsyActivity.mhandler.handleMessage(): HANDLE_HISTORY_CHANGED_EVENT");

				String message = mApp.getLastMessage();

				// The history could be empty because this event is sent even on
				// a channel init
				if (message == null)
					break;

				BartsyCommand command = parseMessage(message);
				if (command != null) {
					processCommand(command);
				} else {
					Log.d(TAG, "Invalid command received");
				}
				break;
			}
			case HANDLE_ALLJOYN_ERROR_EVENT: {
				Log.i(TAG,
						"BartsyActivity.mhandler.handleMessage(): HANDLE_ALLJOYN_ERROR_EVENT");
				alljoynError();
			}
				break;
			case HANDLE_ORDERS_UPDATED_EVENT:
				Log.i(TAG,
						"BartsyActivity.mhandler.handleMessage(): HANDLE_ORDERS_UPDATED_EVENT");
				if (mOrdersFragment != null) {
					Log.i(TAG, "Updating orders view and count...");
					mOrdersFragment.updateOrdersView();
					updateOrdersCount();
				}
				break;
			case HANDLE_PEOPLE_UPDATED_EVENT:
				Log.i(TAG,
						"BartsyActivity.mhandler.handleMessage(): HANDLE_PEOPLE_UPDATED_EVENT");
				if (mPeopleFragment != null) {
					Log.i(TAG, "Updating people view and count...");
					mPeopleFragment.updatePeopleView();
					updatePeopleCount();
				}
				break;
			default:
				break;
			}
		}
	};
	private Order order;

	private void alljoynError() {
		if (mApp.getErrorModule() == BartsyApplication.Module.GENERAL
				|| mApp.getErrorModule() == BartsyApplication.Module.USE) {
			appendStatus("AllJoyn ERROR!!!!!!");
			// showDialog(DIALOG_ALLJOYN_ERROR_ID);
		}
	}

	public BartsyCommand parseMessage(String readMessage) {

		appendStatus("Message received: " + readMessage);

		// parse the command
		BartsyCommand command = null;
		ByteArrayInputStream stream = new ByteArrayInputStream(
				readMessage.getBytes());
		CommandParser commandParser = new CommandParser();

		try {
			command = commandParser.parse(stream);
		} catch (XmlPullParserException e) {
			// Auto-generated catch block
			e.printStackTrace();
			appendStatus("Invalid command format received");
			return null;
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
			appendStatus("Parser IO exception");
			return null;
		} finally {
			// Makes sure that the InputStream is closed after the app is
			// finished using it.
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					// Auto-generated catch block
					e.printStackTrace();
					appendStatus("Stream close IO exception");
					return null;
				}
			}
		}

		// check to make sure there was a
		if (command == null) {
			appendStatus("Parser succeeded but command is null");
			return null;
		}

		// Return successfully processed command
		return command;
	}

	void processCommand(BartsyCommand command) {
		if (command.opcode.equalsIgnoreCase("order_status_changed")) {
			if (processRemoteOrderStatusChanged(command))
				// An error occurred - for now log it
				appendStatus("ERROR PROCESSING ORDER STATUS CHANGED COMMAND");
		} else if (command.opcode.equalsIgnoreCase("profile")) {
			processProfile(command);
		} else
			appendStatus("Unknown command: " + command.opcode);
	}

	/******
	 * 
	 * 
	 * TODO - Send/receive drink order
	 * 
	 */

	@Override
	public void onDialogPositiveClick(DialogFragment dialog) {
		// User touched the dialog's positive button
		MenuDrink drink = ((DrinkDialogFragment) dialog).drink;

		appendStatus("Placing order for: " + drink.getTitle());
		if (mApp.activeVenue == null) {
			return;
		}
		order = new Order();
		String tip = ((DrinkDialogFragment) dialog).tipPercentageValue;
		String tipPercentageValue = tip.replace("%", "");
		order.tipAmount = Float.valueOf(tipPercentageValue);

		order.initialize(Long.toString(mApp.mOrderIDs), // arg(0) - Client order
														// ID
				null, // arg(1) - This order stil doesn't have a server-assigned
						// ID
				drink.getTitle(), // arg(2) - Title
				drink.getDescription(), // arg(3) - Description
				drink.getPrice(), // arg(4) - Price
				Integer.toString(R.drawable.drinks), // arg(5) - Image resource
														// for the order. for
														// now always use the
														// same picture for the
														// drink
														// drink.getImage(),
				mApp.mProfile); // arg(6) - Each order contains the profile of
								// the sender (and later the profile of the
								// person that should pick it up)
		order.itemId = drink.getDrinkId();

		// invokePaypalPayment(); // To enable paypal payment

		processOrderData(); // bypass PayPal for now for testing

	}

	/**
	 * To invoke PayPal payment
	 */
	private void invokePaypalPayment() {
		try {
			// Configure Paypal
			PayPalPayment newPayment = new PayPalPayment();
			newPayment.setSubtotal(BigDecimal.valueOf(order.total));
			newPayment.setCurrencyType("USD");
			// .setCurrency("USD");
			newPayment.setRecipient("example@paypal.com");
			newPayment.setMerchantName("Bartsy");

			PayPal pp = PayPal.getInstance();
			if (pp == null)
				pp = PayPal.initWithAppID(this, Constants.PAYPAL_KEY,
						PayPal.ENV_SANDBOX); // For now, Paypal sandbox is
												// enable

			// To check out the paypal payment
			Intent paypalIntent = pp.checkout(newPayment, this);
			this.startActivityForResult(paypalIntent, 1);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Invokes when the payment process is completed. Success/Failure cases has
	 * to be handle in this method
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode != 1)
			return;

		/**
		 * If you choose not to implement the PayPalResultDelegate, then you
		 * will receive the transaction results here. Below is a section of code
		 * that is commented out. This is an example of how to get result
		 * information for the transaction. The resultCode will tell you how the
		 * transaction ended and other information can be pulled from the Intent
		 * using getStringExtra.
		 */
		switch (resultCode) {
		case Activity.RESULT_OK:
			String resultTitle = "SUCCESS";
			String resultInfo = "You have successfully completed this ";
			System.out.println(resultInfo);

			processOrderData();

			// + (isPreapproval ? "preapproval." : "payment.");
			// resultExtra = "Transaction ID: " +
			// data.getStringExtra(PayPalActivity.EXTRA_PAY_KEY);
			break;
		case Activity.RESULT_CANCELED:
			resultTitle = "CANCELED";
			resultInfo = "The transaction has been cancelled.";
			System.out.println(resultInfo);
			String resultExtra = "";
			break;
		case PayPalActivity.RESULT_FAILURE:
			resultTitle = "FAILURE";
			resultInfo = data
					.getStringExtra(PayPalActivity.EXTRA_ERROR_MESSAGE);
			resultExtra = "Error ID: "
					+ data.getStringExtra(PayPalActivity.EXTRA_ERROR_ID);
		}
	}

	private void processOrderData() {

		if (Constants.USE_ALLJOYN) {

			// Send order to server
			mApp.newLocalUserMessage("<command><opcode>order</opcode>"
					+ "<argument>"
					+ mApp.mOrderIDs
					+ "</argument>"
					+ // client order ID
					"<argument>"
					+ mApp.mOrderIDs
					+ "</argument>"
					+ // server order ID
					"<argument>" + order.title + "</argument>" + "<argument>"
					+ order.description + "</argument>" + "<argument>"
					+ order.total + "</argument>"
					+ "<argument>" // Image +
					+ "</argument>" + "<argument>" + mApp.mProfile.userID
					+ "</argument>" +
					// Each order contains the profile of the sender (and
					// later the profile of the person that should pick it up)
					"</command>");
			appendStatus("Placed drink order");

		} else {
			// Web service call
			WebServices.postOrderTOServer(VenueActivity.this, order,
					mApp.activeVenue.getId());
		}

		// Add order to the list and update views
		mApp.addOrder(order);

		// Increment the local order count
		mApp.mOrderIDs++;

		// Update tab title with the number of open orders
		updateOrdersCount();

	}

	/*
	 * 
	 * TODO - Send/receive order status changed command
	 */

	public void sendOrderStatusChanged(Order order) {
		// Expects the order status and the server ID to be already set on this
		// end
		appendStatus("Sending order response for order: " + order.serverID);

		mApp.newLocalUserMessage("<command><opcode>order_status_changed</opcode>"
				+ "<argument>" + order.status + "</argument>" + // arg(0) -
																// status is
																// already
																// updated on
																// this end
				"<argument>" + order.serverID + "</argument>" + // arg(1)
				"<argument>" + order.clientID + "</argument>" + // arg(2)
				"<argument>" + order.orderSender.userID + "</argument>" + // arg(3)
				"</command>");

		// Update tab title with the number of open orders
		updateOrdersCount();

	}

	public Boolean processRemoteOrderStatusChanged(BartsyCommand command) {
		// Return false if everything went well, true if we need to perform
		// recovery

		appendStatus("Received new remote order status: "
				+ command.arguments.get(1));

		String orderSenderID = command.arguments.get(3);

		// Because with Alljoyn every connected client gets a command, we make
		// sure this command is for us
		if (!orderSenderID.equalsIgnoreCase(mApp.mProfile.userID))
			return true;

		mApp.updateOrder(command.arguments.get(1), command.arguments.get(0));

		return false;
	}

	/*
	 * 
	 * TODO - User interaction commands
	 */

	@Override
	public void onUserDialogPositiveClick(DialogFragment dialog) {
		// User touched the dialog's positive button

		Person user = ((PeopleDialogFragment) dialog).mUser;

		// DukesDonorUtil.getInstance().setDeviceId(registrationId);
		// displayMessage(context, getString(R.string.gcm_registered));
		// ServerUtilities.register(context, registrationId);

		appendStatus("Sending drink to: " + user.getNickname());

		mApp.newLocalUserMessage("<command><opcode>message</opcode>"
				+ "<argument>" + user.getNickname() + "</argument>"
				+ "<argument>" + "hi buddy" + "</argument>" + "</command>");
		appendStatus("Placed drink order");
	}

	@Override
	public void onUserDialogNegativeClick(DialogFragment dialog) {
		// User touched the dialog's positive button

		Person user = ((PeopleDialogFragment) dialog).mUser;

		appendStatus("Sending message to: " + user.getNickname());

		mApp.newLocalUserMessage("<command><opcode>message</opcode>"
				+ "<argument>" + user.getNickname() + "</argument>"
				+ "<argument>" + "hi buddy" + "</argument>" + "</command>");
		appendStatus("Sent message");
	}

	/*
	 * 
	 * TODO - Profile commands
	 */

	void processProfile(BartsyCommand command) {
		appendStatus("Process command: " + command.opcode);
		mApp.addPerson(command.arguments.get(0), command.arguments.get(1),
				command.arguments.get(2), command.arguments.get(3),
				command.arguments.get(4), command.arguments.get(5));
	}
}
