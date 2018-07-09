package ru.myx.al.api.access;

import ru.myx.ae3.base.BaseMap;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.report.Report;
import ru.myx.ae3.xml.Xml;

/**
 * Title: ae1 4 platform implementation Description: Copyright: Copyright (c)
 * 2001 Company:
 * 
 * @author Alexander I. Kharitchev
 * @version 1.0
 */
final class UserProfileData {
	private String			userID;
	
	private String			name;
	
	private long			created;
	
	private long			changed;
	
	private long			accessed;
	
	private String			dataOriginal;
	
	private BaseObject	data;
	
	UserProfileData() {
		this.created = 0;
	}
	
	UserProfileData(final String userID, final String name, final long created) {
		assert created == -1L : "For new profiles only, use -1L for 'new'";
		this.userID = userID;
		this.name = name;
		this.created = created;
	}
	
	@Override
	public final boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof UserProfileData) {
			final UserProfileData other = (UserProfileData) obj;
			return other.userID.equals( this.userID ) && other.name.equals( this.name );
		}
		return false;
	}
	
	final long getAccessed() {
		return this.accessed;
	}
	
	final long getChanged() {
		return this.changed;
	}
	
	final long getCreated() {
		return this.created;
	}
	
	final BaseObject getData() {
		if (this.data == null) {
			synchronized (this) {
				if (this.data == null) {
					if (this.dataOriginal == null) {
						final BaseMap result = new BaseNativeObject();
						this.data = result;
						this.dataOriginal = null;
						return result;
					}
					final String text = this.dataOriginal.trim();
					if (text.length() == 0) {
						final BaseMap result = new BaseNativeObject();
						this.data = result;
						this.dataOriginal = null;
						return result;
					}
					try {
						final BaseObject result = Xml.toBase( "userProfile, uid="
								+ this.userID
								+ ", name="
								+ this.name, text, null, null, null );
						this.data = result;
						this.dataOriginal = null;
						return result;
					} catch (final Exception e) {
						Report.exception( "RQ-USR-PROFILE", ("error parsing user profile, profile_source=" + text), e );
						this.data = new BaseNativeObject();
						this.dataOriginal = null;
						return this.data;
					}
				}
			}
		}
		return this.data;
	}
	
	final String getName() {
		return this.name;
	}
	
	final String getUserID() {
		return this.userID;
	}
	
	@Override
	public int hashCode() {
		return this.userID.hashCode() ^ this.name.hashCode();
	}
	
	final void setAccessed(final long accessed) {
		this.accessed = accessed;
	}
	
	final void setChanged(final long changed) {
		this.changed = changed;
	}
	
	final void setCreated(final long created) {
		this.created = created;
	}
	
	final void setData(final BaseObject data) {
		assert data != null : "NULL java value!";
		assert !data.baseIsPrimitive() : "Primitive value: class=" + data.getClass().getName() + ", value=" + data;
		assert data.baseArray() == null || data.baseHasKeysOwn() : "Array value: class="
				+ data.getClass().getName()
				+ ", value="
				+ data;
		this.data = data;
	}
	
	final void setDataOriginal(final String data) {
		this.dataOriginal = data;
	}
	
	final void setName(final String name) {
		this.name = name;
	}
	
	final void setUserID(final String userID) {
		this.userID = userID;
	}
}
