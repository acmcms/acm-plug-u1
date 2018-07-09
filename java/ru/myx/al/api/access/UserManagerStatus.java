/*
 * Created on 10.04.2006
 */
package ru.myx.al.api.access;

import ru.myx.ae3.cache.CacheL1;
import ru.myx.ae3.cache.CacheL2;
import ru.myx.ae3.status.StatusFiller;
import ru.myx.ae3.status.StatusInfo;
import ru.myx.ae3.status.StatusProvider;
import ru.myx.ae3.status.StatusProviderFiller;

final class UserManagerStatus implements StatusProvider {

	private final StatusProvider[] children;

	private final AccessManagerImpl impl;

	UserManagerStatus(//
			final AccessManagerImpl impl,
			final CacheL1<?> userCache,
			final CacheL2<?> profileCache,
			final CacheL1<?> userGroupsCache,
			final CacheL1<?> userLoginCache//
	) {

		this.children = new StatusProvider[]{
				new StatusProviderFiller("user_cache", "User cache", (StatusFiller) userCache),
				new StatusProviderFiller("profile_cache", "Profile cache", (StatusFiller) profileCache),
				new StatusProviderFiller("user_groups_cache", "User groups cache", (StatusFiller) userGroupsCache),
				new StatusProviderFiller("login_cache", "Login cache", (StatusFiller) userLoginCache)
		};
		this.impl = impl;
	}

	@Override
	public final StatusProvider[] childProviders() {

		return this.children;
	}

	@Override
	public final String statusDescription() {

		return "User manager status";
	}

	@Override
	public final void statusFill(final StatusInfo data) {

		this.impl.statusFill(data);
	}

	@Override
	public final String statusName() {

		return "user";
	}
}
