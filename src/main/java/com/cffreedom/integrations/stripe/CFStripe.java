package com.cffreedom.integrations.stripe;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cffreedom.utils.Convert;
import com.cffreedom.utils.DateTimeUtils;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Customer;
import com.stripe.model.Plan;
import com.stripe.model.Subscription;
import com.stripe.model.Token;

/**
 * Class to make working with the Stripe API even easier
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
 */
public class CFStripe
{
	private static final Logger logger = LoggerFactory.getLogger("com.cffreedom.integrations.stripe.CFStripe");
	public final static String TEST_CC_NUMBER = "4242424242424242";
	public final static String TEST_CC_DECLINE = "4000000000000002";
	
	private String apiKey = null;
	
	public CFStripe(String apiKey)
	{
		this.apiKey = apiKey;
	}

	private String getApiKey()
	{
		return this.apiKey;
	}

	public Plan createPlan(String planCode, String name, int amountInCents) throws StripeException
	{
		Stripe.apiKey = this.getApiKey();
		Map<String, Object> planParams = new HashMap<String, Object>();
		planParams.put("interval", "month");
		planParams.put("id", planCode);
		planParams.put("currency", "usd");
		planParams.put("name", name);
		planParams.put("amount", amountInCents);
		return Plan.create(planParams);
	}

	public void deletePlan(String planCode) throws StripeException
	{
		Stripe.apiKey = this.getApiKey();
		Plan plan = Plan.retrieve(planCode);
		plan.delete();
	}

	public Token createCardToken(String cardholderName, String cardNum, String secCode, int expMonth, int expYear) throws StripeException
	{
		return createCardToken(this.getApiKey(), cardholderName, cardNum, secCode, expMonth, expYear);
	}
	
	public static Token createCardToken(String apiKey, String cardholderName, String cardNum, String secCode, int expMonth, int expYear) throws StripeException
	{
		Stripe.apiKey = apiKey;
		Map<String, Object> tokenParams = new HashMap<String, Object>();
		Map<String, Object> cardParams = new HashMap<String, Object>();
		cardParams.put("name", cardholderName);
		cardParams.put("number", cardNum);
		cardParams.put("cvc", secCode);
		cardParams.put("exp_month", expMonth);
		cardParams.put("exp_year", expYear);
		tokenParams.put("card", cardParams);
		return Token.create(tokenParams);
	}
	
	public Customer order(Token cardToken, String email, String desc, String planCode) throws StripeException
	{
		return order(cardToken, email, desc, planCode, new Date());
	}

	public Customer order(Token cardToken, String email, String desc, String planCode, Date planStartDate) throws StripeException
	{
		Date today = Convert.toDateNoTime(new Date());
		planStartDate = Convert.toDateNoTime(planStartDate);
		
		Stripe.apiKey = this.getApiKey();
		Map<String, Object> customerParams = new HashMap<String, Object>();
		customerParams.put("card", cardToken.getId());
		customerParams.put("email", email);
		customerParams.put("plan", planCode);
		customerParams.put("description", desc);
		if (planStartDate.after(today) == true)
		{
			customerParams.put("trial_end", DateTimeUtils.dateAsEpoc(planStartDate));
		}
		return Customer.create(customerParams);
	}

	public Charge orderOneTime(Token cardToken, String email, String desc, int amountInCents) throws StripeException
	{
		Stripe.apiKey = this.getApiKey();
		
		logger.info("Creating customer: {}", email);
		Map<String, Object> customerParams = new HashMap<String, Object>();
		customerParams.put("card", cardToken.getId());
		customerParams.put("email", email);
		Customer cust = Customer.create(customerParams);
		
		logger.info("Creating charge: {} ({})", amountInCents, desc);
		Map<String, Object> chargeParams = new HashMap<String, Object>();
		chargeParams.put("amount", amountInCents);
		chargeParams.put("description", desc);
		chargeParams.put("currency", "usd");
		chargeParams.put("customer", cust.getId());
		return Charge.create(chargeParams);
	}
	
	public Subscription updateOrder(String custCode, String newPlanCode) throws StripeException
	{
		Stripe.apiKey = this.getApiKey();
		Customer c = Customer.retrieve(custCode);
		Map<String, Object> subscriptionParams = new HashMap<String, Object>();
		subscriptionParams.put("plan", newPlanCode);
		subscriptionParams.put("prorate", "true");
		return c.updateSubscription(subscriptionParams);
	}
	
	public Subscription updateCreditCard(Token cardToken, String custCode, String planCode) throws StripeException
	{
		return updateCreditCard(this.getApiKey(), cardToken, custCode, planCode);
	}
	
	public static Subscription updateCreditCard(String apiKey, Token cardToken, String custCode, String planCode) throws StripeException
	{
		Stripe.apiKey = apiKey;
		Customer c = Customer.retrieve(custCode);
		Map<String, Object> subscriptionParams = new HashMap<String, Object>();
		subscriptionParams.put("plan", planCode);
		subscriptionParams.put("prorate", "true");
		subscriptionParams.put("card", cardToken.getId());
		return c.updateSubscription(subscriptionParams);
	}
	
	public Subscription cancelOrder(String custCode) throws StripeException
	{
		Stripe.apiKey = this.getApiKey();
		Customer cu = this.getCustomer(custCode);
		return cu.cancelSubscription();
	}

	public Customer getCustomer(String id) throws StripeException
	{
		return Customer.retrieve(id);
	}
	
	public static String test()
	{
		return "Hi from static test()";
	}
}
