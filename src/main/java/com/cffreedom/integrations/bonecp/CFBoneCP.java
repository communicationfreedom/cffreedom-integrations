package com.cffreedom.integrations.bonecp;

import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cffreedom.beans.DbConn;
import com.cffreedom.exceptions.DbException;
import com.cffreedom.exceptions.InfrastructureException;
import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

/**
 * Class to make working with the BoneCP connection pooler easier
 * 
 * Original Class: com.cffreedom.integrations.bonecp.CFBoneCP
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
 * 2013-07-15 	markjacobsen.net 	Created
 */
public class CFBoneCP
{
	private static final Logger logger = LoggerFactory.getLogger("com.cffreedom.integrations.bonecp.CFBoneCP");
	private BoneCP connectionPool = null;
	
	public CFBoneCP(DbConn dbconn) throws InfrastructureException, DbException
	{
		this(dbconn, 1, 5, 10);
	}
	
	public CFBoneCP(DbConn dbconn, int partitions, int minConnections, int maxConnections) throws InfrastructureException, DbException
	{
		try
		{
			logger.info("Creating connection pool");
			Class.forName(dbconn.getDriver());
			
			BoneCPConfig config = new BoneCPConfig();
			config.setJdbcUrl(dbconn.getUrl()); // jdbc url specific to your database, eg jdbc:mysql://127.0.0.1/yourdb
			config.setUsername(dbconn.getUser()); 
			config.setPassword(dbconn.getPassword());
			config.setMinConnectionsPerPartition(minConnections);
			config.setMaxConnectionsPerPartition(maxConnections);
			config.setPartitionCount(partitions);
			this.connectionPool = new BoneCP(config); // setup the connection pool
		} 
		catch (ClassNotFoundException e)
		{
			throw new InfrastructureException("ClassNotFoundException: " + dbconn.getDriver());
		} 
		catch (SQLException e)
		{
			throw new DbException("SQLException: " + e.getMessage());
		}
	}
	
	public void close()
	{
		logger.debug("Closing connection pool");
		this.connectionPool.shutdown();
	}
	
	public Connection getConnection() throws SQLException
	{
		return this.connectionPool.getConnection();
	}
}
