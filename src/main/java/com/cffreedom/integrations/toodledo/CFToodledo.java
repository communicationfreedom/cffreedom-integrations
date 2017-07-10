package com.cffreedom.integrations.toodledo;

import java.util.ArrayList;
import java.util.Calendar;
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
import com.cffreedom.exceptions.InfrastructureException;
import com.cffreedom.exceptions.NetworkException;
import com.cffreedom.utils.Convert;
import com.cffreedom.utils.DateTimeUtils;
import com.cffreedom.utils.JsonUtils;
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
	private List<Container> contexts = null;
	
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
	public CFToodledo(String userEmail, String userPassword, String apiToken) throws Exception {
		logger.debug("User: {}", userEmail);
		
		this.userEmail = userEmail;
		this.userPass = userPassword;
		this.apiToken = apiToken;
		
		if (this.token == null) {
			logger.debug("No cached token so lets go get one");
			String sig = Convert.toMd5(this.getUserEmail() + this.apiToken);
			String url = HTTP_PROTOCOL + "api.toodledo.com/2/account/lookup.php?appid=" + this.APP_ID + ";sig=" + sig + ";email=" + this.getUserEmail() + ";pass=" + this.getUserPass();
			String response = HttpUtils.httpGet(url).getDetail();
			JSONObject jsonObj = JsonUtils.getJsonObject(response);
			String userId = JsonUtils.getString(jsonObj, "userid");
			logger.debug("Getting encoded login for {}/{}", this.getUserEmail(), userId);
			String encodedLogin = Convert.toMd5(userId + this.apiToken);
			url = HTTP_PROTOCOL + "api.toodledo.com/2/account/token.php?userid=" + userId + ";appid=" + this.APP_ID + ";sig=" + encodedLogin;
			response = HttpUtils.httpGet(url).getDetail();
			jsonObj = JsonUtils.getJsonObject(response);
			this.token = JsonUtils.getString(jsonObj, "token");
		}
		
		if (this.token == null) {
			throw new InfrastructureException("Login failed. Token is null.");
		} else {
			logger.debug("Token: {}", this.token);
		}

		this.key = Convert.toMd5(Convert.toMd5(this.getUserPass()) + this.apiToken + this.getToken());
	}

	private String getUserEmail() {
		return this.userEmail;
	}

	private String getUserPass() {
		return this.userPass;
	}

	public String getToken() {
		return this.token;
	}

	private String getKey() {
		return this.key;
	}

	private String getProjectSyncCode(List<Container> tags) {
		String code = "srUNKNOWN";

		for (Container tag : tags) {
			String tagName = tag.getValue();
			if ((tagName.length() >= 3) 
				&& (tagName.startsWith("sr")) 
				&& (Utils.isInt(tagName.replaceFirst("sr", ""), false)))
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
		List<Container> contexts = getContexts();
		List<Container> folders = getFolders();
		String url = HTTP_PROTOCOL + "api.toodledo.com/2/tasks/get.php?key=" + this.getKey() + ";comp=0;fields=" + FIELDS;
		String response = HttpUtils.httpGet(url).getDetail();
		Utils.output(response);
		JSONArray itemArray = JsonUtils.getJsonArray(response);
		logger.debug("{} tasks retrieved", itemArray.size());

		for (int i = 0; i < itemArray.size(); i++) {
			logger.trace("Item {}", i);
			try {
				JSONObject task = (JSONObject)itemArray.get(i);
				if (task.containsKey("id")) {
					List<Container> tags = new ArrayList<>();
					String code = JsonUtils.getString(task, "id");
					String title = JsonUtils.getString(task, "title");
					String meta = JsonUtils.getString(task, "meta");
					String note = JsonUtils.getString(task, "note");
					String folderId = JsonUtils.getString(task, "folder");
					String tagList = JsonUtils.getString(task, "tag");
					String contextId = JsonUtils.getString(task, "context");
					
					// Due date handling
					Calendar dueDate = null;
					Calendar dueTime = null;
					Long dueL = JsonUtils.getLong(task, "duedate");
					
					try{
						Long dueTimeL = JsonUtils.getLong(task, "duetime");
						if (dueTimeL != null) { // We have a time
							dueTime = DateTimeUtils.gmtToLocal(Convert.toCalendar(dueTimeL.longValue()*1000)); 
						}
					}catch (Exception e){
						String dueTimeS = JsonUtils.getString(task, "duetime");
						if ((dueTimeS != null) && !dueTimeS.equalsIgnoreCase("0")) {
							dueTime = DateTimeUtils.gmtToLocal(Convert.toCalendar(Convert.toLong(dueTimeS)*1000));
						}
					}
					
					if (dueL != null) {
						dueDate = Convert.toCalendar(dueL.longValue()*1000);
						if (dueTime != null){ 
							dueDate = DateTimeUtils.combineDates(dueDate, dueTime);
						} else {
							dueDate = DateTimeUtils.stripTime(dueDate); // to make it due first thing in the day
						}
					}
					
					// Start date handling
					Calendar startDate = null;
					Calendar startTime = null;
					Long startL = JsonUtils.getLong(task, "startdate");
					
					try{
						Long startTimeL = JsonUtils.getLong(task, "starttime");
						if (startTimeL != null) { // We have a time
							startTime = DateTimeUtils.gmtToLocal(Convert.toCalendar(startTimeL.longValue()*1000)); 
						}
					}catch (Exception e){
						String startTimeS = JsonUtils.getString(task, "starttime");
						if ((startTimeS != null) && !startTimeS.equalsIgnoreCase("0")) {
							startTime = DateTimeUtils.gmtToLocal(Convert.toCalendar(Convert.toLong(startTimeS)*1000));
						}
					}
					
					if (startL != null) {
						startDate = Convert.toCalendar(startL.longValue()*1000);
						if (startTime != null){ 
							startDate = DateTimeUtils.combineDates(startDate, startTime);
						} else {
							startDate = DateTimeUtils.stripTime(startDate); // to make it start first thing in the day
						}
					}
					
					// Tag handling
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
					Container folder = getFolderById(folders, folderId);
		
					if (code != null) {
						Task tsk = new Task(Task.SYS_TOODLEDO, folder, project, code, title, note, meta, startDate, dueDate, tags);
						tsk.setContext(getContextById(contexts, contextId));
						tasks.add(tsk);
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

	/**
	 * Get the tasks according to the passed in parameters
	 * @param folderName Tasks with the passed in name will be returned. All tasks if null or doesn't exist.
	 * @param contextName Tasks with the passed in name will be returned. All tasks if null or doesn't exist.
	 * @param start Null for all tasks, value to return tasks starting before this date
	 * @param due Null for all tasks, value to return tasks due before this date
	 * @return
	 * @throws NetworkException
	 * @throws ParseException
	 */
	public List<Task> getTasks(String folderName, String contextName, Calendar start, Calendar due) throws NetworkException, ParseException {
		List<Task> tasks = getTasks();
		Container folder = getFolder(folderName);
		Container context = getContext(contextName);

		for (Iterator<Task> iter = tasks.listIterator(); iter.hasNext();) {
			Task task = iter.next();
			boolean removed = false;
			if (!removed && (folderName != null) && !task.getFolder().getValue().equals(folder.getValue())) {
				iter.remove();
				removed = true;
			}
			if (!removed && (contextName != null) && !task.getContext().getValue().equals(context.getValue())) {
				iter.remove();
				removed = true;
			}
			if (!removed && (start != null) && (task.getStartDate().after(start))) {
				iter.remove();
				removed = true;
			}
			if (!removed && (due != null) && (task.getDueDate().after(due))) {
				iter.remove();
				removed = true;
			}			
		}

		logger.info("Returning {} tasks for search", tasks.size());
		return tasks;
	}
	
	public boolean insertTask(Task task) throws NetworkException, ParseException {
		String url = HTTP_PROTOCOL + "api.toodledo.com/2/tasks/add.php?key="+this.getKey()+";tasks=[{\"title\"%3A\""+task.getTitle()+"\"%2C\"folder\"%3A\""+task.getFolder().getCode()+"\"}];fields=folder";
		String response = HttpUtils.httpGet(url).getDetail();
		logger.debug("{}", response);
		JSONArray itemArray = JsonUtils.getJsonArray(response);
		JSONObject newItem = (JSONObject)(itemArray.get(0));
		Long id = JsonUtils.getLong(newItem, "id");
		logger.debug("New task id: {}", id);
		return true;
	}
	
	// return list of folders w/ each container having id for code and name for value
	public List<Container> getFolders() throws NetworkException, ParseException {
		contexts = new ArrayList<>();
		String url = HTTP_PROTOCOL + "api.toodledo.com/2/folders/get.php?key=" + this.getKey();
		String response = HttpUtils.httpGet(url).getDetail();
		//Utils.output(response);
		JSONArray itemArray = JsonUtils.getJsonArray(response);
		logger.debug("{} folders retrieved", itemArray.size());
		for (int i = 0; i < itemArray.size(); i++) {
			logger.trace("Item {}", i);
			try {
				JSONObject context = (JSONObject)itemArray.get(i);
				String id = JsonUtils.getString(context, "id");
				String name = JsonUtils.getString(context, "name");
				if (Utils.hasLength(name)) {
					contexts.add(new Container(id, name));
				}
			} catch (Exception e) {
				logger.error("Error processing item "+i, e);
			}
		}

		logger.debug("Returning {} folders", contexts.size());
		return contexts;
	}
	
	/**
	 * Get the folder corresponding to the id, return folder with zero length string for id and value if not found
	 * @param name
	 * @return
	 * @throws NetworkException
	 * @throws ParseException
	 */
	private Container getFolderById(List<Container> folders, String id) throws NetworkException, ParseException {
		Container ret = null;
		for (Container folder : folders) {
			if ((ret == null) && (folder.getCode().equals(id))) {
				logger.debug("Found folder: {}", id);
				ret = folder;
			}
		}
		if (ret == null) {
			ret = new Container("", "");
		}
		return ret;
	}
	
	/**
	 * Get the folder corresponding to the name, return null if not found
	 * @param name
	 * @return
	 * @throws NetworkException
	 * @throws ParseException
	 */
	public Container getFolder(String name) throws NetworkException, ParseException {
		Container ret = null;
		List<Container> folders = getFolders();
		for (Container folder : folders) {
			if ((ret == null) && (folder.getValue().equals(name))) {
				logger.debug("Found folder: {}", name);
				ret = folder;
			}
		}
		if (ret == null) {
			logger.debug("Folder not found: {}", name);
		}
		return ret;
	}
	
	// return list of contexts w/ each container having id for code and name for value
	public List<Container> getContexts() throws NetworkException, ParseException {
		contexts = new ArrayList<>();
		String url = HTTP_PROTOCOL + "api.toodledo.com/2/contexts/get.php?key=" + this.getKey();
		String response = HttpUtils.httpGet(url).getDetail();
		//Utils.output(response);
		JSONArray itemArray = JsonUtils.getJsonArray(response);
		logger.debug("{} contexts retrieved", itemArray.size());
		for (int i = 0; i < itemArray.size(); i++) {
			logger.trace("Item {}", i);
			try {
				JSONObject context = (JSONObject)itemArray.get(i);
				String id = JsonUtils.getString(context, "id");
				String name = JsonUtils.getString(context, "name");
				if (Utils.hasLength(name)) {
					contexts.add(new Container(id, name));
				}
			} catch (Exception e) {
				logger.error("Error processing item "+i, e);
			}
		}

		logger.debug("Returning {} contexts", contexts.size());
		return contexts;
	}
	
	/**
	 * Get the context corresponding to the id, return context with zero length string for id and value if not found
	 * @param name
	 * @return
	 * @throws NetworkException
	 * @throws ParseException
	 */
	private Container getContextById(List<Container> contexts, String id) throws NetworkException, ParseException {
		Container ctx = null;
		for (Container context : contexts) {
			if ((ctx == null) && (context.getCode().equals(id))) {
				logger.debug("Found context: {}", id);
				ctx = context;
			}
		}
		if (ctx == null) {
			ctx = new Container("", "");
		}
		return ctx;
	}
	
	/**
	 * Get the context corresponding to the name, return null if not found
	 * @param name
	 * @return
	 * @throws NetworkException
	 * @throws ParseException
	 */
	public Container getContext(String name) throws NetworkException, ParseException {
		Container ctx = null;
		List<Container> contexts = getContexts();
		for (Container context : contexts) {
			if ((ctx == null) && (context.getValue().equals(name))) {
				logger.debug("Found context: {}", name);
				ctx = context;
			}
		}
		if (ctx == null) {
			logger.debug("Context not found: {}", name);
		}
		return ctx;
	}
	
	public Container insertContext(String name) throws NetworkException, ParseException {
		Container existing = getContext(name);
		if (existing != null) {
			logger.info("Context already exists: {}", name);
			return existing;
		} else {
			String url = HTTP_PROTOCOL + "api.toodledo.com/2/contexts/add.php?key="+this.getKey()+";name="+name.replace(' ', '+');
			String response = HttpUtils.httpGet(url).getDetail();
			logger.debug("{}", response);
			JSONArray itemArray = JsonUtils.getJsonArray(response);
			JSONObject newItem = (JSONObject)(itemArray.get(0));
			String id = JsonUtils.getString(newItem, "id");
			logger.debug("New context id: {}", id);
			return new Container(id, name);
		}
	}
}
