/*
 * Created on 11.04.2006
 */
package ru.myx.al.api.access;

import ru.myx.ae1.access.AccessGroup;
import ru.myx.ae3.cache.CreationHandlerObject;
import ru.myx.ae3.report.Report;

final class CreatorUserGroups implements CreationHandlerObject<Void, AccessGroup<?>[]> {
	private final AccessManagerImpl	manager;
	
	CreatorUserGroups(final AccessManagerImpl manager) {
		this.manager = manager;
	}
	
	@Override
	public AccessGroup<?>[] create(final Void attachment, final String key) {
		if (Report.MODE_DEBUG) {
			Report.devel( "UMAN", "USR_USER_GROUPS_CREATE, userId=" + key );
		}
		final RequestUserGroups request = new RequestUserGroups( this.manager, key );
		this.manager.enqueueTask( request );
		return request.baseValue();
	}
	
	@Override
	public long getTTL() {
		return 10L * 60000L;
	}
}
