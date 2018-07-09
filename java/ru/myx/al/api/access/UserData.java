package ru.myx.al.api.access;

import ru.myx.ae1.access.PasswordType;
import ru.myx.ae1.access.UserTypes;
import ru.myx.util.PublicCloneable;

/**
 * Title: ae1 4 platform implementation Description: Copyright: Copyright (c)
 * 2001 Company:
 * 
 * @author Alexander I. Kharitchev
 * @version 1.0
 */
final class UserData implements PublicCloneable {
	static final int getPassHash(final String login, final String password) {
		if (password == null || login == null || password.length() == 0) {
			return 0;
		}
		return password.hashCode() ^ login.toLowerCase().hashCode();
	}
	
	String			userID			= null;
	
	private String	login			= null;
	
	private String	email			= null;
	
	private String	description		= null;
	
	int				type			= UserTypes.UT_AUTO;
	
	long			created			= -1;
	
	private long	changed			= 0;
	
	private String	language;
	
	private int		passHashLow		= 0;
	
	private int		passHashHigh	= 0;
	
	UserData() {
		// empty
	}
	
	final boolean checkPassword(final String password, final PasswordType passwordType) {
		final int hash = UserData.getPassHash( this.login, password );
		if (passwordType == null || passwordType == PasswordType.NORMAL) {
			return this.passHashLow == hash;
		}
		return this.passHashHigh == hash;
	}
	
	@Override
	public final Object clone() {
		final UserData result = new UserData();
		result.userID = this.userID;
		result.changed = this.changed;
		result.created = this.created;
		result.email = this.email;
		result.login = this.login;
		return result;
	}
	
	@Override
	public final boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof UserData) {
			return ((UserData) obj).userID.equals( this.userID );
		}
		return false;
	}
	
	final long getChanged() {
		return this.changed;
	}
	
	final long getCreated() {
		return this.created;
	}
	
	final String getDescription() {
		return this.description;
	}
	
	final String getEmail() {
		return this.email;
	}
	
	final String getLanguage() {
		return this.language;
	}
	
	final String getLogin() {
		return this.login;
	}
	
	final int getPassHashHigh() {
		return this.passHashHigh;
	}
	
	final int getPassHashLow() {
		return this.passHashLow;
	}
	
	final int getType() {
		return this.type;
	}
	
	final String getUserId() {
		return this.userID;
	}
	
	@Override
	public int hashCode() {
		return this.userID.hashCode();
	}
	
	final void setChanged(final long changed) {
		this.changed = changed;
	}
	
	final void setCreated(final long created) {
		this.created = created;
	}
	
	final void setDescription(final String description) {
		this.description = description;
	}
	
	final void setEmail(final String email) {
		this.email = email == null
				? null
				: email.toLowerCase();
	}
	
	final void setLanguage(final String language) {
		this.language = language;
	}
	
	final void setLogin(final String login) {
		final String loginCorrect = login.toLowerCase();
		if (this.passHashLow != 0) {
			this.passHashLow ^= this.login.hashCode() ^ loginCorrect.hashCode();
		}
		if (this.passHashHigh != 0) {
			this.passHashHigh ^= this.login.hashCode() ^ loginCorrect.hashCode();
		}
		this.login = loginCorrect;
	}
	
	final void setPassHashHigh(final int passHash) {
		this.passHashHigh = passHash;
	}
	
	final void setPassHashLow(final int passHash) {
		this.passHashLow = passHash;
	}
	
	final void setPassword(final String password, final PasswordType passwordType) {
		final int hash = UserData.getPassHash( this.login, password );
		if (passwordType == null || passwordType == PasswordType.NORMAL) {
			this.passHashLow = hash;
		}
		if (passwordType == null || passwordType == PasswordType.HIGHER) {
			this.passHashHigh = hash;
		}
	}
	
	final void setType(final int type) {
		this.type = type;
	}
	
	final void setUserID(final String userID) {
		this.userID = userID;
	}
}
