package com.cffreedom.integrations.stripe;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cffreedom.beans.CreditCard;
import com.cffreedom.exceptions.DoesNotExistException;
import com.cffreedom.utils.Convert;
import com.cffreedom.utils.DateTimeUtils;
import com.stripe.Stripe;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.Card;
import com.stripe.model.Charge;
import com.stripe.model.Customer;
import com.stripe.model.CustomerCardCollection;
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
 * 
 * Changes:
 * 2013-10-05 	markjacobsen.net 	Added chargeCustomer()
 * 2013-10-07   markjacobsen.net 	Renamed createCardToken to getCreditCardToken()
 * 2013-10-07   markjacobsen.net 	Renamed createPlan() to addPlan()
 */
public class CFStripe
{
	private static final Logger logger = LoggerFactory.getLogger(CFStripe.class);
	public final static String TEST_CC_NUMBER = "4242424242424242";
	public final static String TEST_CC_DECLINE = "4000000000000002";
	public final static String INTERVAL_MONTHLY = "month";
	public final static String CURRENCY_USD = "usd";
	
	//private String apiKey = null;
	
	public CFStripe(String apiKey)
	{
		logger.debug("Initializing");
		//this.apiKey = apiKey;
		Stripe.apiKey = apiKey;
		logger.debug("Initialized");
	}

	/*
	private String getApiKey()
	{
		return this.apiKey;
	}
	*/
	
	/**
	 * @return Stripe.apiVersion
	 */
	public String getVersion()
	{
		return Stripe.apiVersion;
	}
	
	public Plan getPlan(String planCode) throws DoesNotExistException
	{
		try
		{
			//Stripe.apiKey = this.getApiKey();
			return Plan.retrieve(planCode);
		}
		catch (Exception e)
		{
			throw new DoesNotExistException("PlanCode "+planCode+" does not exist");
		}
	}

	/**
	 * Create a new monthly plan billed in USD
	 * @param planCode
	 * @param name
	 * @param amountInCents
	 * @return The new Plan object
	 * @throws StripeException
	 */
	public Plan addPlan(String planCode, String name, int amountInCents) throws StripeException
	{
		return addPlan(planCode, name, amountInCents, CFStripe.INTERVAL_MONTHLY, CFStripe.CURRENCY_USD);
	}
	
	/**
	 * Create a new plan with the specified information
	 * @param planCode
	 * @param name
	 * @param amountInCents
	 * @param interval
	 * @param currency
	 * @return The new Plan object
	 * @throws StripeException
	 */
	public Plan addPlan(String planCode, String name, int amountInCents, String interval, String currency) throws StripeException
	{
		logger.info("Adding plan {} named {}", planCode, name);
		//Stripe.apiKey = this.getApiKey();
		Map<String, Object> planParams = new HashMap<String, Object>();
		planParams.put("interval", interval);
		planParams.put("id", planCode);
		planParams.put("currency", currency);
		planParams.put("name", name);
		planParams.put("amount", amountInCents);
		return Plan.create(planParams);
	}

	public void deletePlan(String planCode) throws StripeException
	{
		logger.warn("Deleting plan: {}", planCode);
		//Stripe.apiKey = this.getApiKey();
		Plan plan = Plan.retrieve(planCode);
		plan.delete();
	}

