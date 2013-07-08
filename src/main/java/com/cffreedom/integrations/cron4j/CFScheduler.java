package com.cffreedom.integrations.cron4j;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cffreedom.exceptions.ApplicationStateException;
import com.cffreedom.utils.file.FileUtils;

import it.sauronsoftware.cron4j.Scheduler;

public class CFScheduler
{
	private static final Logger logger = LoggerFactory.getLogger("com.cffreedom.integrations.cron4j.CFScheduler");
	private static Scheduler scheduler = new Scheduler();
	
	public static void main(String[] args) throws ApplicationStateException
	{
		if (args.length < 2)
		{
			throw new ApplicationStateException("Incorrect number of input parameters supplied: " + args.length + 
												". Expected format: main(scheduleFile, emergencyStopFile).");
		}
		
		start(args[0], args[1]);
	}
	
	public static void start(String scheduleFile, final String emergencyStopFile) throws ApplicationStateException
	{
		if (FileUtils.fileExists(emergencyStopFile) == true)
		{
			logger.debug("Removing emergency stop file {} because it existed before we started", emergencyStopFile);
			FileUtils.deleteFile(emergencyStopFile);
		}
		scheduleFile(scheduleFile);
		
		logger.debug("Scheduling emergency stop file checker. Watching for: {}", emergencyStopFile);
		scheduler.schedule("* * * * *", new Runnable() {
			public void run() {
				if (FileUtils.fileExists(emergencyStopFile) == true)
				{
					stop();
				}
			}
		});
		
		logger.info("Starting");
		scheduler.start();
	}
	
	public static void scheduleFile(String scheduleFile) throws ApplicationStateException
	{
		if (FileUtils.fileExists(scheduleFile) == false)
		{
			throw new ApplicationStateException("Schedule file does not exist: " + scheduleFile);
		}
		
		logger.info("Scheduling file: {}", scheduleFile);
		scheduler.scheduleFile(new File(scheduleFile));
	}
	
	public static void descheduleFile(String scheduleFile) throws ApplicationStateException
	{
		if (FileUtils.fileExists(scheduleFile) == false)
		{
			throw new ApplicationStateException("Schedule file does not exist: " + scheduleFile);
		}
		
		scheduler.descheduleFile(new File(scheduleFile));
	}
	
	public static void stop()
	{
		logger.info("Stopping");
		scheduler.stop();
	}
}
