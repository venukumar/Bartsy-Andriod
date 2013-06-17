/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vendsy.bartsy;

import static com.vendsy.bartsy.utils.Utilities.SENDER_ID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;
import com.vendsy.bartsy.model.Order;
import com.vendsy.bartsy.utils.Utilities;
import com.vendsy.bartsy.utils.WebServices;

/**
 * IntentService responsible for handling GCM messages.
 */
public class GCMIntentService extends GCMBaseIntentService {
	public static final String REG_ID = "RegId";

	private static final String TAG = "GCMIntentService";

	public GCMIntentService() {

		super(SENDER_ID);
	}

	@Override
	protected void onRegistered(Context context, String registrationId) {
		Log.v(TAG, "senderid ::: " + SENDER_ID);
		Log.v(TAG, "Device registered: regId = " + registrationId);

		SharedPreferences settings = getSharedPreferences(REG_ID, 0);
		// String uname = settings.getString("user", "").toString();
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("RegId", registrationId);

		editor.commit();

	}

	@Override
	protected void onUnregistered(Context context, String registrationId) {
		Log.v(TAG, "Device unregistered");
		// displayMessage(context, getString(R.string.gcm_unregistered));
		if (GCMRegistrar.isRegisteredOnServer(getApplicationContext())) {
			// ServerUtilities.unregister(context, registrationId);
		} else {
			// This callback results from the call to unregister made on
			// ServerUtilities when the registration to the server failed.
			Log.v(TAG, "Ignoring unregister callback");
		}
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		String message = (String) intent.getExtras().get(Utilities.EXTRA_MESSAGE);
		String count = (String) intent.getExtras().get("badgeCount");

		Log.v(TAG, "message: " + message);
		
		// Process notification
		if (message != null)
			message = processPushNotification(message);
		
		// notifies user
		if (message != null)
			generateNotification(context, message, count);
	}

	/**
	 * To process push notification message
	 * 
	 * @param message
	 * @return
	 */
	private String processPushNotification(String message) {
		BartsyApplication app = (BartsyApplication) getApplication();
		String messageTypeMSG = "";
		try {
			Log.v(TAG, "push message " + message);
			JSONObject json = new JSONObject(message);
			if (json.has("messageType")) {

				if (json.getString("messageType").equals("updateOrderStatus")) {
					// Handle updateOrderStatus from Push Notification
					app.updateOrder(json.getString("orderId"), json.getString("orderStatus"));
					messageTypeMSG = "Your order status changed";
				} else if(json.getString("messageType").equals("orderTimeout")) {
					// Handle orderTimeout from Push Notification. Time Out is based on venue configuration
					app.updateOrder(json.getString("cancelledOrder"),json.getString("orderStatus"));
					messageTypeMSG = "Your order timed out";
				} else if (json.getString("messageType").equals("heartBeat")) {
					// Handle heart beat ping. 
					
					Log.v(TAG, "Heartbeat" + json);
					
					// Bartsy ID mismatch - don't send response for now
					if (app.loadBartsyId() == null ||
							!app.loadBartsyId().equalsIgnoreCase(json.getString("bartsyId"))) {
						Log.e(TAG, "Received hearbeat for user " + json.getString("bartsyId") + " instead of local user: " + app.loadBartsyId());
						return null;
					}
					
					
					// Update venue, order and people counts
					if (json.has("venueId"))
						app.updateActiveVenue(json.getString("venueId"), json.getString("venueName"), json.getInt("userCount"), json.getInt("orderCount"));
					else {
						// We don't have an active venue - make sure we don't and delete local references
						app.userCheckOut();
					}
					
					// Send reply to host
					WebServices.postHeartbeatResponse(app.getApplicationContext(), app.loadBartsyId(), app.mActiveVenue == null ? "" : app.mActiveVenue.getId());
					messageTypeMSG = null;
				}else if(json.getString("messageType").equals("DrinkOffered")){
					// To display offer drink dialog
					app.displayOfferDrink(new Order(json),json.getString("senderBartsyId"));
					if(json.has("body")){
						messageTypeMSG = json.getString("body");
					}
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return messageTypeMSG;
	}

	// @Override
	// protected void onDeletedMessages(Context context, int total) {
	// Log.v(TAG, "Received deleted messages notification");
	// String message = getString(R.string.gcm_deleted, total);
	// displayMessage(context, message);
	// // notifies user
	// generateNotification(context, message);
	// }

	@Override
	public void onError(Context context, String errorId) {
		Log.v(TAG, "Received error: " + errorId);
	}

	@Override
	protected boolean onRecoverableError(Context context, String errorId) {
		// log message
		Log.v(TAG, "Received recoverable error: " + errorId);
		return super.onRecoverableError(context, errorId);
	}

	/**
	 * Issues a notification to inform the user that server has sent a message.
	 * 
	 * @param count
	 * @param count
	 */
	private static void generateNotification(Context context, String message,
			String count) {
		int icon = R.drawable.ic_launcher;
		long when = System.currentTimeMillis();
		NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(icon, message, when);
		String title = context.getString(R.string.app_name);

		Intent notificationIntent = new Intent(context, MainActivity.class);
		// set intent so it does not start a new activity
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent intent = PendingIntent.getActivity(context, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(context, title, message, intent);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		try {
			int countValue = Integer.parseInt(count);
			notification.number = countValue;
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}

		notification.defaults = Notification.DEFAULT_ALL;
		notificationManager.notify(0, notification);
	}

}
