/*
 * Created on 11.04.2006
 */
package ru.myx.al.api.access;

import ru.myx.ae3.cache.CreationHandlerObject;
import ru.myx.ae3.report.Report;

final class CreatorAuthType implements CreationHandlerObject<Void, AuthTypeImpl> {
	
	private final AccessManagerImpl manager;

	CreatorAuthType(final AccessManagerImpl manager) {
		
		this.manager = manager;
	}

	@Override
	public AuthTypeImpl create(final Void attachment, final String key) {
		
		if (Report.MODE_DEBUG) {
			Report.devel("UMAN", "USR_LOGIN_CACHE_CREATE, key=" + key);
		}
		return null;
	}

	@Override
	public long getTTL() {
		
		return 11L * 60000L;
	}
}
