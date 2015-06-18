package com.cffreedom.integrations.gmaps;

import com.cffreedom.beans.Address;
import com.cffreedom.exceptions.InfrastructureException;
import com.cffreedom.utils.Cacher;
import com.cffreedom.utils.Utils;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;

/**
 * Class to make working with the Google Maps API easier
 * 
 * @author markjacobsen.net (http://markjacobsen.net/code)
 * Copyright: Communication Freedom, LLC - http://www.communicationfreedom.com
 * 
 * Free to use, modify, redistribute.  Must keep full class header including 
 * copyright and note your modifications.
 * 
 * If this helped you out or saved you time, please consider...
 * 1) Donating: http://www.communicationfreedom.com/go/donate/
 * 2) Shoutout on twitter: @MarkJacobsen or @cffreedom
 * 3) Linking to: http://markjacobsen.net
 * 
 * Reference:
 * https://github.com/googlemaps/google-maps-services-java
 * https://developers.google.com/maps/documentation/geocoding/
 * 
 * Changes:
 * 2015-06-17 	markjacobsen.net 	Created
 */
public class CFGMaps 
{
	private Cacher cacher = new Cacher(60*4);
	private Cacher cacherLatLng = new Cacher(60*4);
	private GeoApiContext context = null;
	
	public CFGMaps(String apiKey)
	{
		this.context = new GeoApiContext().setApiKey(apiKey);
	}
	
	private Address getAddress(String address, String city, String state, int zip)
	{
		return new Address(address, city, state, zip+"");
	}
	
	private GeocodingResult[] getGeocodingResults(Address address) throws InfrastructureException
	{
		try
		{
			String key = address.toString();
			if (this.cacher.containsKey(key) == false) {
				this.cacher.put(key, GeocodingApi.geocode(this.context, key).await());
			}
			return this.cacher.get(key);
		} catch (Exception e) {
			throw new InfrastructureException("Error", e);
		}
	}
	
	private GeocodingResult[] getGeocodingResults(double latitude, double longitude) throws InfrastructureException
	{
		try
		{
			String key = latitude + "/" + longitude;
			LatLng latLng = new LatLng(longitude, longitude);
			if (this.cacherLatLng.containsKey(key) == false) {
				this.cacherLatLng.put(key, GeocodingApi.reverseGeocode(this.context, latLng).await());
			}
			return this.cacherLatLng.get(key);
		} catch (Exception e) {
			throw new InfrastructureException("Error", e);
		}
	}
	
	/**
	 * Get an address for a given lat/lng
	 * @param latitude
	 * @param longitude
	 * @return
	 * @throws InfrastructureException
	 */
	public String getFormattedAddress(double latitude, double longitude) throws InfrastructureException
	{
		GeocodingResult[] results = getGeocodingResults(latitude, longitude);
		return results[0].formattedAddress;
	}
	
	/**
	 * Get the latitude for a given address
	 * @param address
	 * @param city
	 * @param state
	 * @param zip
	 * @return
	 * @throws InfrastructureException
	 */
	public double getLatitude(String address, String city, String state, int zip) throws InfrastructureException
	{
		Address addr = getAddress(address, city, state, zip);
		return getLatitude(addr);
	}
	
	public double getLatitude(Address address) throws InfrastructureException
	{
		GeocodingResult[] results = getGeocodingResults(address);
		return results[0].geometry.location.lat;
	}
	
	/**
	 * Get the longitude for a given address
	 * @param address
	 * @param city
	 * @param state
	 * @param zip
	 * @return
	 * @throws InfrastructureException
	 */
	public double getLongitude(String address, String city, String state, int zip) throws InfrastructureException
	{
		Address addr = getAddress(address, city, state, zip);
		return getLongitude(addr);
	}
	
	public double getLongitude(Address address) throws InfrastructureException
	{
		GeocodingResult[] results =  getGeocodingResults(address);
		return results[0].geometry.location.lng;
	}
	
	/**
	 * Exists just for quick testing
	 * @param args
	 * @throws InfrastructureException
	 */
	public static void main(String[] args) throws InfrastructureException
	{
		CFGMaps gmaps = new CFGMaps("yourKey");
		double lng = gmaps.getLongitude("123 Main Street", "Holt", "MI", 48842);
		double lat = gmaps.getLatitude("123 Main Street", "Holt", "MI", 48842);
		Utils.output(lng + " / " + lat);
	}
}
