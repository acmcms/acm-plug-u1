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
final class RequestLoadGroups extends RequestAttachment<Void, RunnerDatabaseRequestor> {
	
	private final AccessManagerImpl manager;

	RequestLoadGroups(final AccessManagerImpl manager) {
		this.manager = manager;
	}

	@Override
	public final Void apply(final RunnerDatabaseRequestor ctx) {
		
		final Connection conn = ctx.ctxGetConnection();
		try {
			this.manager.executeLoadGroups(conn);
			this.setResult(null);
			return null;
		} catch (final SQLException e) {
			throw new RuntimeException("while loading groups", e);
		}
	}

	@Override
	public final String getKey() {
		
		return null;
	}
}
