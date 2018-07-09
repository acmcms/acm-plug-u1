package ru.myx.al.api.access;

import java.util.Map;
import java.util.TreeMap;

import ru.myx.ae1.access.AbstractAccessUser;
import ru.myx.ae1.access.AccessGroup;
import ru.myx.ae1.access.AccessUser;
import ru.myx.ae1.access.PasswordType;
import ru.myx.ae1.access.UserTypes;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.report.Report;

/**
 * Title: ae1 4 platform implementation Description: Copyright: Copyright (c)
 * 2001 Company:
 *
 * @author Alexander I. Kharitchev
 * @version 1.0
 */
final class UserObject extends AbstractAccessUser<UserObject> {
	
	
	UserData data;
	
	Map<String, AccessGroup<?>> groupAdd = null;
	
	Map<String, AccessGroup<?>> groupRemove = null;
	
	// //////////////////////////////////////////////////////////////////////////////////////////
	// primary user data
	private final AccessManagerImpl manager;
	
	String userId;
	
	boolean fresh;
	
	UserObject(final AccessManagerImpl manager, final String userId, final boolean fresh) {
		this.manager = manager;
		this.userId = userId;
		this.data = null;
		this.fresh = fresh;
	}
	
	@Override
	public final boolean checkPassword(final String password) {
		
		
		return this.checkPassword(password, null);
	}
	
	@Override
	public boolean checkPassword(final String password, final PasswordType passwordType) {
		
		
		this.loadData();
		final boolean result = this.data.checkPassword(password, passwordType);
		if (result) {
			synchronized (this.manager.toUpdateUsers) {
				this.manager.toUpdateUsers.add(this.getKey());
			}
		}
		return result;
	}
	
	@Override
	public void commit() {
		
		
		this.loadData();
		this.manager.commitUser(this);
	}
	
	@Override
	public boolean equals(final Object another) {
		
		
		if (another == this) {
			return true;
		}
		if (another instanceof AccessUser<?>) {
			return this.userId.equals(((AccessUser<?>) another).getKey());
		}
		return another != null && another.equals(this);
	}
	
	@Override
	public long getChanged() {
		
		
		this.loadData();
		return this.data.getChanged();
	}
	
	@Override
	public long getCreated() {
		
		
		this.loadData();
		return this.data.getCreated();
	}
	
	@Override
	public String getDescription() {
		
		
		this.loadData();
		return this.data.getDescription();
	}
	
	@Override
	public String getEmail() {
		
		
		this.loadData();
		return this.data.getEmail();
	}
	
	@Override
	public AccessGroup<?>[] getGroups() {
		
		
		return this.manager.getGroups(this);
	}
	
	@Override
	public String getKey() {
		
		
		return this.userId;
	}
	
	@Override
	public String getLanguage() {
		
		
		this.loadData();
		return this.data.getLanguage();
	}
	
	@Override
	public String getLogin() {
		
		
		this.loadData();
		return this.data.getLogin();
	}
	
	@Override
	public BaseObject getProfile() {
		
		
		return this.getProfile("mwmRegistration", true);
	}
	
	@Override
	public BaseObject getProfile(final String name, final boolean create) {
		
		
		final UserProfileData profile = this.manager.getUserProfile(this.userId, name, this.fresh, create);
		return profile == null
			? null
			: profile.getData();
	}
	
	@Override
	public int getType() {
		
		
		this.loadData();
		return this.data.getType();
	}
	
	@Override
	public void groupAdd(final AccessGroup<?> group) {
		
		
		if (group == null) {
			return;
		}
		this.loadData();
		this.fresh = false;
		if (this.groupAdd == null) {
			synchronized (this) {
				if (this.groupAdd == null) {
					this.groupAdd = new TreeMap<>();
				}
			}
			this.groupAdd.put(group.getKey(), group);
			if (this.groupRemove != null) {
				if (this.groupRemove.remove(group.getKey()) != null) {
					if (this.groupRemove.isEmpty()) {
						this.groupRemove = null;
					}
				}
			}
		}
	}
	
	@Override
	public void groupRemove(final AccessGroup<?> group) {
		
		
		if (group == null) {
			return;
		}
		this.loadData();
		this.fresh = false;
		if (this.groupRemove == null) {
			synchronized (this) {
				if (this.groupRemove == null) {
					this.groupRemove = new TreeMap<>();
				}
			}
			this.groupRemove.put(group.getKey(), group);
			if (this.groupAdd != null) {
				if (this.groupAdd.remove(group.getKey()) != null) {
					if (this.groupAdd.isEmpty()) {
						this.groupAdd = null;
					}
				}
			}
		}
	}
	
