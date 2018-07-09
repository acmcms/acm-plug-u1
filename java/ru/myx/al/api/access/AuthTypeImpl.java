package ru.myx.al.api.access;

import ru.myx.ae3.cache.Cache;
import ru.myx.ae3.cache.CacheL1;
import ru.myx.ae3.cache.CacheType;

abstract class AuthTypeImpl {
	
	private final CacheL1<String>	cacheUserIds;
	
	private final String			authType;
	
	AuthTypeImpl(final String authType) {
		this.authType = authType;
		this.cacheUserIds = Cache.createL1( "user_ids:" + authType, CacheType.NORMAL_JAVA_SOFT );
	}
	
	public abstract String getUserId(final String uniqueId);
}
