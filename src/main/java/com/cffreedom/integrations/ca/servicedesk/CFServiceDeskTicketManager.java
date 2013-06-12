package com.cffreedom.integrations.ca.servicedesk;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

import com.ca.www.UnicenterServicePlus.ServiceDesk.*;

import java.io.StringReader;
import java.net.MalformedURLException;
import java.rmi.RemoteException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.rpc.ServiceException;
import javax.xml.rpc.holders.StringHolder;

public class CFServiceDeskTicketManager
{
	public enum TICKET_TYPE { REQ, INC, PROB }
	
	private String userId = null;
	private int userSid;
	private USD_WebServiceSoap serviceDeskWS = null;
	
	public CFServiceDeskTicketManager(String userId, String password, String wsdlEndpointUrl) throws MalformedURLException, ServiceException, RemoteException 
	{
		USD_WebServiceLocator wsLocator = new USD_WebServiceLocator();
		java.net.URL url = new java.net.URL(wsdlEndpointUrl);
		this.serviceDeskWS = wsLocator.getUSD_WebServiceSoap(url);
		this.userSid = this.serviceDeskWS.login(userId, password);
		this.userId = userId;
	}
	
	public void logoff()
	{
		try {
			this.serviceDeskWS.logout(this.userSid);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	private String getTicketType(TICKET_TYPE problemType)
	{
		if (problemType.compareTo(TICKET_TYPE.REQ) == 0) { return "R"; }
		else if (problemType.compareTo(TICKET_TYPE.INC) == 0) { return "I"; }
		else if (problemType.compareTo(TICKET_TYPE.PROB) == 0) { return "P"; }
		else { return null; }
	}

	public String createTicket(TICKET_TYPE ticketType, String category, String summary, String desc)
	{
		try
		{
			String userHandle = this.serviceDeskWS.getHandleForUserid(this.userSid, this.userId);
			
			ArrayOfString attrVals = new ArrayOfString();
	        attrVals.setString(new String[]{"customer", userHandle, 
	                                       "description", desc,
	                                       "summary", summary,
	                                       "priority", "3",
	                                       "status", "OP",
	                                       "category", retrieveArea(category),
	                                       "type", this.getTicketType(ticketType)});
	
	        String template = "";
	        
	        ArrayOfString attributes = new ArrayOfString();
	        attributes.setString(new String[0]);
	
	        ArrayOfString propertyValues = new ArrayOfString();
	        propertyValues.setString(new String[0]);
	
			StringHolder newTicketHandle = new StringHolder();
			StringHolder newTicketNumber = new StringHolder();
			
			String result = this.serviceDeskWS.createRequest(this.userSid, userHandle, attrVals, propertyValues, template, attributes, newTicketHandle, newTicketNumber);
			
			//System.out.println("TicketHandle = " + newTicketHandle.value);
	        //System.out.println("TicketNumber = " + newTicketNumber.value);
	        
	        return newTicketNumber.value;
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public boolean commentOnTicket(String ticket, String comment)
	{
		try
		{
			String ticketHandle = this.getHandleFromTicket(ticket);
			int internalFlag = 0;
			this.serviceDeskWS.logComment(this.userSid, ticketHandle, comment, internalFlag);
			
			return true;
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean closeTicket(String ticket) { return this.closeTicket(ticket, ""); }
	public boolean closeTicket(String ticket, String closureNote)
	{
		try
		{
			String ticketHandle = this.getHandleFromTicket(ticket);
			String result = this.serviceDeskWS.closeTicket(this.userSid, closureNote, ticketHandle);
			
			return true;
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	private String getHandleFromTicket(String ticket)
	{
		try
		{
			String objectType = "cr";
			
			String whereClause = "ref_num = '" + ticket + "'";
					
			ArrayOfString attributes = new ArrayOfString();
	        attributes.setString(new String[]{"ref_num", "persistent_id"});
	        
			String result = this.serviceDeskWS.doSelect(this.userSid, objectType, whereClause, 1, attributes);
			
			return getAttributeHandle(result);
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	private String retrieveArea(String area)
	{
        String nArea = String.format("sym = '%s'", area);
        ArrayOfString catAttrib = new ArrayOfString();
        catAttrib.setString(new String[0]);
        String handle = "";
        
        try
        {
               String ret = this.serviceDeskWS.doSelect(this.userSid, "pcat", nArea, 1, catAttrib);
               handle = getAttributeHandle(ret);
        }
        catch (Exception ex)
        {
        	System.out.println("Exception occurred:" + ex.getMessage());
        	ex.printStackTrace(System.out);
        }
        
        return handle;
	}

	private String getAttributeHandle(String result)
	{
        String return_val = "";
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(result)));
            Element root = doc.getDocumentElement();
            Element udsObjectElement = (Element) root;

            NodeList attributesList = udsObjectElement.getElementsByTagName("Handle");
            if (attributesList.getLength() > 0) {
                Element attributesElement = (Element) attributesList.item(0);
                
                Text attrValueText = (Text) attributesElement.getFirstChild();
                if (attrValueText != null)
                    return_val = attrValueText.getNodeValue();
            }
        } catch (Exception ex) {
            System.out.println("Exception occurred:" + ex.getMessage());
            ex.printStackTrace(System.out);
        }

        return return_val;
    }

}
