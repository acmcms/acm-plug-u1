/**
 *
 */
package ru.myx.al.api.access;

import java.sql.Connection;
import java.sql.SQLException;

import ru.myx.ae1.access.AccessGroup;
import ru.myx.jdbc.queueing.RequestAttachment;
import ru.myx.jdbc.queueing.RunnerDatabaseRequestor;

/**
 * @author myx
 */
final class RequestUserGroups extends RequestAttachment<AccessGroup<?>[], RunnerDatabaseRequestor> {

	private final AccessManagerImpl manager;
	
	private final String userID;
	
	RequestUserGroups(final AccessManagerImpl manager, final String userID) {
		this.manager = manager;
		this.userID = userID;
	}
	
	@Override
	public final AccessGroup<?>[] apply(final RunnerDatabaseRequestor ctx) {

		final Connection conn = ctx.ctxGetConnection();
		try {
			final AccessGroup<?>[] result = this.manager.executeLoadUserGroups(conn, this.userID);
			this.setResult(result);
			return result;
		} catch (final SQLException e) {
			throw new RuntimeException("while reading user groups", e);
		}
	}
	
	@Override
	public final String getKey() {

		return null;
	}
}
