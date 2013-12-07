package com.cffreedom.integrations.stripe;

import java.util.Date;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.cffreedom.beans.Payment;
import com.cffreedom.utils.Convert;
import com.cffreedom.utils.JsonUtils;

public class CFStripeWebHook
{
	/**
	 * Return a JSONObject containing the web hook (to be used in subsequent calls)
	 * @param jsonText
	 * @return JSONObject containing web hook
	 * @throws ParseException
	 */
	public static JSONObject getWebHook(String jsonText) throws ParseException
	{
		return JsonUtils.getJsonObject(jsonText);
	}
	
	/**
	 * Get the "id"
	 * @param webHook
	 * @return Value of "id"
	 */
	public static String getId(JSONObject webHook)
	{
		return JsonUtils.getString(webHook, "id");
	}
	
	/**
	 * Get the "type"
	 * @param webHook
	 * @return Value of "type"
	 */
	public static String getType(JSONObject webHook)
	{
		return JsonUtils.getString(webHook, "type");
	}

	/**
	 * Get "livemode" as boolean
	 * @param webHook
	 * @return Value of "livemode"
	 */
	public static boolean getIsLive(JSONObject webHook)
	{
		return JsonUtils.getBoolean(webHook, "livemode");
	}
	
	/**
	 * Get the "created" date in GMT. Hint: Use DateTimeUtils.gmtToLocal() if desired
	 * @param webHook
	 * @return Created date in GMT
	 */
	public static Date getCreated(JSONObject webHook)
	{
		return Convert.toDate(JsonUtils.getLong(webHook, "created")*1000);
	}
	
	public static Payment getPayment(JSONObject webHook)
	{
		Payment payment = new Payment();
		
		JSONObject data = JsonUtils.getJsonObject(webHook, "data");
		JSONObject pmt = JsonUtils.getJsonObject(data, "object");
		payment.setPaymentDate(Convert.toDate(JsonUtils.getLong(pmt, "created")*1000));
		payment.setPaymentCode(JsonUtils.getString(pmt, "id"));
		payment.setCustomerCode(JsonUtils.getString(pmt, "customer"));
		payment.setMemo(JsonUtils.getString(pmt, "description"));
		payment.setCurrency(JsonUtils.getString(pmt, "currency"));
		payment.setGross(Convert.toBigDecimalFromCents(JsonUtils.getLong(pmt, "amount")));
		payment.setFees(Convert.toBigDecimalFromCents(JsonUtils.getLong(pmt, "fee")));
		payment.setPaid(JsonUtils.getBoolean(pmt, "paid"));
		payment.setRefunded(JsonUtils.getBoolean(pmt, "refunded"));
		
		return payment;
	}
}