	@Override
	public int hashCode() {
		
		
		return this.getKey().hashCode();
	}
	
	@Override
	public boolean isActive() {
		
		
		this.loadData();
		return this.getType() >= UserTypes.UT_REGISTERED;
	}
	
	@Override
	public boolean isAnonymous() {
		
		
		return this.fresh || this.getType() < UserTypes.UT_HALF_REGISTERED;
	}
	
	@Override
	public boolean isInGroup(final AccessGroup<?> group) {
		
		
		return this.manager.isInGroup(this, group);
	}
	
	@Override
	public boolean isInGroup(final String groupId) {
		
		
		return this.manager.isInGroup(this, this.manager.getGroup(groupId, true));
	}
	
	@Override
	public boolean isSystem() {
		
		
		this.loadData();
		return this.getType() >= UserTypes.UT_SYSTEM;
	}
	
	private void loadData() {
		
		
		if (this.data == null) {
			synchronized (this) {
				if (this.data != null) {
					return;
				}
				final UserData data = new UserData();
				final RequestUserData request = new RequestUserData(this.manager, this.userId, data);
				this.manager.enqueueTask(request);
				request.baseValue();
				this.data = data;
				if (this.data.created == -1L) {
					this.fresh = true;
				}
			}
		}
	}
	
	@Override
	public void setActive() {
		
		
		this.loadData();
		this.fresh = false;
		if (!this.isActive()) {
			this.setType(UserTypes.UT_REGISTERED);
		}
	}
	
	@Override
	public void setDescription(final String description) {
		
		
		this.loadData();
		this.fresh = false;
		this.data.setDescription(description);
	}
	
	@Override
	public void setEmail(final String email) {
		
		
		this.loadData();
		this.fresh = false;
		this.data.setEmail(email);
	}
	
	@Override
	public void setLanguage(final String language) {
		
		
		this.loadData();
		this.fresh = false;
		this.data.setLanguage(language);
	}
	
	@Override
	public void setLogin(final String login) {
		
		
		this.loadData();
		this.fresh = false;
		this.data.setLogin(login);
	}
	
	@Override
	public void setPassword(final String password) {
		
		
		this.loadData();
		this.fresh = false;
		Report.debug("WSM4/UMAN/USER", "USER_PASSWS_CHANGE, userid=" + this.userId);
		this.data.setPassword(password, null);
	}
	
	void setPassword(final String password, final PasswordType passwordType) {
		
		
		this.loadData();
		this.fresh = false;
		Report.debug("WSM4/UMAN/USER", "USER_PASSW_CHANGE, userid=" + this.userId + ", passwordType=" + passwordType);
		this.data.setPassword(password, passwordType);
	}
	
	@Override
	public void setPasswordHigh(final String password) {
		
		
		this.loadData();
		this.fresh = false;
		Report.debug("WSM4/UMAN/USER", "USER_PASSWS_CHANGE, userid=" + this.userId);
		this.data.setPassword(password, PasswordType.HIGHER);
	}
	
	@Override
	public void setPasswordNormal(final String password) {
		
		
		this.loadData();
		this.fresh = false;
		Report.debug("WSM4/UMAN/USER", "USER_PASSWS_CHANGE, userid=" + this.userId);
		this.data.setPassword(password, PasswordType.NORMAL);
	}
	
	@Override
	public void setProfile(final BaseObject data) {
		
		
		this.setProfile("mwmRegistration", data);
	}
	
	@Override
	public void setProfile(final String name, final BaseObject data) {
		
		
		this.manager.storeUserProfile(this.userId, name, this.fresh, data);
	}
	
	@Override
	public void setRegistered() {
		
		
		this.loadData();
		this.fresh = false;
		if (!this.isActive()) {
			this.setType(UserTypes.UT_HALF_REGISTERED);
		}
	}
	
	@Override
	public void setSystem() {
		
		
		this.loadData();
		this.fresh = false;
		if (!this.isSystem()) {
			this.setType(UserTypes.UT_SYSTEM);
		}
	}
	
	@Override
	public void setType(final int type) {
		
		
		this.loadData();
		this.fresh = false;
		this.data.setType(type);
	}
	
	@Override
	public final String toString() {
		
		
		final UserData data = this.data;
		if (data == null) {
			if (this.fresh) {
				return "USER('" + this.userId + "' /* 'ANONYMOUS */)";
			}
			return "USER('" + this.userId + "' /* DEFFERED */)";
		}
		if (this.fresh) {
			return "USER('" + this.userId + "' /* UNCOMMITTED */)";
		}
		return "USER('" + this.userId + "', '" + this.data.getLogin() + "' /* COMMITED */)";
	}
}
