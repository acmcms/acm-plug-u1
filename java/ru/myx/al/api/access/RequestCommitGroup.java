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
final class RequestCommitGroup extends RequestAttachment<Void, RunnerDatabaseRequestor> {
	
	private final AccessManagerImpl manager;

	private final GroupObject group;

	RequestCommitGroup(final AccessManagerImpl manager, final GroupObject group) {
		this.manager = manager;
		this.group = group;
	}

	@Override
	public final Void apply(final RunnerDatabaseRequestor ctx) {
		
		final Connection conn = ctx.ctxGetConnection();
		try {
			this.manager.executeCommitGroup(conn, this.group);
			this.setResult(null);
			return null;
		} catch (final SQLException e) {
			throw new RuntimeException("while commiting group", e);
		}
	}

	@Override
	public final String getKey() {
		
		return null;
	}
}
