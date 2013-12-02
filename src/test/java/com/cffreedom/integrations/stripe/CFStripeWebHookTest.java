package com.cffreedom.integrations.stripe;

import static org.junit.Assert.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import com.cffreedom.beans.Payment;
import com.cffreedom.utils.Convert;

public class CFStripeWebHookTest
{
	private static String chargeSucceeded = "{" +
			"  \"id\": \"evt_asdfwe34SDkl43\",                      " +
			"  \"created\": 1385705053,                             " +
			"  \"livemode\": true,                                  " +
			"  \"type\": \"charge.succeeded\",                      " +
			"  \"data\": {                                          " +
			"    \"object\": {                                      " +
			"      \"id\": \"ch_34sdrfljdslSDF\",                   " +
			"      \"object\": \"charge\",                          " +
			"      \"created\": 1385705052,                         " +
			"      \"livemode\": true,                              " +
			"      \"paid\": true,                                  " +
			"      \"amount\": 595,                                 " +
			"      \"currency\": \"usd\",                           " +
			"      \"refunded\": false,                             " +
			"      \"card\": {                                      " +
			"        \"id\": \"card_34JdsflkD434lk\",               " +
			"        \"object\": \"card\",                          " +
			"        \"last4\": \"1548\",                           " +
			"        \"type\": \"MasterCard\",                      " +
			"        \"exp_month\": 9,                              " +
			"        \"exp_year\": 2016,                            " +
			"        \"fingerprint\": \"7asdflkjDASDF988\",         " +
			"        \"customer\": null,                            " +
			"        \"country\": \"US\",                           " +
			"        \"name\": \"John Smith\",                      " +
			"        \"address_line1\": null,                       " +
			"        \"address_line2\": null,                       " +
			"        \"address_city\": null,                        " +
			"        \"address_state\": null,                       " +
			"        \"address_zip\": null,                         " +
			"        \"address_country\": null,                     " +
			"        \"cvc_check\": null,                           " +
			"        \"address_line1_check\": null,                 " +
			"        \"address_zip_check\": null                    " +
			"      },                                               " +
			"      \"captured\": true,                              " +
			"      \"balance_transaction\": \"txn_sak4980ASDFljl\", " +
			"      \"failure_message\": null,                       " +
			"      \"failure_code\": null,                          " +
			"      \"amount_refunded\": 0,                          " +
			"      \"customer\": \"cus_jasfklWESD345D\",            " +
			"      \"invoice\": \"in_jkasdf345Ua3SD\",              " +
			"      \"description\": null,                           " +
			"      \"dispute\": null,                               " +
			"      \"metadata\": {                                  " +
			"      },                                               " +
			"      \"fee\": 59,                                     " +
			"      \"fee_details\": [                               " +
			"        {                                              " +
			"          \"amount\": 59,                              " +
			"          \"currency\": \"usd\",                       " +
			"          \"type\": \"stripe_fee\",                    " +
			"          \"description\": \"Stripe processing fees\", " +
			"          \"application\": null,                       " +
			"          \"amount_refunded\": 0                       " +
			"        }                                              " +
			"      ]                                                " +
			"    }                                                  " +
			"  },                                                   " +
			"  \"object\": \"event\",                               " +
			"  \"pending_webhooks\": 1,                             " +
			"  \"request\": null                                    " +
			"}";
	
