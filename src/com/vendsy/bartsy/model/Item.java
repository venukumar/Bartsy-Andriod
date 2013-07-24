package com.vendsy.bartsy.model;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.j256.ormlite.field.DatabaseField;
import com.vendsy.bartsy.R;

/**
 * @author Seenu
 * 
 *         A MenuDrink object we are creating.
 */
public class Item {

	public String valid = null;
	
	private String itemId;
	private String title;
	private String description;
	private float price;
	private String specialPrice;
	private String venueId;
	private ArrayList<OptionGroup> optionGroups;
	
	protected class OptionGroup {
		protected String type;
		protected String text;
		protected ArrayList<Option> options;
		
		public OptionGroup (JSONObject json) throws JSONException {
			if (json.has("type"))
				type = json.getString("type");
			if (json.has("text"))
				text = json.getString("text");
			
			if (json.has("options")) {
				JSONArray optionsJSON = json.getJSONArray("options");
				options = new ArrayList<Option>();
				for (int i = 0 ; i < optionsJSON.length() ; i++) {
					JSONObject optionJSON = optionsJSON.getJSONObject(i);
					options.add(new Option(optionJSON));
				}
			}
		}
		
		protected class Option {
			protected String name;
			protected String price;
			
			public Option(JSONObject json) throws JSONException {
				if (json.has("name"))
					name = json.getString("name");
				if (json.has("price"))
					price = json.getString("price");
			}
		}
	}
	
	public Item () {
	}
	
	public Item(String title, String description, float price) {
		this.title = title;
		this.description = description;
		this.price = price;
	}
	
	public Item(JSONObject object) {
		try {
			
			// Process items of type Locu MENU_ITEM only 
			if (object.has("type"))
				if (!object.getString("type").equals("ITEM"))
					return;
			
			if (object.has("name"))
				this.title = object.getString("name");
			if (object.has("itemName"))
				this.title = object.getString("itemName");

			if (object.has("description"))
				this.description = object.getString("description");

			if (object.has("price"))
				this.price = Float.parseFloat(object.getString("price"));
			if (object.has("basePrice"))
				this.price = Float.parseFloat(object.getString("basePrice"));

			if (object.has("id"))
				this.itemId = object.getString("id");
			if (object.has("itemId"))
				this.itemId = object.getString("itemId");
			
			// Parse options
			if (object.has("option_groups")) {
				JSONArray optionGroupsJSON = object.getJSONArray("option_groups");
				optionGroups = new ArrayList<OptionGroup>();
				for (int i = 0 ; i < optionGroupsJSON.length() ; i++) {
					JSONObject optionGroupJSON = optionGroupsJSON.getJSONObject(i);
					optionGroups.add(new OptionGroup(optionGroupJSON));
				}
			}
			
			valid = "yes";

		} catch (JSONException e) {
			e.printStackTrace();
		}

	}


	public String getItemId() {
		return itemId;
	}

	public void setItemId(String drinkId) {
		this.itemId = drinkId;
	}

	
	/**
	 * @return the venueId
	 */
	public String getVenueId() {
		return venueId;
	}

	/**
	 * @param venueId the venueId to set
	 */
	public void setVenueId(String venueId) {
		this.venueId = venueId;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title
	 *            the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description
	 *            the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return float the price
	 */
	public float getPrice() {
		return price;
	}

	/**
	 * @param price
	 *            the price to set
	 */
	public void setPrice(float price) {
		this.price = price;
	}

	/**
	 * @return the price_special
	 */
	public String getPrice_special() {
		return specialPrice;
	}

	/**
	 * @param price_special
	 *            the price_special to set
	 */
	public void setPrice_special(String price_special) {
		this.specialPrice = price_special;
	}


}
