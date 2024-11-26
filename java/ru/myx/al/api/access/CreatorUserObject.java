/*
 * Created on 11.04.2006
 */
package ru.myx.al.api.access;

import ru.myx.ae3.cache.CreationHandlerObject;
import ru.myx.ae3.report.Report;

final class CreatorUserObject implements CreationHandlerObject<Void, UserObject> {
	
	static final long TTL = 60_000L * 60L;

	private final AccessManagerImpl manager;

	CreatorUserObject(final AccessManagerImpl manager) {
		
		this.manager = manager;
	}

	@Override
	public final UserObject create(final Void attachment, final String key) {
		
		if (Report.MODE_DEBUG) {
			Report.devel("UMAN", "USER_CACHE_CREATE, key=" + key);
		}
		return new UserObject(this.manager, key, false);
	}

	@Override
	public final long getTTL() {
		
		return CreatorUserObject.TTL;
	}
}
