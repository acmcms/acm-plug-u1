/*
 * Created on 11.04.2006
 */
package ru.myx.al.api.access;

import ru.myx.ae3.cache.CreationHandlerObject;
import ru.myx.ae3.report.Report;

final class CreatorUserProfileTryCreate implements CreationHandlerObject<String, UserProfileData> {
	
	private final AccessManagerImpl manager;

	CreatorUserProfileTryCreate(final AccessManagerImpl manager) {
		
		this.manager = manager;
	}

	@Override
	public UserProfileData create(final String attachment, final String key) {
		
		final String userId = attachment;
		if (Report.MODE_DEBUG) {
			Report.devel("UMAN", "CREATOR_PROFILE_TRY_CREATE, key=" + key + ", userId=" + userId);
		}
		final UserProfileData data = new UserProfileData();
		final RequestUserProfile request = new RequestUserProfile(this.manager, userId, key, data);
		this.manager.enqueueTask(request);
		request.baseValue();
		// this.manager.loadUserProfileData( userId, key, data );
		return data.getCreated() == -1L
			/** pristine new 8-) */
			? new UserProfileData(userId, key, -1L)
			: data;
	}

	@Override
	public long getTTL() {
		
		return 10L * 60_000L;
	}
}
