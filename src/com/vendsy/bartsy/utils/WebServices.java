package com.vendsy.bartsy.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;

import com.vendsy.bartsy.GCMIntentService;
import com.vendsy.bartsy.R;
import com.vendsy.bartsy.VenueActivity;
import com.vendsy.bartsy.model.Order;
import com.vendsy.bartsy.model.UserProfile;

public class WebServices {

	private static final String TAG = "WebServices";

	/**
	 * To check internet connection
	 * 
	 * @param context
	 * @return
	 * @throws Exception
	 */
	public static boolean isNetworkAvailable(Context context) throws Exception {

		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm.getActiveNetworkInfo() != null
				&& cm.getActiveNetworkInfo().isFailover())
			return false;
		else if (cm.getActiveNetworkInfo() != null

		&& cm.getActiveNetworkInfo().isAvailable()
				&& cm.getActiveNetworkInfo().isConnected()) {

			return true;

		}

		else {

			return false;
		}

	}

	/**
	 * Create a new HttpClient and Post data
	 * 
	 * @param url
	 * @param postData
	 * @param context
	 * @return
	 * @throws Exception
	 */
	public static String postRequest(String url, JSONObject postData, Context context) throws Exception {

		String response = null;
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(url);

		String data = postData.toString();
		
		Log.v(TAG, "postRequest(" + url + ", " + data + ")");

		try {
			boolean status = isNetworkAvailable(context);
			if (status == true) {
				try {
					httppost.setEntity(new StringEntity(data));

					// Execute HTTP Post Request

					httppost.setHeader("Accept", "application/json");
					httppost.setHeader("Content-type", "application/json");

					HttpResponse httpResponse = httpclient.execute(httppost);

					String responseofmain = EntityUtils.toString(httpResponse.getEntity());
					response = responseofmain.toString();
				} catch (Exception e) {
					Log.e("log_tag", "Error in http connection" + e.toString());
					Log.v(TAG, "Exception found ::: " + e.getMessage());

				}
			}
		} catch (Exception e) {
			e.printStackTrace();

		}
		return response;

	}

	/**
	 * Service call for user check in and check out
	 * 
	 * @param url
	 * @param context
	 * @return
	 */
	public static String userCheckInOrOut (final Context context, int bartsyID, String venueId, String url) {
		String response = null;
		SharedPreferences sharedPref = context.getSharedPreferences(
				context.getResources().getString(
						R.string.config_shared_preferences_name),
				Context.MODE_PRIVATE);
		Resources r = context.getResources();

		Log.v(TAG, "bartsyId ::: " + bartsyID);
		final JSONObject json = new JSONObject();
		try {
			json.put("bartsyId", bartsyID);
			json.put("venueId", venueId);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		try {

			response = postRequest(url, json, context);
			Log.v(TAG, "CheckIn or Check Out response :: " + response);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}

	
	/**
	 * 
	 * Post the response to a heartbeat PN. The response simply indicates to the server
	 * the app is alive, connected to the internet, able to process PN's and able to send
	 * responses back. If the server does not receive the response for some period of time
	 * (currently 15 min), the server will check the user out of the venue.
	 * 
	 */
	
	public static void postHeartbeatResponse (final Context context, 
			String bartsyId, String venueId) {
		
		Log.v(TAG, "WebService.postHeartbeatResponse()");
		final JSONObject json = new JSONObject();

		// Prepare syscall
		try {
			json.put("bartsyId", bartsyId);
			json.put("venueId", venueId);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		// Invoke syscall
		try {
			// We are not interested in the response to the syscall as the server is what's
			// checking to see if everything is working property. If the server doesn't see
			// the heartbeat it will check the user out. 
			postRequest(Constants.URL_HEARTBEAT_RESPONSE, json, context);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	/*
	 * This Method i am using for each and every request which is going through
	 * get() method.
	 */
	public static String getRequest(String url, Context context) {
		BufferedReader bufferReader = null;
		StringBuffer stringBuffer = new StringBuffer("");
		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpRequest = new HttpGet();
		String result = "";

		try {
			boolean status = isNetworkAvailable(context);
			if (status == true) {
				// Set Url to http request
				httpRequest.setURI(new URI(url));

				HttpResponse response = httpClient.execute(httpRequest);

				bufferReader = new BufferedReader(new InputStreamReader(
						response.getEntity().getContent()));
				String line = "";
				String NL = System.getProperty("line.separator");
				while ((line = bufferReader.readLine()) != null) {
					stringBuffer.append(line + NL);
				}
				bufferReader.close();

			}
			result = stringBuffer.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * Service call for post order. 
	 * 
	 * @param context
	 * @param order
	 * @param venueID
	 * @param handler - this handler handles error events. It needs to be in the calling activity
	 * @return	true	- failed to send syscall
	 * 			false	- successfully sent syscall
	 */
	public static boolean postOrderTOServer(final Context context, final Order order, 
			String venueID, final Handler processOrderDataHandler) {
		final JSONObject orderData = order.getPlaceOrderJSON();
		Resources r = context.getResources();
		SharedPreferences sharedPref = context.getSharedPreferences(context.getResources().getString(R.string.config_shared_preferences_name), Context.MODE_PRIVATE);
		int bartsyId = sharedPref.getInt(r.getString(R.string.config_user_bartsyID), 0);

		// Prepare syscall 
		try {
			orderData.put("bartsyId", bartsyId);
			orderData.put("venueId", venueID);
			orderData.put("recieverBartsyId", bartsyId);

		} catch (JSONException e) {
			e.printStackTrace();
			return true;
		}

		// Place order webservice call in background
		new Thread() {
			@Override
			public void run() {
				
				Message msg = processOrderDataHandler.obtainMessage(VenueActivity.HANDLE_ORDER_RESPONSE_FAILURE);
				
				try {
					String response = postRequest(Constants.URL_PLACE_ORDER, orderData, context);
					Log.v(TAG, "Post order to server response :: " + response);
					
												
					JSONObject json = new JSONObject(response);
					String errorCode = json.getString("errorCode");
					
					if (errorCode.equalsIgnoreCase("0")) {
						// Error code 0 means the order was placed successfullly. Set the serverID of the order from the syscall.
						response = "success";
						order.serverID = json.getString("orderId");
						msg = processOrderDataHandler.obtainMessage(VenueActivity.HANDLE_ORDER_RESPONSE_SUCCESS);
					} else if (errorCode.equalsIgnoreCase("1")) {
						// Error code 1 means the venue doesn't accept orders.
						response = json.getString("errorMessage");
						msg = processOrderDataHandler.obtainMessage(VenueActivity.HANDLE_ORDER_RESPONSE_FAILURE_WITH_CODE, response);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				// Send the response back to the caller's response handler
				processOrderDataHandler.sendMessage(msg);
			}
		}.start();

		return false;
	}
	

	/**
	 * @methodName: postProfile
	 * 
	 *              Service call for profile information
	 * 
	 * @param user
	 * @param profileImage
	 * @param path
	 * @param context
	 * @return
	 */
	public static JSONObject postProfile(UserProfile user, String path, Context context) {

		String url = path;
		byte[] dataFirst = null;

		// Setup connection parameters
		int TIMEOUT_MILLISEC = 10000; // = 10 seconds
		HttpParams my_httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(my_httpParams, TIMEOUT_MILLISEC); 
		HttpConnectionParams.setSoTimeout(my_httpParams, TIMEOUT_MILLISEC); 

		// get registration id from shared preferences
		SharedPreferences settings = context.getSharedPreferences(
				GCMIntentService.REG_ID, 0);

		// Created a json object for posting data to server
		JSONObject json = new JSONObject();
		try {
			
			// Required system parameter
			json.put("deviceType", String.valueOf(Constants.DEVICE_Type));
			json.put("deviceToken", settings.getString("RegId", ""));

			
			// Check and place required user-related system parameters
			if (!user.hasUsername()) {
				Log.e(TAG, "Missing username");
				return null;
			} else {
				json.put("userName", user.getUsername());
			}
			if (!user.hasSocialNetworkId()) {
				Log.e(TAG, "Missing loginId (socialNetoworkId)");
				return null;
			} else {
				json.put("loginId", user.getSocialNetworkId());
			}
			if (!user.hasType()) {
				Log.e(TAG, "Missing type");
				return null;				
			} else {
				json.put("loginType", user.getType());
			}

			// Required parameters (user)
			if (!user.hasEmail()) {
				Log.e(TAG, "Missing email");
				return null;				
			} else {
				json.put("emailId", user.getEmail());
			}
			if (!user.hasPassword()) {
				Log.e(TAG, "Missing password");
				return null;				
			} else {
				json.put("password", user.getPassword());
			}
			if (!user.hasNickname()) {
				Log.e(TAG, "Missing nickname");
				return null;				
			} else {
				json.put("nickname", user.getNickname());
			}
			
			// Optional parameters (user)
			if (user.hasVisibility())
				json.put("showProfile", user.getVisibility());
			if (user.hasName())
				json.put("name", user.getName());
			if (user.hasFirstName())
				json.put("firstname", user.getFirstName());
			if (user.hasLastName())
				json.put("lastname", user.getLastName());
			if (user.hasBirthday())
				json.put("dateofbirth", user.getBirthday());
			if (user.hasStatus())
				json.put("status", user.getStatus()); 
			if (user.hasOrientation())
				json.put("orientation", user.getOrientation());
			if (user.hasDescription())
				json.put("description", user.getDescription());
			if (user.hasGender()) 
				json.put("gender", user.getGender());
			
		} catch (JSONException e1) {
			e1.printStackTrace();
			return null;
		}

		try {

			// Converting profile bitmap image into byte array
			if (user.hasImage()) {
				// Image found - converting it to a byte array and adding to syscall

				Log.v(TAG, "Syscall (with image): " + json);

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				user.getImage().compress(Bitmap.CompressFormat.JPEG, 100, baos);
				dataFirst = baos.toByteArray();


			} else {
				// Could not find image
				Log.v(TAG, "Syscall (no image): " + json);
			}

			// String details = URLEncoder.encode(json.toString(), "UTF-8");
			// url = url + details;

			// Execute HTTP Post Request

			HttpPost postRequest = new HttpPost(url);
			HttpClient client = new DefaultHttpClient();
			ByteArrayBody babFirst = null;

			if (dataFirst != null)
				babFirst = new ByteArrayBody(dataFirst, "userImage" + ".jpg");

			MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

			// added profile image into MultipartEntity

			if (babFirst != null)
				reqEntity.addPart("userImage", babFirst);
			if (json != null)
				reqEntity.addPart("details", new StringBody(json.toString(), Charset.forName("UTF-8")));
			postRequest.setEntity(reqEntity);
			HttpResponse responses = client.execute(postRequest);

			// Check response 

			if (responses != null){
				
				String responseofmain = EntityUtils.toString(responses.getEntity());
				Log.v(TAG, "postProfileResponseChecking " + responseofmain);
				JSONObject resultJson = new JSONObject(responseofmain);
				return resultJson;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}


	/**
	 * @methodName : getVenueList
	 * 
	 *             To get venue list from server
	 * 
	 * @param context
	 * @return list of venues
	 */
	public static String getVenueList(final Context context) {
		String response = null;

		response = WebServices.getRequest(Constants.URL_GET_VENU_LIST, context);
		Log.v(TAG, "response venu list " + response);
		return response;
	}
	
	
	public static String getIngredients(final Context context, String venueID){
		String response = null;
		JSONObject json = new JSONObject();
		try {
			json.put("venueId", venueID);
			response = postRequest(Constants.URL_GET_INGREDIENTS, json, context);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		Log.v(TAG, "response Ingredient list " + response);
		return response;
	}
	

	/**
	 * methodName : getUserOrdersList
	 * 
	 * @return ordersList
	 */

	public static String getUserOrdersList(Context context) {
		Resources r = context.getResources();
		SharedPreferences sharedPref = context.getSharedPreferences(context.getResources().getString(R.string.config_shared_preferences_name), Context.MODE_PRIVATE);
		int bartsyId = sharedPref.getInt(r.getString(R.string.config_user_bartsyID), 0);

		String response = null;
		try {

			JSONObject postData = new JSONObject();
			postData.put("bartsyId", bartsyId);
			response = WebServices.postRequest(
					Constants.URL_LIST_OF_USER_ORDERS, postData, context);

		} catch (Exception e) {
			Log.v(TAG, "getUserOdersList Exception found " + e.getMessage());
			return null;
		}
		return response;
	}

	/**
	 * To get list of menu list
	 * 
	 * @param context
	 * @param venueID
	 */
	public static String getMenuList(Context context, String venueID) {

		Log.v(TAG, "getting menu for venue: " + venueID);

		String response = null;
		JSONObject json = new JSONObject();
		try {
			json.put("venueId", venueID);
			response = postRequest(Constants.URL_GET_BAR_LIST, json, context);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		return response;
	}

	/**
	 * @methodName : downloadImage
	 * 
	 *             To download the image from server and set image bitmap to
	 *             imageView
	 * 
	 * @param fileUrl
	 * @param model
	 * @param imageView
	 */
	public static void downloadImage(final String fileUrl, final Object model,
			final ImageView imageView) {

		new AsyncTask<String, Void, Bitmap>() {
			Bitmap bmImg;

			protected void onPreExecute() {
				super.onPreExecute();

			}

			protected Bitmap doInBackground(String... params) {

				Log.v("file Url: ", fileUrl);
				URL myFileUrl = null;
				try {
					myFileUrl = new URL(fileUrl);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
				// Webservice call to get the image bitmap from the image url
				try {

					HttpURLConnection conn = (HttpURLConnection) myFileUrl
							.openConnection();
					conn.setDoInput(true);
					conn.connect();
					InputStream is = conn.getInputStream();
					bmImg = BitmapFactory.decodeStream(is);

				} catch (IOException e) {
					e.printStackTrace();
				}

				return bmImg;
			}

			protected void onPostExecute(Bitmap result) {
				if (model!=null && model instanceof UserProfile) {
					UserProfile profile = (UserProfile) model;
					profile.setImage(result);
				}
				// Set bitmap image to profile image view
				imageView.setImageBitmap(result);
				imageView.setTag(result);
			}

		}.execute();

	}

	
	// alert box
	public static void alertbox(final String message, final Activity a) {

		new AlertDialog.Builder(a).setMessage(message).setCancelable(false)
				.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {

						return;
					}
				}).show();
	}

}
