/*
 * Created on 10.04.2006
 */
package ru.myx.al.api.access;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import ru.myx.ae1.know.Server;
import ru.myx.ae3.Engine;
import ru.myx.ae3.act.Act;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.report.Report;

final class MaintananceTask implements Runnable {
	private final Server	server;
	
	private final String	poolAlias;
	
	private final String	tablePrefix;
	
	MaintananceTask(final Server server, final String poolAlias, final String tablePrefix) {
		this.server = server;
		this.poolAlias = poolAlias;
		this.tablePrefix = tablePrefix;
	}
	
	private final int getCleanUpNeverLogged() {
		return Convert.MapEntry.toInt( this.server.getStorage().load( "umSettings" ), "neverLogged", 0 );
	}
	
	private final int getCleanUpNotLogged() {
		return Convert.MapEntry.toInt( this.server.getStorage().load( "umSettings" ), "notLogged", 0 );
	}
	
	@Override
	public void run() {
		long nvMonths = this.getCleanUpNeverLogged();
		long ntMonths = this.getCleanUpNotLogged();
		final long cTime = Engine.fastTime();
		if (nvMonths != 0) {
			nvMonths = cTime - 1000L * 60 * 60 * 24 * 30 * nvMonths;
		}
		if (ntMonths != 0) {
			ntMonths = cTime - 1000L * 60 * 60 * 24 * 30 * ntMonths;
		}
		final Timestamp date1970 = new Timestamp( 0 );
		final Timestamp oldLogin = new Timestamp( ntMonths );
		final Timestamp oldUnknown = new Timestamp( nvMonths );
		try (final Connection conn = this.server.getServerConnection( this.poolAlias )) {
			try {
				conn.setAutoCommit( false );
				try (final PreparedStatement ps = conn
						.prepareStatement( "DELETE FROM "
								+ this.tablePrefix
								+ "UserAccounts WHERE (lastlogin is not NULL AND lastlogin!=? AND lastlogin<? AND type = 10) OR ((lastlogin is NULL OR lastlogin=?) AND added<?)" )) {
					ps.setTimestamp( 1, date1970 );
					ps.setTimestamp( 2, oldLogin );
					ps.setTimestamp( 3, date1970 );
					ps.setTimestamp( 4, oldUnknown );
					final int i = ps.executeUpdate();
					if (i > 0) {
						Report.info( "UMANAGER", i + " inactive accounts was deleted." );
					}
				}
				conn.commit();
				try (final PreparedStatement ps = conn
						.prepareStatement( "DELETE FROM "
								+ this.tablePrefix
								+ "UserProfiles WHERE LastAccess<? AND UserID!='systemUserInternal' AND Scope!='mwmRegistration'" )) {
					ps.setTimestamp( 1, oldLogin );
					final int i = ps.executeUpdate();
					if (i > 0) {
						Report.info( "UMANAGER", i + " inactive profiles was deleted." );
					}
				}
				conn.commit();
				try (final Statement st = conn.createStatement()) {
					st.execute( "DELETE FROM " + this.tablePrefix + "UserGroups WHERE groupid='def.registered'" );
					st.execute( "INSERT INTO "
							+ this.tablePrefix
							+ "UserGroups(groupid,userid,ucounter) SELECT DISTINCT 'def.registered',UserID,'upd' FROM "
							+ this.tablePrefix
							+ "UserAccounts WHERE UserID IS NOT NULL AND type>=10" );
					
					st.execute( "DELETE FROM " + this.tablePrefix + "UserGroups WHERE groupid='def.handmade'" );
					st.execute( "INSERT INTO "
							+ this.tablePrefix
							+ "UserGroups(groupid,userid,ucounter) SELECT DISTINCT 'def.handmade',UserID,'upd' FROM "
							+ this.tablePrefix
							+ "UserAccounts WHERE UserID IS NOT NULL AND type=20" );
					
					st.execute( "DELETE FROM " + this.tablePrefix + "UserGroups WHERE groupid='def.system'" );
					st.execute( "INSERT INTO "
							+ this.tablePrefix
							+ "UserGroups(groupid,userid,ucounter) SELECT DISTINCT 'def.system',UserID,'upd' FROM "
							+ this.tablePrefix
							+ "UserAccounts WHERE UserID IS NOT NULL AND type=30" );
					
					st.execute( "DELETE FROM " + this.tablePrefix + "UserGroups WHERE groupid='def.guest'" );
				}
				conn.commit();
			} catch (final Throwable t) {
				try {
					conn.rollback();
				} catch (final SQLException e) {
					// ignore
				}
				Report.exception( "UMANAGER", "Error cleaning user accounts...", t );
			}
		} catch (final SQLException e) {
			throw new RuntimeException( e );
		}
		Act.later( null, this, 600000L );
	}
}
