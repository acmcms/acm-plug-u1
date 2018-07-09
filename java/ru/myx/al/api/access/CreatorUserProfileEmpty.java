/*
 * Created on 11.04.2006
 */
package ru.myx.al.api.access;

import ru.myx.ae3.cache.CreationHandlerObject;
import ru.myx.ae3.report.Report;

final class CreatorUserProfileEmpty implements CreationHandlerObject<String, UserProfileData> {
	CreatorUserProfileEmpty() {
		// empty
	}
	
	@Override
	public UserProfileData create(final String attachment, final String key) {
		final String userId = attachment;
		if (Report.MODE_DEBUG) {
			Report.devel( "UMAN", "CREATOR_PROFILE_EMPTY, key=" + key + ", userId=" + userId );
		}
		return new UserProfileData( userId, key, -1L );
	}
	
	@Override
	public long getTTL() {
		return 10L * 60000L;
	}
}
