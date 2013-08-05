package com.vendsy.bartsy.model;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.j256.ormlite.field.DatabaseField;
import com.vendsy.bartsy.R;

/**
 * @author Seenu
 * 
 *         A MenuDrink object we are creating.
 */
public class Item {
	
	private static final String TAG = "Item";

	// Menu types
	private String type = null;
	public static final String MENU_ITEM 	 = "ITEM";
	public static final String ITEM_SELECT 	 = "ITEM_SELECT";
	public static final String SECTION_TEXT  = "SECTION_TEXT";
	public static final String BARTSY_ITEM 	 = "BARTSY_ITEM";
	public static final String LOADING		 = "LOADING";
	
	// For MENU_ITEM type
	private String itemId = null;
	private String name = null;
	private String description = null;
	private double price;
	private double specialPrice;
	private String venueId = null;

	private String glass = null;
	private String ingredients = null;
	private String instructions = null;
	private String specialInstructions = null;
	private String category = null;

	private String menuPath = null;
	private String optionsDescription = null;
	
	private ArrayList<OptionGroup> optionGroups = null;

	// For SECTION_TEXT type
	private String text = null;
	
	private String favoriteId;
	
	/**
	 * TODO - Constructors / parsers
	 */
	
	public Item () {
		this.type = LOADING;
	}
	
	public Item(String title, String description, double price) {
		this.type = MENU_ITEM;
		this.name = title;
		this.description = description;
		this.price = price;
	}
	
	public Item(JSONObject json, HashMap<String, JSONObject> savedSelections) throws JSONException, NumberFormatException {
			
		// Find the type and parse the json accordingly
		if (json.has("type"))
			type = json.getString("type");
		else 
			// Default as in orders we don't yet get full item details
			type = MENU_ITEM;
		
		if (type.equals(MENU_ITEM) || type.equals(BARTSY_ITEM) || type.equals(ITEM_SELECT)) {
			
			// Parse MENU_ITEM type
			if (json.has("name"))
				this.name = json.getString("name");
			if (json.has("itemName"))
				this.name = json.getString("itemName");
			if (json.has("description"))
				this.description = json.getString("description");
			if (json.has("optionsDescription"))
				this.description = json.getString("optionsDescription");
			if (json.has("options_description"))
				this.description = json.getString("options_description");
			if (json.has("price"))
				this.price = Double.parseDouble(json.getString("price"));
			if (json.has("id"))
				this.itemId = json.getString("id");
			if (json.has("itemId"))
				this.itemId = json.getString("itemId");
			if (json.has("glass"))
				glass = json.getString("glass");
			if (json.has("ingredients"))
				ingredients = json.getString("ingredients");
			if (json.has("instructions"))
				instructions = json.getString("instructions");
			if (json.has("special_instructions"))
				this.specialInstructions = json.getString("special_instructions");
			if (json.has("category"))
				category = json.getString("category");
			
			// Parse options
			JSONArray optionGroupsJSON = null;
			if (json.has("option_groups"))
				optionGroupsJSON = json.getJSONArray("option_groups");
			if (json.has("options_groups"))
				optionGroupsJSON = json.getJSONArray("options_groups");
			if (optionGroupsJSON != null) {
				
				optionGroups = new ArrayList<OptionGroup>();
				for (int i = 0 ; i < optionGroupsJSON.length() ; i++) {
					
					JSONObject optionGroupJSON = optionGroupsJSON.getJSONObject(i);

					// If we're saving a selection with ITEM_SELECT, save the first option
					if (type.equals(ITEM_SELECT) && i == 0 && savedSelections != null) {
						Log.v(TAG, "Saving selection " + name + " for: " + optionGroupJSON);
						savedSelections.put(name, optionGroupJSON);
					}
					
					if (optionGroupJSON.getString("type").equals(OptionGroup.OPTION_SELECT)
							&& savedSelections != null) {
						
						// If we're dealing with an OPTION_SELECT group, try to uncompress it or ignore it
						JSONArray optionsJSON = optionGroupJSON.getJSONArray("options");
						for (int j = 0 ; j < optionsJSON.length() ; j++) {
							String selectionName = optionsJSON.getString(j);
							if(savedSelections.containsKey(selectionName)){
								OptionGroup option = new OptionGroup(savedSelections.get(selectionName));
								if (option != null) {
									Log.v(TAG, "Loading selection " + selectionName + " for: " + name + ", " + optionGroupJSON);
									optionGroups.add(option);
								} else {
									Log.e(TAG, "Could not load selection: " + selectionName);
								}
							}
						}
					} else {
						// If it's not a compressed group just add it to the option groups
						optionGroups.add(new OptionGroup(optionGroupJSON));
					}
				}
			}
			
			// If the item is a cocktail (marked with a special item type called BARTSY_ITEM), adjust the prices accordingly
			if (type.equals(BARTSY_ITEM))
				adjustCocktailPrices();
			
			// Calculate the drink price based on selected options, if any.
			updateOptionsDescription();
			
			// Restore normal menu type
			type = MENU_ITEM;
			
		} else if (type.equals(SECTION_TEXT)) {
			
			// Parse SECTION_TEXT type
			text = json.getString("text");
		} else {
			throw new JSONException("Invalid menu item type");
		}
	}
	
