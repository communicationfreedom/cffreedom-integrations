package com.cffreedom.integrations.toodledo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cffreedom.beans.Container;
import com.cffreedom.beans.Project;
import com.cffreedom.beans.Task;
import com.cffreedom.exceptions.NetworkException;
import com.cffreedom.utils.Convert;
import com.cffreedom.utils.DateTimeUtils;
import com.cffreedom.utils.JsonUtils;
import com.cffreedom.utils.KeyValueFileMgr;
import com.cffreedom.utils.SystemUtils;
import com.cffreedom.utils.Utils;
import com.cffreedom.utils.net.HttpUtils;

/**
 * Class to make working with the Toodledo API easier
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
 * 2013-05-29 	markjacobsen.net 	Added start/due dates
 */
public class CFToodledo
{
	private static final Logger logger = LoggerFactory.getLogger("com.cffreedom.integrations.toodledo.CFToodledo");
	private final String APP_ID = "cffreedom";
	private final String HTTP_PROTOCOL = "https://";

	private String userEmail = null;
	private String userPass = null;
	private String apiToken = null;
	private String token = null;
	private String key = null;
	
	/**
	 * Create an instance of the ToodledoDAO
	 * 
	 * Note: Per the API doc "Each user is allowed to request 10 tokens per hour via the 
	 * "/account/token.php" API call. Any further token requests will be blocked."  Therefore,
	 * we work around this with the use of the KeyValueFileMgr to cache tokens between runs
	 * and to allow the use of this class for multiple users.
	 * 
	 * @param userEmail your account email
	 * @param userPassword your account password
	 * @param apiToken from your account settings
	 * @throws Exception
	 * 
	 * Changes:
	 * 2013-06-13	markjacobsen.net 	Enhancements for caching tokens and handling due/start dates/times
	 */
	public CFToodledo(String userEmail, String userPassword, String apiToken) throws Exception
	{
		logger.debug("User: {}", userEmail);
		
		this.userEmail = userEmail;
		this.userPass = userPassword;
		this.apiToken = apiToken;

		String tokenFile = null;
		KeyValueFileMgr tokenCache = null;
		
		try
		{
			tokenFile = SystemUtils.getDirTemp() + SystemUtils.getPathSeparator() + "CFToodledo.tokens";
			tokenCache = new KeyValueFileMgr(tokenFile);
			if (tokenCache.keyExists(this.getUserEmail()) == true)
			{
				String cachedVal = tokenCache.getEntryAsString(this.getUserEmail());
				logger.debug("Cached token exists for: {}, value: {}", this.getUserEmail(), cachedVal);
				String[] value = cachedVal.split("\\|");
				if (Convert.toDate(value[1], DateTimeUtils.DATE_TIMESTAMP).after(DateTimeUtils.dateAdd(new Date(), -2, DateTimeUtils.DATE_PART_HOUR)) == true)
				{
					logger.debug("Using cached token");
					this.token = value[0];
				}
				else
				{
					logger.debug("Removing expired token");
					tokenCache.removeEntry(this.getUserEmail());
				}
			}
		}
		catch (Exception e){ logger.error("Error attempting to get token from cache file"); }
		
		if (this.token == null)
		{
			logger.debug("No cached token so lets go get one");
			String sig = Convert.toMd5(this.getUserEmail() + this.apiToken);
			String url = HTTP_PROTOCOL + "api.toodledo.com/2/account/lookup.php?appid=" + this.APP_ID + ";sig=" + sig + ";email=" + this.getUserEmail() + ";pass=" + this.getUserPass();
			String response = HttpUtils.httpGet(url).getDetail();
			JSONObject jsonObj = JsonUtils.getJsonObject(response);
			String userId = JsonUtils.getString(jsonObj, "userid");
			String encodedLogin = Convert.toMd5(userId + this.apiToken);
			url = HTTP_PROTOCOL + "api.toodledo.com/2/account/token.php?userid=" + userId + ";appid=" + this.APP_ID + ";sig=" + encodedLogin;
			response = HttpUtils.httpGet(url).getDetail();
			jsonObj = JsonUtils.getJsonObject(response);
			this.token = JsonUtils.getString(jsonObj, "token");
			
			try
			{
				if (tokenCache.addEntry(this.getUserEmail(), this.token + "|" + Convert.toString(new Date(), DateTimeUtils.DATE_TIMESTAMP)) == true)
				{
					logger.debug("Stored token in cache file");
				}
			}
			catch (Exception e){ logger.error("Error attempting to store token in cache file"); }
		}
		
		logger.debug("Token: {}", this.token);

		this.key = Convert.toMd5(Convert.toMd5(this.getUserPass()) + this.apiToken + this.getToken());
	}

	private String getUserEmail()
	{
		return this.userEmail;
	}

	private String getUserPass()
	{
		return this.userPass;
	}

	public String getToken()
	{
		return this.token;
	}

	private String getKey()
	{
		return this.key;
	}

	private String getProjectSyncCode(List<Container> tags) {
		String code = "srUNKNOWN";

		for (Container tag : tags) {
			String tagName = tag.getValue();
			if ((tagName.length() >= 3) 
					&& (tagName.startsWith("sr") == true) 
					&& (Utils.isInt(tagName.replaceFirst("sr", "")) == true))
			{
				code = tagName;
				break;
			}
		}
		
		return code;
	}

