package com.cffreedom.integrations.twilio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cffreedom.beans.PhoneNumber;
import com.cffreedom.exceptions.InfrastructureException;
import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.CallFactory;
import com.twilio.sdk.resource.factory.MessageFactory;
import com.twilio.sdk.resource.factory.SmsFactory;
import com.twilio.sdk.resource.instance.Account;
import com.twilio.sdk.resource.instance.AvailablePhoneNumber;
import com.twilio.sdk.resource.instance.Call;
import com.twilio.sdk.resource.instance.Message;
import com.twilio.sdk.resource.instance.Sms;
import com.twilio.sdk.resource.list.AvailablePhoneNumberList;
import com.twilio.sdk.verbs.Dial;
import com.twilio.sdk.verbs.Gather;
import com.twilio.sdk.verbs.Hangup;
import com.twilio.sdk.verbs.Reject;
import com.twilio.sdk.verbs.TwiMLException;
import com.twilio.sdk.verbs.TwiMLResponse;
import com.twilio.sdk.verbs.Say;

/**
 * Class to make working with the Twilio API easier
 * 
 * @author markjacobsen.net (http://mjg2.net/code)
 * Copyright: Communication Freedom, LLC - http://www.communicationfreedom.com
 * 
 * Free to use, modify, redistribute.  Must keep full class header including 
 * copyright and note your modifications.
 * 
 * If this helped you out or saved you time, please consider...
 * 1) Donating: http://www.communicationfreedom.com/go/donate/
 * 2) Shoutout on twitter: @MarkJacobsen or @cffreedom
 * 3) Linking to: http://visit.markjacobsen.net
 * 
 * Reference:
 * https://github.com/twilio/twilio-java
 * http://twilio.github.com/twilio-java/
 * 
 * Changes:
 * 2013-09-30 	markjacobsen.net 	Added sendMms(), twimlRejectCall(), and twimlSayAndHangUp()
 * 2013-10-28 	MarkJacobsen.net	Added getAvailableTollFreeNumbers(), getAvailableLocalNumbers(), and getAvailableNumbers()
 */
public class CFTwilio
{
	private static final Logger logger = LoggerFactory.getLogger(CFTwilio.class);
	private String accountSID = null;
	private String authToken = null;
	private TwilioRestClient restClient = null;
	private Account account = null;

	public CFTwilio(String accountSID, String authToken)
	{
		logger.debug("Initializing account: {}", accountSID);
		this.accountSID = accountSID;
		this.authToken = authToken;
		logger.trace("Getting TwilioRestClient");
		this.restClient = new TwilioRestClient(this.accountSID, this.authToken);

		// Get the main account (The one we used to authenticate the client)
		logger.trace("Getting Account from TwilioRestClient");
		this.account = this.restClient.getAccount();
		logger.debug("Initialized");
	}

	private Account getAccount()
	{
		return this.account;
	}
	
	public ArrayList<PhoneNumber> getAvailableTollFreeNumbers(String contains)
	{
		return this.getAvailableNumbers("TollFree", "US", contains);
	}
	
	public ArrayList<PhoneNumber> getAvailableLocalNumbers(String contains)
	{
		return this.getAvailableNumbers("Local", "US", contains);
	}
	
	/**
	 * Return potential numbers for the criteria passed in
	 * @param type One of "Local" or "TollFree"
	 * @param isoCountry Option like "US", "GB", etc
	 * @param contains Send null or ZLS for random numbers. No need to use wildcards
	 * @return
	 */
	public ArrayList<PhoneNumber> getAvailableNumbers(String type, String isoCountry, String contains)
	{
		ArrayList<PhoneNumber> results = new ArrayList<PhoneNumber>();
		HashMap<String, String> params = new HashMap<String, String>();
		
		if ((contains != null) && (contains.length() > 0))
		{
			logger.debug("Searching for numbers containing: {}", contains);
			params.put("Contains", contains);
		}
		
		AvailablePhoneNumberList numbers = this.getAccount().getAvailablePhoneNumbers(params, isoCountry, type);
		
		for (AvailablePhoneNumber num : numbers.getPageData())
		{
			PhoneNumber number = new PhoneNumber();
			number.setDisplay(num.getFriendlyName());
			number.setCode(num.getPhoneNumber());
			number.setIsoCountry(num.getIsoCountry());
			results.add(number);
		}
		
		return results;
	}

	/**
	 * Make an outbound call
	 * @param systemNumber
	 * @param to
	 * @param afterConnectedUrl URL Twilio should call after the call is connected
	 * @return Call SID
	 * @throws InfrastructureException
	 */
	public String makeCall(String systemNumber, String to, String afterConnectedUrl) throws InfrastructureException
	{
		logger.debug("{}/{}/{}", systemNumber, to, afterConnectedUrl);
		
		try
		{
			final CallFactory callFactory = this.getAccount().getCallFactory();
			final Map<String, String> callParams = new HashMap<String, String>();
			callParams.put("To", to);
			callParams.put("From", systemNumber);
			callParams.put("Url", afterConnectedUrl);
			final Call call = callFactory.create(callParams);
			return call.getSid();
		}
		catch (TwilioRestException e)
		{
			throw new InfrastructureException("Error making call: " + e.getMessage(), e);
		}
	}
	
