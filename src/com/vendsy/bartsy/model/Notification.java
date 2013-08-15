package com.vendsy.bartsy.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author Seenu Malireddy
 *
 */

public class Notification {
	
	public static final String TYPE_CHECKIN = "checkin";
	public static final String TYPE_CHECKOUT = "checkout";
	public static final String TYPE_PLACE_ORDER = "placeOrder";
	public static final String TYPE_UPDATE_ORDER = "updateorder";

	private String id; // unique id generated by the server
	private String message;
	private String type;
	private String userImage;
	private String createdTime;
	private String venueName;
	private String venueImage;
	private Order order;
	private String orderType;
	
	@Override
	public String toString() {

		return  "{" +
				" id: " + id +
				" message: " + message +
				" type: " + type +
				" userImage: " + userImage + 
				" createdTime: " + createdTime +
				"}";
	}
	
	public Notification(String ourBartsyId, JSONObject json) {
		try {
			message = json.getString("message");
			type = json.getString("type");
			createdTime = json.getString("createdTime");
			venueName = json.getString("venueName"); 
			venueImage = json.getString("venueImage"); 
			if(json.has("orderType")){
				orderType = json.getString("orderType");
				order = new Order(ourBartsyId, json);
			}
			
		} catch (JSONException e) {
		}
	}
	
	public boolean hasVenueImage(){
		return venueImage!=null;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUserImage() {
		return userImage;
	}

	public void setUserImage(String userImage) {
		this.userImage = userImage;
	}

	public String getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(String createdTime) {
		this.createdTime = createdTime;
	}

	public String getVenueName() {
		return venueName;
	}

	public String getVenueImage() {
		return venueImage;
	}

	public Order getOrder() {
		return order;
	}

	public String getOrderType() {
		return orderType;
	}
	
}
