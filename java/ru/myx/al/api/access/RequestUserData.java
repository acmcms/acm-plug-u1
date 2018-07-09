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
final class RequestUserData extends RequestAttachment<UserData, RunnerDatabaseRequestor> {

	private final AccessManagerImpl manager;

	private final String userID;

	private final UserData data;

	/**
	 * @param userID
	 * @param name
	 * @param data
	 */
	RequestUserData(final AccessManagerImpl manager, final String userID, final UserData data) {
		this.manager = manager;
		this.userID = userID;
		this.data = data;
	}

	@Override
	public final UserData apply(final RunnerDatabaseRequestor ctx) {

		final Connection conn = ctx.ctxGetConnection();
		try {
			this.manager.executeFillUserData(conn, this.userID, this.data);
			this.setResult(this.data);
			return this.data;
		} catch (final SQLException e) {
			throw new RuntimeException("while reading user data", e);
		}
	}

	@Override
	public final String getKey() {

		return null;
	}
}
