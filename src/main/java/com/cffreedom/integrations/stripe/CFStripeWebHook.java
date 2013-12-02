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
		return JsonUtils.getJsonObjectStringVal(webHook, "id");
	}
	
	/**
	 * Get the "type"
	 * @param webHook
	 * @return Value of "type"
	 */
	public static String getType(JSONObject webHook)
	{
		return JsonUtils.getJsonObjectStringVal(webHook, "type");
	}

	/**
	 * Get "livemode" as boolean
	 * @param webHook
	 * @return Value of "livemode"
	 */
	public static boolean getIsLive(JSONObject webHook)
	{
		return JsonUtils.getJsonObjectBooleanVal(webHook, "livemode");
	}
	
	/**
	 * Get the "created" date in GMT. Hint: Use DateTimeUtils.gmtToLocal() if desired
	 * @param webHook
	 * @return Created date in GMT
	 */
	public static Date getCreated(JSONObject webHook)
	{
		return Convert.toDate(JsonUtils.getJsonObjectLongVal(webHook, "created")*1000);
	}
	
	public static Payment getPayment(JSONObject webHook)
	{
		Payment payment = new Payment();
		
		JSONObject data = JsonUtils.getJsonObject(webHook, "data");
		JSONObject pmt = JsonUtils.getJsonObject(data, "object");
		payment.setPaymentDate(Convert.toDate(JsonUtils.getJsonObjectLongVal(pmt, "created")*1000));
		payment.setPaymentCode(JsonUtils.getJsonObjectStringVal(pmt, "id"));
		payment.setCustomerCode(JsonUtils.getJsonObjectStringVal(pmt, "customer"));
		payment.setMemo(JsonUtils.getJsonObjectStringVal(pmt, "description"));
		payment.setCurrency(JsonUtils.getJsonObjectStringVal(pmt, "currency"));
		payment.setGross(Convert.toBigDecimalFromCents(JsonUtils.getJsonObjectLongVal(pmt, "amount")));
		payment.setFees(Convert.toBigDecimalFromCents(JsonUtils.getJsonObjectLongVal(pmt, "fee")));
		payment.setPaid(JsonUtils.getJsonObjectBooleanVal(pmt, "paid"));
		payment.setRefunded(JsonUtils.getJsonObjectBooleanVal(pmt, "refunded"));
		
		return payment;
	}
}