	/**
	 * Create a token for a credit card
	 * @param cardholderName
	 * @param cardNum
	 * @param secCode
	 * @param expMonth
	 * @param expYear
	 * @return Token object representing the credit card
	 * @throws StripeException
	 */
	public Token getCreditCardToken(String cardholderName, String cardNum, String secCode, int expMonth, int expYear) throws StripeException
	{
		logger.info("Getting CC token for {} with {}/{} exp", cardholderName, expMonth, expYear);
		//Stripe.apiKey = this.getApiKey();
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
	
	/**
	 * Same as other getCreditCardToken, just taking a CreditCard object from cffreedom-beans
	 * @param cc
	 * @return
	 * @throws StripeException
	 */
	public Token getCreditCardToken(CreditCard cc) throws StripeException
	{
		return this.getCreditCardToken(cc.getFullName(), cc.getCardNum(), cc.getSecNum(), cc.getExpMonth(), cc.getExpYear());
	}
	
	public Customer order(Token cardToken, String email, String desc, String planCode) throws StripeException
	{
		return order(cardToken, email, desc, planCode, new Date());
	}

	/**
	 * Create a new customer and and add them to a predefined plan
	 * @param cardToken
	 * @param email
	 * @param desc
	 * @param planCode
	 * @param planStartDate
	 * @return
	 * @throws StripeException
	 */
	public Customer order(Token cardToken, String email, String desc, String planCode, Date planStartDate) throws StripeException
	{
		logger.info("Ordering plan {} for {}", planCode, email);
		Date today = Convert.toDateNoTime(new Date());
		planStartDate = Convert.toDateNoTime(planStartDate);
		
		//Stripe.apiKey = this.getApiKey();
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

	/**
	 * Create a one time order for a specific amount based on credit card info
	 * @param cardToken
	 * @param email
	 * @param desc
	 * @param amountInCents
	 * @return
	 * @throws StripeException
	 */
	public Charge orderOneTime(Token cardToken, String email, String desc, int amountInCents) throws StripeException
	{
		logger.info("Creating one time charge in the amount of {} cents for {}", amountInCents, email);
		//Stripe.apiKey = this.getApiKey();
		
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
	
	/**
	 * Create a one time charge for a specific amount for a specific customer
	 * @param custCode
	 * @param desc
	 * @param amountInCents
	 * @return
	 * @throws StripeException
	 */
	public Charge chargeCustomer(String custCode, String desc, int amountInCents) throws StripeException
	{
		logger.info("Creating one time charge in the amount of {} cents for {}: {}", amountInCents, custCode, desc);
		//Stripe.apiKey = this.getApiKey();
		Customer cust = getCustomer(custCode);
		Map<String, Object> chargeParams = new HashMap<String, Object>();
		chargeParams.put("amount", amountInCents);
		chargeParams.put("description", desc);
		chargeParams.put("currency", "usd");
		chargeParams.put("customer", cust.getId());
		return Charge.create(chargeParams);
	}
	
	/**
	 * Change an existing order to a new/different plan. Will automatically prorate the change.
	 * @param custCode
	 * @param newPlanCode
	 * @return Updated Subscription object
	 * @throws StripeException
	 */
	public Subscription updateOrderPlan(String custCode, String newPlanCode) throws StripeException
	{
		logger.info("Updating order plan to {} for cust {}", newPlanCode, custCode);
		//Stripe.apiKey = this.getApiKey();
		Customer c = Customer.retrieve(custCode);
		Map<String, Object> subscriptionParams = new HashMap<String, Object>();
		subscriptionParams.put("plan", newPlanCode);
		subscriptionParams.put("prorate", "true");
		return c.updateSubscription(subscriptionParams);
	}
	
	/**
	 * Get a specific credit card for a specific customer
	 * @param custCode
	 * @param cardCode
	 * @return Card for the passed information
	 * @throws StripeException
	 */
	public Card getCustomerCard(String custCode, String cardCode) throws StripeException
	{
		logger.debug("Getting cust {} card {}", custCode, cardCode);
		Customer cu = this.getCustomer(custCode);
		return cu.getCards().retrieve(cardCode);
	}
	
	/**
	 * Add a credit card to an existing customer
	 * @param cardToken
	 * @param custCode
	 * @param makeDefault
	 * @throws StripeException
	 */
	public void addCreditCardToCust(Token cardToken, String custCode, boolean makeDefault) throws StripeException
	{
		logger.info("Adding card {} to cust {}", cardToken.getId(), custCode);
		//Stripe.apiKey = this.getApiKey();
		Customer cu = this.getCustomer(custCode); //Customer.retrieve(custCode);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("card", cardToken.getId());
		logger.debug("Creating CC");
		Card card = cu.createCard(params);
		if (makeDefault == true)
		{
			logger.debug("Making new CC the default");
			params = new HashMap<String, Object>();
			params.put("default_card", card.getId());
			cu.update(params);
		}
		logger.info("Added CC to cust {}", custCode);
	}
	
	/**
	 * Remove an existing card on a customer
	 * @param cardCode
	 * @param custCode
	 * @return True if successful, false otherwise
	 * @throws StripeException
	 */
	public boolean deleteCreditCardForCust(String cardCode, String custCode) throws StripeException
	{
		logger.info("Deleting CC {} for cust {}", cardCode, custCode);
		//Stripe.apiKey = this.getApiKey();
		Customer cu = Customer.retrieve(custCode);
		for(Card card : cu.getCards().getData())
		{
			if(card.getId().equals(cardCode))
			{
				card.delete();
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Update the credit card used to bill a specific order for a specific customer
	 * @param cardToken
	 * @param custCode
	 * @param planCode
	 * @return
	 * @throws StripeException
	 */
	public Subscription updateOrderCreditCard(Token cardToken, String custCode, String planCode) throws StripeException
	{
		logger.info("Updating order CC for plan {}, cust {} to {}", planCode, custCode, cardToken.getId());
		//Stripe.apiKey = this.getApiKey();
		Customer c = Customer.retrieve(custCode);
		Map<String, Object> subscriptionParams = new HashMap<String, Object>();
		subscriptionParams.put("plan", planCode);
		subscriptionParams.put("prorate", "true");
		subscriptionParams.put("card", cardToken.getId());
		return c.updateSubscription(subscriptionParams);
	}
	
	public Subscription cancelOrder(String custCode) throws StripeException
	{
		logger.info("Canceling order for {}", custCode);
		//Stripe.apiKey = this.getApiKey();
		Customer cu = this.getCustomer(custCode);
		return cu.cancelSubscription();
	}

	public Customer getCustomer(String custCode) throws StripeException
	{
		logger.debug("Getting cust: {}", custCode);
		return Customer.retrieve(custCode);
	}
	
	public List<Card> getCustomerCards(String custCode) throws StripeException
	{
		logger.debug("Getting cust cards: {}", custCode);
		Customer inv = Customer.retrieve(custCode);
		Map<String, Object> cardParams = new HashMap<String, Object>();
		cardParams.put("count", 10);
		CustomerCardCollection cards = inv.getCards().all(cardParams);
		return cards.getData();
	}
}