	public String getFavoriteId() {
		return favoriteId;
	}

	public void setFavoriteId(String favoriteId) {
		this.favoriteId = favoriteId;
	}

	/**
	 * TODO - Serializers
	 */
	
	public JSONObject toJson() throws JSONException {

		JSONObject json = new JSONObject();

		if (has(itemId)) {
			json.put("itemId", itemId);
			json.put("id", itemId);
		}
		if (has(name)) {
			json.put("name", name);
			json.put("title", name);
			json.put("itemName", name);
		}
		// Hard-code quantity for now
			json.put("quantity", "1");
		if (has(type))
			json.put("type", type);
		if (has(description))
			json.put("description", description);
		if (has(optionsDescription))
			json.put("options_description", optionsDescription);

		if (has(price))
			json.put("price", Double.toString(price));
		if (has(glass))
			json.put("glass", glass);
		if (has(ingredients))
			json.put("ingredients", ingredients);
		if (has(instructions))
			json.put("instructions", instructions);
		if (has(specialInstructions))
			json.put("special_instructions", specialInstructions);
		if (has(category))
			json.put("category", category);
		
		if (has(optionGroups)) {
			JSONArray optionGroupsJson = new JSONArray();
			for (OptionGroup optionGroup : optionGroups)
				optionGroupsJson.put(optionGroup.toJson());
			json.put("option_groups", optionGroupsJson);
		}
		
		// Send the price for the bartender app
		json.put("basePrice", Double.toString(getPrice()));
		
		return json;
	}
	
	@Override 
	public String toString ()  {
		try {
			return toJson().toString();
		} catch (JSONException e) {
			e.printStackTrace();
			return "Item serializer error";
		}
	}
	
	/**
	 * 
	 * TODO - Views
	 * 
	 */
	
	public View customizeView(LayoutInflater inflater) {
		
		View view = inflater.inflate(R.layout.item_customize, null);
		
		// Set title and description
		if (hasTitle()) {
			((TextView) view.findViewById(R.id.view_order_item_name)).setText(getTitle());
			if (hasDescription())
				((TextView) view.findViewById(R.id.view_order_item_description)).setText(getDescription());
			else 
				((TextView) view.findViewById(R.id.view_order_item_description)).setVisibility(View.GONE);
		} else {
			view.findViewById(R.id.view_order_item_header).setVisibility(View.GONE);
		}
		
		// Set options views
		LinearLayout options = (LinearLayout) view.findViewById(R.id.view_order_item_options);
		options.setTag(this);

		if (getOptionGroups() != null) {
			for (OptionGroup optionGroup : getOptionGroups()) {
				options.addView(optionGroup.customizeView(inflater));
			}
		}
		
		return view;
	}
	
	public View orderView(LayoutInflater inflater) {

	    DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(0);
		df.setMinimumFractionDigits(0);

		LinearLayout view = (LinearLayout) inflater.inflate(R.layout.item_order, null);
		
		((TextView) view.findViewById(R.id.view_order_mini_price)).setText(df.format(getPrice()));
		((TextView) view.findViewById(R.id.view_order_title)).setText(getTitle());
		if (has(optionsDescription))
			((TextView) view.findViewById(R.id.view_order_description)).setText(getOptionsDescription());
		else
			view.findViewById(R.id.item_order_description_field).setVisibility(View.GONE);
		if (has(specialInstructions))
			((TextView) view.findViewById(R.id.item_order_special_instructions)).setText(getSpecialInstructions());
		else
			view.findViewById(R.id.item_order_special_instructions_field).setVisibility(View.GONE);
		
		
		return view;
	}

