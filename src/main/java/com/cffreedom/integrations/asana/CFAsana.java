package com.cffreedom.integrations.asana;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
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
import com.cffreedom.utils.Utils;
import com.cffreedom.utils.net.HttpUtils;

/**
 * Class to make working with the Asana API easier
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
public class CFAsana
{
	private final String URL_AUTH = "https://app.asana.com/api/1.0/users/me";
	private static final Logger logger = LoggerFactory.getLogger("com.cffreedom.integrations.asana.CFAsana");
	private String apiKey;
	private String authVal;
	private JSONObject userData;

	public CFAsana(String apiKey) throws NetworkException, ParseException
	{
		this.apiKey = apiKey;
		String encodedLogin = new String(Base64.encodeBase64(this.apiKey.getBytes()));
		this.authVal = "Basic " + encodedLogin;
		JSONObject jsonObj = processUrl(URL_AUTH);
		this.userData = JsonUtils.getJsonObject(jsonObj, "data");
	}

	public JSONObject getUserData()
	{
		return this.userData;
	}

	private String getAuthVal()
	{
		return this.authVal;
	}

	private String getProjectSyncCode(String projectName)
	{
		String code = projectName.split(" ")[0];
		if (Utils.isInt(code) == false)
		{
			code = "UNKNOWN";
		}
		code = "sr" + code;
		System.out.println("SyncCode: " + code);

		return code;
	}

	public List<Container> getWorkspaces() throws ParseException {
		List<Container> workspaces = new ArrayList<>();

		JSONArray wspaces = JsonUtils.getJsonArray(this.getUserData(), "workspaces");

		Iterator<JSONObject> iterator = wspaces.iterator();
		while (iterator.hasNext()) {
			JSONObject workspace = iterator.next();
			Long id = JsonUtils.getLong(workspace, "id");
			String code = Convert.toString(id);
			String name = JsonUtils.getString(workspace, "name");

			workspaces.add(new Container(code, name));
		}

		return workspaces;
	}

	public List<Project> getProjects(Container workspace) throws IOException, ParseException {
		logger.debug("Getting projects for workspace: {}", workspace.getValue());
		List<Project> projects = new ArrayList<>();

		try {
			String url = "https://app.asana.com/api/1.0/workspaces/" + workspace.getCode() + "/projects";
			JSONObject jsonObj = processUrl(url);
			JSONArray tasksArray = JsonUtils.getJsonArray(jsonObj, "data");

			Iterator<JSONObject> iterator = tasksArray.iterator();
			while (iterator.hasNext()) {
				JSONObject task = iterator.next();
				Long id = JsonUtils.getLong(task, "id");
				String code = Convert.toString(id);
				String name = JsonUtils.getString(task, "name");
				String note = JsonUtils.getString(task, "notes");
				String syncCode = this.getProjectSyncCode(name);

				if (code != null)
				{
					projects.add(new Project(code, syncCode, name, note));
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		return projects;
	}

	public List<Task> getTasks(Container workspace, Project project) throws IOException, ParseException {
		logger.debug("Getting tasks for project: {}", project.getName());
		List<Task> tasks = new ArrayList<>();

		try {
			String url = "https://app.asana.com/api/1.0/projects/" + project.getCode() + "/tasks?opt_fields=name,notes,due_on";
			JSONObject jsonObj = processUrl(url);
			JSONArray tasksArray = JsonUtils.getJsonArray(jsonObj, "data");

			Iterator<JSONObject> iterator = tasksArray.iterator();
			while (iterator.hasNext()) {
				JSONObject task = iterator.next();
				//Utils.output(task.toJSONString());
				Long id = JsonUtils.getLong(task, "id");
				String code = Convert.toString(id);
				String title = JsonUtils.getString(task, "name");
				String meta = "";
				String note = JsonUtils.getString(task, "notes");
				String due = JsonUtils.getString(task, "due_on");		
				Calendar dueDate = Convert.toCalendar(due, DateTimeUtils.DATE_FILE);

				if (code != null)
				{
					Task tempTask = new Task(Task.SYS_ASANA, workspace, project, code, title, note, meta, dueDate, dueDate, null);
					List<Container> tags = this.getTags(tempTask);
					tempTask.setTags(tags);
					tasks.add(tempTask);
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		logger.info("Returning {} tasks for project {}", tasks.size(), project.getName());
		return tasks;
	}

	public List<Task> getTasks(Container workspace) throws IOException, ParseException {
		List<Project> projects = this.getProjects(workspace);
		List<Task> tasks = new ArrayList<Task>();

		for (Project project : projects) {
			for (Task task : this.getTasks(workspace, project)) {
				tasks.add(task);
			}
		}

		logger.info("Returning {} tasks for workspace {}", tasks.size(), workspace.getValue());
		return tasks;
	}

	public List<Container> getTags(Task task) throws IOException, ParseException {
		List<Container> tags = new ArrayList<Container>();

		try {
			String url = "https://app.asana.com/api/1.0/tasks/" + task.getCode() + "/tags";
			JSONObject jsonObj = processUrl(url);
			JSONArray tasksArray = JsonUtils.getJsonArray(jsonObj, "data");

			Iterator<JSONObject> iterator = tasksArray.iterator();
			while (iterator.hasNext()) {
				JSONObject tag = iterator.next();
				Long id = JsonUtils.getLong(tag, "id");
				String code = Convert.toString(id);
				String name = JsonUtils.getString(tag, "name");

				if (code != null)
				{
					tags.add(new Container(code, name));
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		return tags;
	}
	
	private JSONObject processUrl(String url) throws NetworkException, ParseException {
		HashMap<String, String> reqProps = new HashMap<String, String>();
		reqProps.put("Authorization", this.getAuthVal());
		String response = HttpUtils.httpGetWithReqProp(url, reqProps);
		return JsonUtils.getJsonObject(response);
	}
}
