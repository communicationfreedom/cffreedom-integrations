package com.cffreedom.integrations.twilio;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.TwilioRestResponse;
import com.twilio.sdk.resource.factory.CallFactory;
import com.twilio.sdk.resource.factory.SmsFactory;
import com.twilio.sdk.resource.instance.Account;
import com.twilio.sdk.resource.instance.AvailablePhoneNumber;
import com.twilio.sdk.resource.instance.Call;
import com.twilio.sdk.resource.instance.Conference;
import com.twilio.sdk.resource.instance.Participant;
import com.twilio.sdk.resource.instance.Sms;
import com.twilio.sdk.resource.list.AccountList;
import com.twilio.sdk.resource.list.AvailablePhoneNumberList;
import com.twilio.sdk.resource.list.ParticipantList;
import com.twilio.sdk.verbs.Dial;
import com.twilio.sdk.verbs.Gather;
import com.twilio.sdk.verbs.Hangup;
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
 */
public class CFTwilio
{
	private static final Logger logger = LoggerFactory.getLogger("com.cffreedom.integrations.twilio.CFTwilio");
	private String accountSID = null;
	private String authToken = null;
	private TwilioRestClient restClient = null;
	private Account account = null;

	public CFTwilio(String accountSID, String authToken)
	{
		logger.debug("Initializing");
		this.accountSID = accountSID;
		this.authToken = authToken;
		this.restClient = new TwilioRestClient(this.accountSID, this.authToken);

		// Get the main account (The one we used to authenticate the client)
		this.account = this.restClient.getAccount();
	}

	private Account getAccount()
	{
		return this.account;
	}

	public String makeCall(String systemNumber, String to, String onceConnectedUrl) throws TwilioRestException
	{
		logger.debug("{}/{}/{}", systemNumber, to, onceConnectedUrl);
		
		final CallFactory callFactory = this.getAccount().getCallFactory();
		final Map<String, String> callParams = new HashMap<String, String>();
		callParams.put("To", to);
		callParams.put("From", systemNumber);
		callParams.put("Url", onceConnectedUrl);
		final Call call = callFactory.create(callParams);
		return call.getSid();
	}

	public String sendSms(String systemNumber, String to, String msg) throws TwilioRestException
	{
		logger.debug("{}/{}/{}", systemNumber, to, msg);
		
		final SmsFactory smsFactory = this.getAccount().getSmsFactory();
		final Map<String, String> smsParams = new HashMap<String, String>();
		smsParams.put("To", to); // Replace with a valid phone number
		smsParams.put("From", systemNumber); // Replace with a valid phone
												// number in your account
		smsParams.put("Body", msg);
		final Sms sms = smsFactory.create(smsParams);
		return sms.getSid();
	}

	public String twimlGetInput(String prompt, int digits, int timeout, String afterInputUrl) throws TwiMLException
	{
		logger.debug("{}/{}/{}/{}", prompt, digits, timeout, afterInputUrl);
		
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

	public String twimlDial(String number) throws TwiMLException
	{
		logger.debug("{}", number);
		
		TwiMLResponse resp = new TwiMLResponse();
		Dial dial = new Dial(number);
		resp.append(dial);
		return getFullXmlTwiML(resp.toXML());
	}

	private String getFullXmlTwiML(String xml)
	{
		String fullXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		fullXml += "\n" + xml;
		return fullXml;
	}
}