	public JSONArray getJSONForFavorite(){
		
		// Create JSON Array for the items
		JSONArray array = new JSONArray();
		
		JSONObject itemInjson = new JSONObject();
		// Prepare the item information for MENU_ITEM and BARTSY_ITEM
		try {
			if(getOptionGroups().size()==0){
				
				itemInjson.put("title", getTitle());
				itemInjson.put("itemName", getTitle());
				itemInjson.put("description", getDescription());
				
				// TODO hard coded quantity as 1 for now
				itemInjson.put("quantity", "1");
				itemInjson.put("itemId", getItemId());
				
				array.put(itemInjson);
			}
			// Prepare the item information
			else{
				for (OptionGroup optionGroup : getOptionGroups()) {
					// Get the options from options array and add to the JSON array	
					for (Option option : optionGroup.options) {
						if(option.selected){
							JSONObject optionInjson = new JSONObject();
							optionInjson.put("itemName", option.name);
							array.put(optionInjson);
						}
					}
				}
			}
		} catch (JSONException e) {
		}
		
		return array;
	}
	
	/**
	 * 
	 * TODO Getters and setters
	 * 
	 */

	public boolean has(String field) {
		return !(field == null || field.equals(""));
	}
	
	public boolean has(Object object) {
		return object != null;
	}
	
	public String getItemId() {
		return itemId;
	}

	public void setItemId(String drinkId) {
		this.itemId = drinkId;
	}

	
	public String getVenueId() {
		return venueId;
	}

	public void setVenueId(String venueId) {
		this.venueId = venueId;
	}

	public boolean hasTitle() {
		return ! (name == null || name.equals(""));
	}
	
	public String getTitle() {
		return name;
	}

	public void setTitle(String title) {
		this.name = title;
	}

	public boolean hasDescription() {
		return ! (description == null || description.equals(""));
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public double getPrice() {

		Double price = this.price;
		
		if (optionGroups == null)
			return price;
		
		for (OptionGroup options : optionGroups) {
			for (Option option : options.options) {
				if (option.selected)
					price += option.price;
			}
		}
		
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public double getPrice_special() {
		return specialPrice;
	}

	public void setPrice_special(double price_special) {
		this.specialPrice = price_special;
	}

	public ArrayList<OptionGroup> getOptionGroups() {
		return optionGroups;
	}

	public void setOptionGroups(ArrayList<OptionGroup> optionGroups) {
		this.optionGroups = optionGroups;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getSpecialPrice() {
		return specialPrice;
	}

	public void setSpecialPrice(double specialPrice) {
		this.specialPrice = specialPrice;
	}

	public String getGlass() {
		return glass;
	}

	public void setGlass(String glass) {
		this.glass = glass;
	}

	public String getIngredients() {
		return ingredients;
	}

	public void setIngredients(String ingredients) {
		this.ingredients = ingredients;
	}

	public String getInstructions() {
		return instructions;
	}

	public void setInstructions(String instructions) {
		this.instructions = instructions;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getMenuPath() {
		return menuPath;
	}

	public void setMenuPath(String menuPath) {
		this.menuPath = menuPath;
	}

	public String getOptionsDescription() {
		return optionsDescription;
	}

	public void setOptionsDescription(String optionDescription) {
		this.optionsDescription = optionDescription;
	}
	
	public String getSpecialInstructions() {
		return specialInstructions;
	}

	public void setSpecialInstructions(String specialInstructions) {
		this.specialInstructions = specialInstructions;
	}
	
	
	/**
	 * TODO - Utilities
	 */
	
	/*
	 * Updates the optionsDescription
	 */
	public void updateOptionsDescription() {

		optionsDescription = "";
		
		if (optionGroups == null) {
			optionsDescription = "Ordered 'as-is.'";
			return;
		}
		
		for (OptionGroup options : optionGroups) {
			for (Option option : options.options) {
				if (option.selected) {
					if (optionsDescription.endsWith(", ") || optionsDescription.equals(""))
						optionsDescription += option.name;
					else
						optionsDescription += ", " + option.name;
				}
			}
		}
	}
	
	/*
	 * Set the price of the option groups of a cocktail to zero except for first option
	 */
	public void adjustCocktailPrices() {

		price = 0;
		
		if (optionGroups == null)
			return;
		
		for (int i = 0 ; i < optionGroups.size() ; i++) {
			
			OptionGroup options = optionGroups.get(i);
			
			for (Option option : options.options) {
				
				if (i == 0) {
					// The first option group of a cocktail sets the price of the item so don't zero it out
				} else {
					// The price of other options should be zero
					option.price = 0;
				}
			}
		}
	}


}