	public List<Task> getTasks() throws NetworkException, ParseException {
		final String FIELDS = "meta,folder,context,tag,startdate,starttime,duedate,duetime,note";
		List<Task> tasks = new ArrayList<>();
		String url = HTTP_PROTOCOL + "api.toodledo.com/2/tasks/get.php?key=" + this.getKey() + ";comp=0;fields=" + FIELDS;
		String response = HttpUtils.httpGet(url).getDetail();
		Utils.output(response);
		JSONArray tasksArray = JsonUtils.getJsonArray(response);
		logger.debug("{} tasks retrieved", tasksArray.size());

		//Iterator<JSONObject> iterator = tasksArray.iterator();
		//while (iterator.hasNext()) {
		for (int i = 0; i < tasksArray.size(); i++) {
			logger.debug("Item {}", i);
			try {
				//JSONObject task = iterator.next();
				JSONObject task = (JSONObject)tasksArray.get(i);
				if (task.containsKey("id")) {
					logger.debug("0");
					List<Container> tags = new ArrayList<>();
					String code = JsonUtils.getString(task, "id");
					String title = JsonUtils.getString(task, "title");
					String meta = JsonUtils.getString(task, "meta");
					String note = JsonUtils.getString(task, "note");
					String folderName = JsonUtils.getString(task, "folder");
					String tagList = JsonUtils.getString(task, "tag");
					
					logger.debug("1");
					Calendar startDate = null;
					Calendar startTime = null;
					Calendar dueDate = null;
					Calendar dueTime = null;
					Long startL = JsonUtils.getLong(task, "startdate");
					String startTimeS = null;
					Long startTimeL = null;
					try {
						startTimeL = JsonUtils.getLong(task, "starttime");
					} catch (ClassCastException e) {
						startTimeS = JsonUtils.getString(task, "starttime");
					}
					Long dueL = JsonUtils.getLong(task, "duedate");
					
					logger.debug("2");
					try{
						Long dueTimeL = JsonUtils.getLong(task, "duetime");
						if (dueTimeL != null) { 
							dueTime = DateTimeUtils.gmtToLocal(Convert.toCalendar(dueTimeL.longValue()*1000)); 
							Utils.output(dueTime + "<-- converted");
						}
					}catch (Exception e){
						// If it's not a long it's going to be a string w/ a value of "0" for no time
						String dueTimeS = JsonUtils.getString(task, "duetime");
						if (dueTimeS != null) { 
							if (dueTimeS.equalsIgnoreCase("0") == true) { 
								dueTime = Convert.toCalendar("1900-01-01 00:00:00", DateTimeUtils.DATE_TIMESTAMP);
							}
							else { dueTime = Convert.toCalendar(Convert.toLong(dueTimeS)*1000); }
						}
					}
					
					logger.debug("3");
					if (startL != null) {
						startDate = Convert.toCalendar(startL.longValue()*1000);
						if (startTimeL != null) {
							startTime = DateTimeUtils.gmtToLocal(Convert.toCalendar(startTimeL*1000));
							startDate = DateTimeUtils.combineDates(startDate, startTime);
						} else if (startTimeS != null) {
							startTime = DateTimeUtils.gmtToLocal(Convert.toCalendar(Convert.toLong(startTimeS)*1000));
							startDate = DateTimeUtils.combineDates(startDate, startTime);
						}
					}
					if (dueL != null) {
						dueDate = Convert.toCalendar(dueL.longValue()*1000);
						if (dueTime != null){ 
							dueDate = DateTimeUtils.combineDates(dueDate, dueTime);
						}
					}
					
					if ((tagList != null) && (tagList.trim().length() > 0)) {
						String[] tagArray = tagList.split(",");
						for (int x = 0; x < tagArray.length; x++) {
							String tag = tagArray[x].trim();
							// System.out.println("tag --> " + tag);
							tags.add(new Container(tag, tag));
						}
					}
		
					String projectSyncCode = this.getProjectSyncCode(tags);
					Project project = new Project(projectSyncCode, projectSyncCode, projectSyncCode, "");
					Container folder = new Container(folderName, folderName);
		
					logger.debug("9");
					if (code != null) {
						logger.debug("10");
						tasks.add(new Task(Task.SYS_TOODLEDO, folder, project, code, title, note, meta, startDate, dueDate, tags));
					} else {
						logger.debug("Code value is null so skipping");
					}
				} else {
					logger.debug("Skipping entry w/o id key");
				}
			} catch (Exception e) {
				logger.error("Error processing item "+i, e);
			}
		}

		logger.debug("Returning {} tasks", tasks.size());
		return tasks;
	}

	public List<Task> getTasks(Container folder) throws NetworkException, ParseException {
		List<Task> tasks = new ArrayList<>();

		for (Task task : this.getTasks()) {
			if (task.getFolder().getValue().equalsIgnoreCase(folder.getValue()) == true)
			{
				tasks.add(task);
			}
		}

		logger.info("Returning {} tasks for folder {}", tasks.size(), folder.getValue());
		return tasks;
	}
	
	public boolean insertTask(Task task) throws NetworkException, ParseException {
		String url = HTTP_PROTOCOL + "api.toodledo.com/2/tasks/add.php?key="+this.getKey()+";tasks=[{\"title\"%3A\""+task.getTitle()+"\"%2C\"folder\"%3A\""+task.getFolder().getCode()+"\"}];fields=folder";
		String response = HttpUtils.httpGet(url).getDetail();
		logger.debug("{}", response);
		JSONArray tasksArray = JsonUtils.getJsonArray(response);
		JSONObject newTask = (JSONObject)(tasksArray.get(0));
		Long id = JsonUtils.getLong(newTask, "id");
		logger.debug("New task id: {}", id);
		return false;
	}
}
