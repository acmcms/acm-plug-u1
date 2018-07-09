package ru.myx.al.api.access;

import ru.myx.ae1.AbstractPluginInstance;
import ru.myx.ae1.know.Server;
import ru.myx.ae3.act.Act;
import ru.myx.ae3.act.Context;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.exec.Exec;
import ru.myx.jdbc.queueing.RunnerDatabaseRequestor;

/*
 * Created on 11.05.2004
 */
/**
 * @author myx
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Plugin extends AbstractPluginInstance {
	private Server				server;
	
	private String				tablePrefix;
	
	private String				poolAlias;
	
	private AccessManagerImpl	manager;
	
	@Override
	public void destroy() {
		this.manager.stop();
	}
	
	@Override
	public void register() {
		this.server = Context.getServer( Exec.currentProcess() );
		final RunnerDatabaseRequestor searchLoader = new RunnerDatabaseRequestor( "U1-RUNNER", //
				this.getServer().getConnections().get( this.poolAlias ) );
		this.manager = new AccessManagerImpl( this.server, this.poolAlias, this.tablePrefix, searchLoader );
		this.server.setStorage( this.manager );
		this.server.setAccessManager( this.manager );
		this.manager.register();
	}
	
	@Override
	public void setup() {
		final BaseObject info = this.getSettingsProtected();
		this.tablePrefix = Base.getString( info, "tablePrefix", "tableprefix", "um" );
		this.poolAlias = Base.getString( info, "poolAlias", "poolid", null );
	}
	
	@Override
	public void start() {
		this.manager.start();
		Act.later( this.server.getRootContext(),
				new MaintananceTask( this.server, this.poolAlias, this.tablePrefix ),
				30000 );
	}
	
	@Override
	public String toString() {
		return "Anything.AccessManager";
	}
}
