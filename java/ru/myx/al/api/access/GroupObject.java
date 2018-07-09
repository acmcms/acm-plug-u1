package ru.myx.al.api.access;

import ru.myx.ae1.access.AbstractAccessGroup;
import ru.myx.ae1.access.AccessUser;
import ru.myx.ae1.access.AuthLevels;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.reflect.ReflectionIgnore;
import ru.myx.ae3.xml.Xml;

/**
 * Title: ae1 4 platform implementation Description: Copyright: Copyright (c)
 * 2001 Company:
 *
 * @author Alexander I. Kharitchev
 * @version 1.0
 */
@ReflectionIgnore
public final class GroupObject extends AbstractAccessGroup<GroupObject> {
	
	
	private final AccessManagerImpl manager;
	
	private final String groupGUID;
	
	private String title = "";
	
	private String description = "";
	
	private int authLevel = AuthLevels.AL_AUTHORIZED_AUTOMATICALLY;
	
	private String xmlData;
	
	private ExcludeChecker excludeChecker;
	
	GroupObject(final AccessManagerImpl manager, final String groupGUID) {
		this.manager = manager;
		assert groupGUID != null;
		this.groupGUID = groupGUID;
	}
	
	@Override
	public boolean checkExclusions() {
		
		
		return this.excludeChecker == null
			? true
			: this.excludeChecker.check();
	}
	
	@Override
	public void commit() {
		
		
		this.manager.commitGroup(this);
	}
	
	@Override
	public int getAuthLevel() {
		
		
		return this.authLevel;
	}
	
	@Override
	public String getDescription() {
		
		
		return this.description;
	}
	
	@Override
	public String getKey() {
		
		
		return this.groupGUID;
	}
	
	@Override
	public String getTitle() {
		
		
		return this.title;
	}
	
	@Override
	public AccessUser<?>[] getUsers() {
		
		
		return this.manager.getUsers(this);
	}
	
	/**
	 * @return string
	 */
	public String getXmlData() {
		
		
		return this.xmlData == null
			? "<data/>"
			: this.xmlData;
	}
	
	@Override
	public void setAuthLevel(final int authLevel) {
		
		
		this.authLevel = authLevel;
	}
	
	@Override
	public void setDescription(final String description) {
		
		
		assert description != null;
		this.description = description;
	}
	
	@Override
	public void setTitle(final String title) {
		
		
		assert title != null;
		this.title = title;
	}
	
	/**
	 * @param xmlData
	 */
	public void setXmlData(final String xmlData) {
		
		
		this.xmlData = xmlData;
		try {
			final BaseObject data = Xml.toBase("groupSetXmlData", xmlData, null, null, null);
			this.excludeChecker = ExcludeChecker.getChecker(this.authLevel, data.baseGet("exclude", BaseObject.UNDEFINED));
		} catch (final Throwable t) {
			// ignore
		}
	}
	
	@Override
	public final String toString() {
		
		
		return "GRP{id=" + this.groupGUID + ", title=" + this.title + ", auth=" + this.authLevel + "}";
	}
}
