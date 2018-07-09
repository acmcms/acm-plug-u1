/*
 * Created on 11.04.2006
 */
package ru.myx.al.api.access;

import ru.myx.ae3.cache.CreationHandlerObject;
import ru.myx.ae3.report.Report;

final class CreatorLoginToGuid implements CreationHandlerObject<Void, String> {
	static final long				TTL	= 5L * 60000L;
	
	private final AccessManagerImpl	manager;
	
	private final String			nullUserId;
	
	CreatorLoginToGuid(final AccessManagerImpl manager, final String nullUserId) {
		this.manager = manager;
		this.nullUserId = nullUserId;
	}
	
	@Override
	public final String create(final Void attachment, final String key) {
		if (Report.MODE_DEBUG) {
			Report.devel( "UMAN", "USR_LOGIN_CACHE_CREATE, key=" + key );
		}
		final RequestLoginToGuid request = new RequestLoginToGuid( this.manager, key );
		this.manager.enqueueTask( request );
		final String userId = request.baseValue();
		return userId != null
				? userId
				: this.nullUserId;
	}
	
	@Override
	public final long getTTL() {
		return CreatorLoginToGuid.TTL;
	}
}