	public String twimlRejectCall() throws InfrastructureException
	{
		logger.debug("Rejecting call");
		
		try
		{
			TwiMLResponse resp = new TwiMLResponse();
			Reject reject = new Reject();
	
			resp.append(reject);
			return getFullXmlTwiML(resp.toXML());
		}
		catch (TwiMLException e)
		{
			throw new InfrastructureException("Error: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Say something and then hang up / disconnect the call
	 * @param msg
	 * @return TWIML XML
	 * @throws InfrastructureException
	 */
	public String twimlSayAndHangUp(String msg) throws InfrastructureException
	{		
		try
		{
			TwiMLResponse resp = new TwiMLResponse();
			Say say = new Say(msg);
			resp.append(say);
			resp.append(new Hangup());
			return getFullXmlTwiML(resp.toXML());
		}
		catch (TwiMLException e)
		{
			throw new InfrastructureException("Error: " + e.getMessage(), e);
		}
	}

	/**
	 * Send a SMS message
	 * @param systemNumber
	 * @param to
	 * @param msg
	 * @return SMS SID
	 * @throws InfrastructureException
	 */
	public String sendSms(String systemNumber, String to, String msg) throws InfrastructureException
	{
		logger.debug("Sending SMS: {}/{}/{}", systemNumber, to, msg);
		
		try
		{
			final SmsFactory smsFactory = this.getAccount().getSmsFactory();
			final Map<String, String> smsParams = new HashMap<String, String>();
			smsParams.put("To", to); // Replace with a valid phone number
			smsParams.put("From", systemNumber); // Replace with a valid phone
													// number in your account
			smsParams.put("Body", msg);
			final Sms sms = smsFactory.create(smsParams);
			logger.debug("Sent SMS: {}", sms.getSid());
			return sms.getSid();
		}
		catch (TwilioRestException e)
		{
			throw new InfrastructureException("Error sending SMS: " + e.getMessage(), e);
		}
	}

	/**
	 * Send a MMS message
	 * @param systemNumber
	 * @param to
	 * @param msg
	 * @imageUrl
	 * @return MMS SID
	 * @throws InfrastructureException
	 */
	public String sendMms(String systemNumber, String to, String msg, String imageUrl) throws InfrastructureException
	{
		logger.debug("Sending MMS: {}/{}/{}/{}", systemNumber, to, msg, imageUrl);
		
		try
		{
			final MessageFactory msgFactory = this.getAccount().getMessageFactory();
			final List<NameValuePair> msgParams = new ArrayList<NameValuePair>();
			msgParams.add(new BasicNameValuePair("To", to)); // Replace with a valid phone number
			msgParams.add(new BasicNameValuePair("From", systemNumber)); // Replace with a valid phone number in your account
			msgParams.add(new BasicNameValuePair("Body", msg));
			msgParams.add(new BasicNameValuePair("MediaUrl", imageUrl));
			final Message message = msgFactory.create(msgParams);
			logger.debug("Sent MMS: {}", message.getSid());
			return message.getSid();
		}
		catch (TwilioRestException e)
		{
			throw new InfrastructureException("Error sending MMS: " + e.getMessage(), e);
		}
	}

	/**
	 * 
	 * @param prompt What to say to the caller
	 * @param digits Number of digits we're expecting the caller to enter
	 * @param timeout How long to wait in seconds
	 * @param afterInputUrl URL Twilio should call after getting input
	 * @return  TWIML XML
	 * @throws InfrastructureException
	 */
	public String twimlGetInput(String prompt, int digits, int timeout, String afterInputUrl) throws InfrastructureException
	{
		logger.debug("{}/{}/{}/{}", prompt, digits, timeout, afterInputUrl);
		
		try
		{
			// http://www.twilio.com/docs/quickstart/java/twiml/record-caller-leave-message
			TwiMLResponse resp = new TwiMLResponse();
			Say say = new Say(prompt);
	
			Gather gather = new Gather();
			gather.setAction(afterInputUrl);
			gather.setNumDigits(digits);
			gather.setMethod("GET");
			gather.setTimeout(timeout);
			gather.append(say);
	
			resp.append(gather);
			// If we get past the gather the request timed out
			resp.append(new Hangup());
			return getFullXmlTwiML(resp.toXML());
		}
		catch (TwiMLException e)
		{
			throw new InfrastructureException("Error in twimlGetInput: " + e.getMessage(), e);
		}
	}

	public String twimlDial(String number) throws InfrastructureException
	{
		logger.debug("{}", number);
		
		try
		{
			TwiMLResponse resp = new TwiMLResponse();
			Dial dial = new Dial(number);
			resp.append(dial);
			return getFullXmlTwiML(resp.toXML());
		}
		catch (TwiMLException e)
		{
			throw new InfrastructureException("Error in twimlDial: " + e.getMessage(), e);
		}
	}

	private String getFullXmlTwiML(String xml)
	{
		String fullXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		fullXml += "\n" + xml;
		return fullXml;
	}
}