	private static String transferCreated = "{" +
			"  \"id\": \"evt_33xASHjHeSEfIw\",                                  " +
			"  \"created\": 1385948990,                                         " +
			"  \"livemode\": false,                                             " +
			"  \"type\": \"transfer.created\",                                  " +
			"  \"data\": {                                                      " +
			"    \"object\": {                                                  " +
			"      \"id\": \"tr_3arZEdFc4ELXQf\",                               " +
			"      \"object\": \"transfer\",                                    " +
			"      \"date\": 1386028800,                                        " +
			"      \"livemode\": false,                                         " +
			"      \"amount\": 936,                                             " +
			"      \"currency\": \"usd\",                                       " +
			"      \"status\": \"pending\",                                     " +
			"      \"balance_transaction\": \"txn_32rZeS5wJIgq7R\",             " +
			"      \"summary\": {                                               " +
			"        \"charge_gross\": 995,                                     " +
			"        \"charge_fees\": 59,                                       " +
			"        \"charge_fee_details\": [                                  " +
			"          {                                                        " +
			"            \"amount\": 59,                                        " +
			"            \"currency\": \"usd\",                                 " +
			"            \"type\": \"stripe_fee\",                              " +
			"            \"description\": null,                                 " +
			"            \"application\": null                                  " +
			"          }                                                        " +
			"        ],                                                         " +
			"        \"refund_gross\": 0,                                       " +
			"        \"refund_fees\": 0,                                        " +
			"        \"refund_fee_details\": [                                  " +
			"                                                                   " +
			"        ],                                                         " +
			"        \"adjustment_gross\": 0,                                   " +
			"        \"adjustment_fees\": 0,                                    " +
			"        \"adjustment_fee_details\": [                              " +
			"                                                                   " +
			"        ],                                                         " +
			"        \"validation_fees\": 0,                                    " +
			"        \"validation_count\": 0,                                   " +
			"        \"charge_count\": 1,                                       " +
			"        \"refund_count\": 0,                                       " +
			"        \"adjustment_count\": 0,                                   " +
			"        \"net\": 936,                                              " +
			"        \"currency\": \"usd\",                                     " +
			"        \"collected_fee_gross\": 0,                                " +
			"        \"collected_fee_count\": 0,                                " +
			"        \"collected_fee_refund_gross\": 0,                         " +
			"        \"collected_fee_refund_count\": 0                          " +
			"      },                                                           " +
			"      \"transactions\": {                                          " +
			"        \"object\": \"list\",                                      " +
			"        \"count\": 1,                                              " +
			"        \"url\": \"/v1/transfers/tr_3arZEdFc4ELXQf/transactions\", " +
			"        \"data\": [                                                " +
			"          {                                                        " +
			"            \"id\": \"ch_30asdlkWE234Ds\",                         " +
			"            \"type\": \"charge\",                                  " +
			"            \"amount\": 995,                                       " +
			"            \"currency\": \"usd\",                                 " +
			"            \"net\": 936,                                          " +
			"            \"created\": 1385470780,                               " +
			"            \"description\": null,                                 " +
			"            \"fee\": 59,                                           " +
			"            \"fee_details\": [                                     " +
			"              {                                                    " +
			"                \"amount\": 59,                                    " +
			"                \"currency\": \"usd\",                             " +
			"                \"type\": \"stripe_fee\",                          " +
			"                \"description\": \"Stripe processing fees\",       " +
			"                \"application\": null                              " +
			"              }                                                    " +
			"            ]                                                      " +
			"          }                                                        " +
			"        ]                                                          " +
			"      },                                                           " +
			"      \"other_transfers\": [                                       " +
			"        \"tr_3arZEdFc4ELXQf\"                                      " +
			"      ],                                                           " +
			"      \"account\": {                                               " +
			"        \"object\": \"bank_account\",                              " +
			"        \"id\": \"ba_ASDJsdlk34Ds$d\",                             " +
			"        \"bank_name\": \"ANOTHER CRAPPY BANK\",                    " +
			"        \"last4\": \"5987\",                                       " +
			"        \"country\": \"US\",                                       " +
			"        \"currency\": \"usd\",                                     " +
			"        \"validated\": false,                                      " +
			"        \"verified\": false,                                       " +
			"        \"fingerprint\": \"g2timWkasdWdzn2n\"                      " +
			"      },                                                           " +
			"      \"description\": \"STRIPE TRANSFER\",                        " +
			"      \"metadata\": {                                              " +
			"      },                                                           " +
			"      \"statement_descriptor\": null,                              " +
			"      \"recipient\": null,                                         " +
			"      \"fee\": 0,                                                  " +
			"      \"fee_details\": [                                           " +
			"                                                                   " +
			"      ]                                                            " +
			"    }                                                              " +
			"  },                                                               " +
			"  \"object\": \"event\",                                           " +
			"  \"pending_webhooks\": 1,                                         " +
			"  \"request\": null                                                " +
			"}";
	
	@Test
	public void testType() throws ParseException
	{
		JSONObject webHook = CFStripeWebHook.getWebHook(chargeSucceeded);
		String actual = CFStripeWebHook.getType(webHook);
		assertEquals(actual, "charge.succeeded");
		
		webHook = CFStripeWebHook.getWebHook(transferCreated);
		actual = CFStripeWebHook.getType(webHook);
		assertEquals(actual, "transfer.created");
	}

	@Test
	public void testLive() throws ParseException
	{
		JSONObject webHook = CFStripeWebHook.getWebHook(chargeSucceeded);
		assertTrue(CFStripeWebHook.getIsLive(webHook));
		
		webHook = CFStripeWebHook.getWebHook(transferCreated);
		assertFalse(CFStripeWebHook.getIsLive(webHook));
	}

	@Test
	public void testId() throws ParseException
	{
		JSONObject webHook = CFStripeWebHook.getWebHook(chargeSucceeded);
		assertEquals("evt_asdfwe34SDkl43", CFStripeWebHook.getId(webHook));
	}

	@Test
	public void testGetPayment() throws ParseException
	{
		JSONObject webHook = CFStripeWebHook.getWebHook(chargeSucceeded);
		Payment pay = CFStripeWebHook.getPayment(webHook);
		assertEquals("ch_34sdrfljdslSDF", pay.getPaymentCode());
		assertEquals("usd", pay.getCurrency());
		assertEquals(Convert.toBigDecimal(5.95), pay.getGross());
		assertEquals(Convert.toBigDecimal(0.59), pay.getFees());
		assertEquals("cus_jasfklWESD345D", pay.getCustomerCode());
		assertTrue(pay.isPaid());
		assertFalse(pay.isRefunded());
	}
}
