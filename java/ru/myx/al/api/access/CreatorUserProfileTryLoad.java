/*
 * Created on 11.04.2006
 */
package ru.myx.al.api.access;

import ru.myx.ae3.cache.CreationHandlerObject;
import ru.myx.ae3.report.Report;

final class CreatorUserProfileTryLoad implements CreationHandlerObject<String, UserProfileData> {
	private final AccessManagerImpl	manager;
	
	private final UserProfileData	nullUserProfile;
	
	CreatorUserProfileTryLoad(final AccessManagerImpl manager, final UserProfileData nullUserProfile) {
		this.manager = manager;
		this.nullUserProfile = nullUserProfile;
	}
	
	@Override
	public UserProfileData create(final String attachment, final String key) {
		final String userId = attachment;
		if (Report.MODE_DEBUG) {
			Report.devel( "UMAN", "CREATOR_PROFILE_TRY_LOAD, key=" + key + ", userId=" + userId );
		}
		final UserProfileData data = new UserProfileData();
		final RequestUserProfile request = new RequestUserProfile( this.manager, userId, key, data );
		this.manager.enqueueTask( request );
		request.baseValue();
		
		// this.manager.loadUserProfileData( userId, key, data );
		
		if (data.getCreated() == -1L) {
			return this.nullUserProfile;
		}
		return data;
	}
	
	@Override
	public long getTTL() {
		return 10L * 60000L;
	}
}
