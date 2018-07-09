/**
 *
 */
package ru.myx.al.api.access;

import java.sql.Connection;

import ru.myx.jdbc.queueing.RequestAttachment;
import ru.myx.jdbc.queueing.RunnerDatabaseRequestor;

/**
 * @author myx
 */
final class RequestDeleteGroup extends RequestAttachment<Void, RunnerDatabaseRequestor> {

	private final AccessManagerImpl manager;

	private final String group;

	RequestDeleteGroup(final AccessManagerImpl manager, final String group) {
		this.manager = manager;
		this.group = group;
	}

	@Override
	public final Void apply(final RunnerDatabaseRequestor ctx) {

		final Connection conn = ctx.ctxGetConnection();
		this.manager.executeDeleteGroup(conn, this.group);
		this.setResult(null);
		return null;
	}

	@Override
	public final String getKey() {

		return null;
	}
}
