/**
 *
 */
package ru.myx.al.api.access;

import java.sql.Connection;
import java.sql.SQLException;

import ru.myx.jdbc.queueing.RequestAttachment;
import ru.myx.jdbc.queueing.RunnerDatabaseRequestor;

/**
 * @author myx
 */
final class RequestUserProfile extends RequestAttachment<UserProfileData, RunnerDatabaseRequestor> {
	
	private final AccessManagerImpl manager;
	
	private final String userID;
	
	private final String name;
	
	private final UserProfileData data;
	
	RequestUserProfile(final AccessManagerImpl manager, final String userID, final String name, final UserProfileData data) {
		this.manager = manager;
		this.userID = userID;
		this.name = name;
		this.data = data;
	}
	
	@Override
	public final UserProfileData apply(final RunnerDatabaseRequestor ctx) {
		
		final Connection conn = ctx.ctxGetConnection();
		try {
			this.manager.executeFillUserProfileData(conn, this.userID, this.name, this.data);
			this.setResult(this.data);
			return this.data;
		} catch (final SQLException e) {
			throw new RuntimeException("while reading user profile", e);
		}
	}
	
	@Override
	public final String getKey() {
		
		return null;
	}
}
