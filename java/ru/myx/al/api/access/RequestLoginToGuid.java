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
final class RequestLoginToGuid extends RequestAttachment<String, RunnerDatabaseRequestor> {

	private final AccessManagerImpl manager;

	private final String login;

	RequestLoginToGuid(final AccessManagerImpl manager, final String login) {
		this.manager = manager;
		this.login = login;
	}

	@Override
	public final String apply(final RunnerDatabaseRequestor ctx) {

		final Connection conn = ctx.ctxGetConnection();
		try {
			final String userId = this.manager.executeLoginToGuid(conn, this.login);
			this.setResult(userId);
			return userId;
		} catch (final SQLException e) {
			throw new RuntimeException("while mapping login to guid", e);
		}

	}

	@Override
	public final String getKey() {

		return null;
	}
}
