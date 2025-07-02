package ru.myx.al.api.access;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import ru.myx.ae1.access.AbstractAccessManager;
import ru.myx.ae1.access.AccessGroup;
import ru.myx.ae1.access.AccessUser;
import ru.myx.ae1.access.AuthLevels;
import ru.myx.ae1.access.PasswordType;
import ru.myx.ae1.access.SortMode;
import ru.myx.ae1.access.UserTypes;
import ru.myx.ae1.control.Control;
import ru.myx.ae1.know.Server;
import ru.myx.ae1.provide.ProvideStatus;
import ru.myx.ae2.TemporaryStorage;
import ru.myx.ae3.Engine;
import ru.myx.ae3.access.AccessPermission;
import ru.myx.ae3.access.AccessPermissions;
import ru.myx.ae3.access.AccessPrincipal;
import ru.myx.ae3.act.Act;
import ru.myx.ae3.act.Context;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.cache.Cache;
import ru.myx.ae3.cache.CacheL1;
import ru.myx.ae3.cache.CacheL2;
import ru.myx.ae3.cache.CacheType;
import ru.myx.ae3.control.ControlActor;
import ru.myx.ae3.control.ControlContainer;
import ru.myx.ae3.control.ControlForm;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.help.Create;
import ru.myx.ae3.help.Format;
import ru.myx.ae3.report.Report;
import ru.myx.ae3.report.ReportReceiver;
import ru.myx.ae3.status.StatusInfo;
import ru.myx.ae3.status.StatusRegistry;
import ru.myx.ae3.xml.Xml;
import ru.myx.al.api.access.group_properties.FormGroupProperties;
import ru.myx.al.api.access.security_setup.FormFolderSecurity;
import ru.myx.al.api.access.user_selection.FormSearchSelectUser;
import ru.myx.al.api.access.user_selection.FormSearchSelectUsers;
import ru.myx.al.api.access.user_selection.FormSearchUsers;
import ru.myx.jdbc.queueing.RequestAttachment;
import ru.myx.jdbc.queueing.RunnerDatabaseRequestor;

/** @author myx */
public final class AccessManagerImpl extends AbstractAccessManager implements TemporaryStorage {

	private static final AccessGroup<?>[] EMPTY_GROUP_ARRAY = new AccessGroup<?>[0];

	private static final Comparator<AclObject> ACL_ASC_COMPARATOR = new ComparatorAclAscending();

	private static final Comparator<AccessGroup<?>> GROUP_DESC_COMPARATOR = new ComparatorGroupDescending();

	private static final String NULL_USER_ID = new String("NUD");

	private static final UserProfileData NULL_USER_PROFILE = new UserProfileData();

	private static final String OWNER = "ACCESS-MANAGER";

	private final static String fixPath(final String path) {

		if (path == null) {
			return "/";
		}
		final int length = path.length() - 1;
		if (length == -1) {
			return "/";
		}
		if (length > 0 && path.charAt(length) == '/') {
			return path.substring(0, length);
		}
		return path;
	}

	private static final String getSortMode(final SortMode sortMode) {

		switch (sortMode) {
			case SM_ACCESSED_DESC :
			case SM_CHANGE_DESC :
				return " ORDER BY lastlogin DESC";
			case SM_ACCESSED :
			case SM_CHANGE :
				return " ORDER BY lastlogin ASC";
			case SM_CREATION_DESC :
				return " ORDER BY added DESC";
			case SM_CREATION :
				return " ORDER BY added ASC";
			case SM_EMAIL_DESC :
				return " ORDER BY email DESC";
			case SM_LOGIN_DESC :
				return " ORDER BY login DESC";
			case SM_EMAIL :
				return " ORDER BY email ASC";
			case SM_LOGIN :
			default :
				return " ORDER BY login ASC";
		}
	}

	private static final String getSortModeGroupByAppend(final SortMode sortMode) {

		if (sortMode == null) {
			return ", login ORDER BY login ASC";
		}
		switch (sortMode) {
			case SM_ACCESSED_DESC :
			case SM_CHANGE_DESC :
				return ", lastlogin ORDER BY lastlogin DESC";
			case SM_ACCESSED :
			case SM_CHANGE :
				return ", lastlogin ORDER BY lastlogin ASC";
			case SM_CREATION_DESC :
				return ", added ORDER BY added DESC";
			case SM_CREATION :
				return ", added ORDER BY added ASC";
			case SM_EMAIL_DESC :
				return ", email ORDER BY email DESC";
			case SM_LOGIN_DESC :
				return ", login ORDER BY login DESC";
			case SM_EMAIL :
				return ", email ORDER BY email ASC";
			case SM_LOGIN :
			default :
				return ", login ORDER BY login ASC";
		}
	}

	private static final long getTime(final Timestamp ts) {

		return ts == null
			? 0L
			: ts.getTime();
	}

	private long stsProfilesAccessMultiUpdates = 0;

	private long stsProfilesAccessUpdated = 0;

	private long stsUserAccessMultiUpdates = 0;

	private long stsUserLoginsResolved = 0;

	private long stsUserAccessUpdated = 0;

	private long stsGroupListLoaded = 0;

	private long stsProfilesLoaded = 0;

	private long stsUserGroupsLoaded = 0;

	private long stsUsersDeleted = 0;

	private long stsGroupsDeleted = 0;

	private long stsGroupUsersLoaded = 0;

	private long stsAclListLoaded = 0;

	private long stsSecurityChecks = 0;

	private long stsSecurityGranted = 0;

	private long stsSecurityDenied = 0;

	private long stsProfilesCommited = 0;

	private long stsProfilesUpdated = 0;

	private long stsProfilesInserted = 0;

	private long stsUsersUpdated = 0;

	private long stsUsersInserted = 0;

	private long stsGroupsUpdated = 0;

	private long stsGroupsInserted = 0;

	private long stsUsersCommited = 0;

	private long stsUsersCreated = 0;

	private long stsUsersLoaded = 0;

	private final Map<String, AclObject> aclCache = new HashMap<>(256, 0.5f);

	private final Object aclLock = new Object();

	private final Map<String, AclObject> aclMap = new HashMap<>(256, 0.5f);

	private boolean aclsLoaded = false;

	private final Map<String, Set<String>> aclViewTreeCache = new HashMap<>(256, 0.75f);

	private final CreatorUserObject creatorUserObject;

	private final CreatorLoginToGuid creatorUserLogin;

	private final CreatorAuthType creatorAuthType;

	private final CreatorUserProfileTryLoad creatorUserProfileTryLoad;

	private final CreatorUserProfileTryCreate creatorUserProfileTryCreate;

	private final CreatorUserProfileEmpty creatorUserProfileFreshCreate;

	private final CreatorUserGroups creatorUserGroups;

	private List<AccessGroup<?>> groupList = null;

	private final Object groupLock = new Object();

	private Map<String, GroupObject> groupMap = null;

	private final String pool;

	private final CacheL2<UserProfileData> cacheUserProfile = Cache.createL2("User profiles", CacheType.NORMAL_JAVA_SOFT);

	private final ScheduledJobs scheduledJobs = new ScheduledJobs(this);

	private RunnerDatabaseRequestor searchLoader;

	private final Server server;

	private boolean stopped = true;

	private final String tablePrefix;

	final Set<UserProfileData> toCommitProfiles = new HashSet<>();

	final Set<UserData> toCommitUsers = new HashSet<>();

	final Set<String> toUpdateUsers = new HashSet<>();

	final Set<UserProfileData> toUpdateProfiles = new HashSet<>();

	private final CacheL1<UserObject> cacheUser = Cache.createL1("users", CacheType.FAST_JAVA_SOFT);

	private final CacheL1<String> cacheUserLogin = Cache.createL1("user_logins", CacheType.NORMAL_JAVA_SOFT);

	private final CacheL1<AuthTypeImpl> cacheAuthType = Cache.createL1("auth_types", CacheType.NORMAL_JAVA_SOFT);

	private final String userByUserId;

	private final CacheL1<AccessGroup<?>[]> cacheUserGroups = Cache.createL1("user_groups", CacheType.NORMAL_JAVA_SOFT);

	private final String userInsertion;

	private final String userUpdate;

	AccessManagerImpl(final Server server, final String pool, final String tablePrefix, final RunnerDatabaseRequestor searchLoader) {

		this.server = server;
		this.pool = pool;
		this.searchLoader = searchLoader;
		this.creatorUserObject = new CreatorUserObject(this);
		this.creatorAuthType = new CreatorAuthType(this);
		this.creatorUserLogin = new CreatorLoginToGuid(this, AccessManagerImpl.NULL_USER_ID);
		this.creatorUserProfileFreshCreate = new CreatorUserProfileEmpty();
		this.creatorUserProfileTryLoad = new CreatorUserProfileTryLoad(this, AccessManagerImpl.NULL_USER_PROFILE);
		this.creatorUserProfileTryCreate = new CreatorUserProfileTryCreate(this);
		this.creatorUserGroups = new CreatorUserGroups(this);
		this.tablePrefix = tablePrefix;
		this.userByUserId = "SELECT UserID,login,email,passhash,passhighhash,language,type,added,lastlogin FROM " + tablePrefix + "UserAccounts WHERE UserID=?";
		this.userInsertion = "INSERT INTO " + tablePrefix + "UserAccounts(UserID,login,email,language,type,added,passhash,passhighhash) VALUES (?,?,?,?,?,?,?,?)";
		this.userUpdate = "UPDATE " + tablePrefix + "UserAccounts SET login=?, email=?, language=?, type=?, passhash=?, passhighhash=? WHERE UserID=?";
	}

	private AclObject aclByPath(final String path) {

		{
			final AclObject aclValue = this.aclCache.get(path);
			if (aclValue != null) {
				return aclValue;
			}
		}
		if (path.length() <= 1) {
			return this.aclCache.get("/");
		}
		{
			final int pos = path.lastIndexOf('/');
			if (pos == -1) {
				return this.aclCache.get("/");
			}
			final AclObject upperValue = this.aclByPath(path.substring(0, pos));
			if (upperValue == null) {
				return null;
			}
			this.aclCache.put(path, upperValue);
			return upperValue;
		}
	}

	private final void commit(final UserProfileData data) {

		if (data.getCreated() == -1L) {
			this.cacheUserProfile.put(data.getUserID(), data.getName(), data, 60_000L);
		}
		synchronized (this.toCommitProfiles) {
			this.toCommitProfiles.add(data);
		}
	}

	private final void cpdINSERT(final Connection conn, final Timestamp time, final List<UserProfileData> data) throws SQLException {

		if (data == null || data.isEmpty()) {
			return;
		}
		assert time.getTime() != -1L : "Can't use -1 for timestamp here!";
		try (final PreparedStatement ps = conn.prepareStatement("INSERT INTO " + this.tablePrefix + "UserProfiles(UserID,Scope,LastAccess,Checked,Profile) VALUES (?,?,?,?,?)")) {
			for (int i = data.size() - 1; i >= 0; --i) {
				final UserProfileData current = data.get(i);
				current.setCreated(time.getTime());
				current.setChanged(time.getTime());
				ps.setString(1, current.getUserID());
				ps.setString(2, current.getName());
				ps.setTimestamp(3, time);
				ps.setTimestamp(4, time);
				if (current.getData() == null) {
					ps.setNull(5, Types.VARCHAR);
				} else {
					ps.setString(5, Xml.toXmlString("data", current.getData(), false));
				}
				ps.execute();
				this.stsProfilesInserted++;
				if (i > 0) {
					ps.clearParameters();
				}
			}
		}
	}

	private final void cpdUPDATE(final Connection conn, final Timestamp time, final List<UserProfileData> profiles) throws SQLException {

		if (profiles == null || profiles.isEmpty()) {
			return;
		}
		try (final PreparedStatement ps = conn.prepareStatement("UPDATE " + this.tablePrefix + "UserProfiles SET LastAccess=?,Profile=? WHERE UserID=? AND Scope=?")) {
			for (int i = profiles.size() - 1; i >= 0; --i) {
				final UserProfileData current = profiles.get(i);
				current.setChanged(time.getTime());
				ps.setTimestamp(1, time);
				if (current.getData() == null) {
					ps.setNull(2, Types.VARCHAR);
				} else {
					ps.setString(2, Xml.toXmlString("data", current.getData(), false));
				}
				ps.setString(3, current.getUserID());
				ps.setString(4, current.getName());
				ps.execute();
				this.stsProfilesUpdated++;
				if (i > 0) {
					ps.clearParameters();
				}
			}
		}
	}

	private final void cudINSERT(final Connection conn, final Timestamp time, final long timestampMillis, final List<UserData> data) throws SQLException {

		if (data == null || data.isEmpty()) {
			return;
		}
		try (final PreparedStatement ps = conn.prepareStatement(this.userInsertion)) {
			for (int i = data.size() - 1; i >= 0; --i) {
				final UserData current = data.get(i);

				ps.setString(1, current.getUserId());
				ps.setString(2, current.getLogin());
				if (current.getEmail() == null) {
					ps.setNull(3, Types.VARCHAR);
				} else {
					ps.setString(3, current.getEmail());
				}
				if (current.getLanguage() == null) {
					ps.setNull(4, Types.VARCHAR);
				} else {
					ps.setString(4, current.getLanguage());
				}
				ps.setInt(5, current.getType());
				ps.setTimestamp(6, time);
				ps.setInt(7, current.getPassHashLow());
				ps.setInt(8, current.getPassHashHigh());
				ps.execute();

				current.setCreated(timestampMillis);
				current.setChanged(timestampMillis);

				this.stsUsersInserted++;
				if (i > 0) {
					ps.clearParameters();
				}
			}
		}
	}

	private final void cudUPDATE(final Connection conn, final Timestamp time, final long timestampMillis, final List<UserData> data) throws SQLException {

		if (data == null || data.isEmpty()) {
			return;
		}
		try (final PreparedStatement ps = conn.prepareStatement(this.userUpdate)) {
			for (int i = data.size() - 1; i >= 0; --i) {
				final UserData current = data.get(i);
				current.setChanged(timestampMillis);
				int index = 1;
				ps.setString(index++, current.getLogin());
				if (current.getEmail() == null) {
					ps.setNull(index++, Types.VARCHAR);
				} else {
					ps.setString(index++, current.getEmail());
				}
				if (current.getLanguage() == null) {
					ps.setNull(index++, Types.VARCHAR);
				} else {
					ps.setString(index++, current.getLanguage());
				}
				ps.setInt(index++, current.getType());
				ps.setInt(index++, current.getPassHashLow());
				ps.setInt(index++, current.getPassHashHigh());
				ps.setString(index++, current.getUserId());
				ps.execute();
				this.stsUsersUpdated++;
				if (i > 0) {
					ps.clearParameters();
				}
			}
		}
	}

	private final AccessGroup<?> executeGetGroup(final Connection conn, final String key, final boolean create) throws SQLException {

		if (key == null || key.length() == 0) {
			if (create) {
				final GroupObject result = new GroupObject(this, Engine.createGuid());
				result.setTitle("new group");
				return result;
			}
			return null;
		}
		Map<String, GroupObject> groupMap;
		for (;;) {
			{
				groupMap = this.groupMap;
				if (groupMap != null) {
					break;
				}
			}
			synchronized (this.groupLock) {
				groupMap = this.groupMap;
				if (groupMap != null) {
					break;
				}
				this.executeLoadGroups(conn);
				groupMap = this.groupMap;
				break;
			}
		}

		GroupObject result = groupMap.get(key);
		if (result == null && create) {
			result = new GroupObject(this, Engine.createGuid());
			result.setTitle("new group");
			return result;
		}
		return result;
	}

	private final void loadAcls() {

		if (this.aclsLoaded) {
			return;
		}
		synchronized (this.aclLock) {
			if (!this.aclsLoaded) {
				final List<AclObject> aclList = new ArrayList<>();
				try (final Connection conn = this.getConnection()) {
					try (final PreparedStatement ps = conn.prepareStatement(
							"SELECT path,groupid,inherit,permissions FROM " + this.tablePrefix + "Acls ORDER BY path DESC",
							ResultSet.TYPE_FORWARD_ONLY,
							ResultSet.CONCUR_READ_ONLY)) {
						try (final ResultSet rs = ps.executeQuery()) {
							this.stsAclListLoaded++;
							while (rs.next()) {
								final String path = AccessManagerImpl.fixPath(rs.getString(1));
								final String groupId = rs.getString(2);
								final boolean inherit = rs.getBoolean(3);
								final boolean created;
								AclObject result = this.aclMap.get(path);
								if (result == null) {
									result = new AclObject(path, inherit);
									created = true;
								} else {
									created = false;
								}
								final String value = rs.getString(4);
								final Set<String> permissions;
								final AccessPermissions nodePermissions;
								boolean control;
								if (value == null || value.isBlank()) {
									permissions = AccessPermissions.PERMISSIONS_NONE;
									control = false;
									nodePermissions = null;
								} else //
								if ("*".equals(value)) {
									permissions = AccessPermissions.PERMISSIONS_ALL;
									control = true;
									nodePermissions = null;
								} else {
									final String[] ids = value.split(",");
									permissions = new TreeSet<>();
									for (int i = ids.length - 1; i >= 0; --i) {
										permissions.add(ids[i].trim());
									}
									control = false;
									final ControlActor<?> node = Control.relativeNode(this.server.getControlRoot(), path);
									nodePermissions = node == null
										? null
										: node.getCommandPermissions();
									if (nodePermissions != null) {
										final AccessPermission[] perms = nodePermissions.getAllPermissions();
										for (int i = ids.length - 1; !control && i >= 0; --i) {
											final String permName = ids[i].trim();
											for (int j = perms.length - 1; j >= 0; j--) {
												if (permName.equals(perms[j].getKey())) {
													if (perms[j].isForControl()) {
														control = true;
													}
													break;
												}
											}
										}
									}
								}
								if (control) {
									for (String sample = path;;) {
										final Set<String> dataObject = this.aclViewTreeCache.get(groupId);
										if (dataObject == null) {
											final Set<String> set = new TreeSet<>();
											set.add(sample);
											this.aclViewTreeCache.put(groupId, set);
										} else {
											dataObject.add(sample);
										}
										if (sample.length() > 1) {
											sample = AccessManagerImpl.fixPath(sample.substring(0, sample.lastIndexOf('/')));
										} else {
											break;
										}
									}
								}
								result.addSetting(new AclObject.Entry(groupId, permissions, nodePermissions));
								if (created) {
									aclList.add(result);
									this.aclMap.put(result.getPath(), result);
									this.aclCache.put(result.getPath(), result);
								}
							}
						}
					}
				} catch (final SQLException e) {
					throw new RuntimeException("groupDataProvider", e);
				}
				if (!this.aclMap.containsKey("/")) {
					final AclObject root = new AclObject("/", false);
					root.addSetting(new AclObject.Entry("def.supervisor", AccessPermissions.PERMISSIONS_ALL, null));
					aclList.add(root);
					this.aclMap.put("/", root);
					this.aclCache.put("/", root);
				}
				Collections.sort(aclList, AccessManagerImpl.ACL_ASC_COMPARATOR);
				final AclObject[] acls = aclList.toArray(new AclObject[aclList.size()]);
				for (int i = acls.length - 1; i > 0; --i) {
					final AclObject child = acls[i];
					for (int j = i - 1; j >= 0; j--) {
						final AclObject current = acls[j];
						if (child != current && child.getParent() == null && child.getPath().startsWith(current.getPath())) {
							child.setParent(current);
						}
					}
				}
				this.aclsLoaded = true;
			}
		}
	}

	private final List<AccessGroup<?>> loadGroupsList() {

		{
			final List<AccessGroup<?>> groupList = this.groupList;
			if (groupList != null) {
				return groupList;
			}
		}
		synchronized (this.groupLock) {
			final List<AccessGroup<?>> groupList = this.groupList;
			if (groupList != null) {
				return groupList;
			}
			final RequestLoadGroups request = new RequestLoadGroups(this);
			this.searchLoader.add(request);
			request.baseValue();
			return this.groupList;
		}
	}

	private final AccessUser<?>[] loadUsersForGroup(final String groupID) {

		try (final Connection conn = this.getConnection()) {
			try (final PreparedStatement ps = conn.prepareStatement(
					"SELECT u.UserID FROM " + this.tablePrefix + "UserGroups g, " + this.tablePrefix + "UserAccounts u WHERE g.groupid=? AND u.UserID=g.userid GROUP BY u.UserID",
					ResultSet.TYPE_FORWARD_ONLY,
					ResultSet.CONCUR_READ_ONLY)) {
				ps.setString(1, groupID);
				this.stsGroupUsersLoaded++;
				try (final ResultSet rs = ps.executeQuery()) {
					final List<AccessUser<?>> array = new ArrayList<>();
					while (rs.next()) {
						array.add(this.getUser(rs.getString(1), true));
					}
					return array.toArray(new AccessUser<?>[array.size()]);
				}
			}
		} catch (final SQLException e) {
			throw new RuntimeException("AccessManager", e);
		}
	}

	@Override
	public final void commitGroup(final AccessGroup<?> group) {

		if (group == null) {
			throw new NullPointerException("group can't be null!");
		}
		final GroupObject groupObject = (GroupObject) group;
		final RequestCommitGroup request = new RequestCommitGroup(this, groupObject);
		this.searchLoader.add(request);
		request.baseValue();
		synchronized (this.groupLock) {
			this.groupList = null;
			this.groupMap = null;
		}
		Report.event("UMAN/GROUP", "SAVE_GROUP", "SAVE GROUP: group=" + group + ", groupid=" + group.getKey());
	}

	@Override
	public final void commitUser(final AccessUser<?> user) {

		final UserObject userObject = (UserObject) user;
		final UserData userData = userObject.data;
		if (userData == null) {
			return;
		}
		userObject.fresh = false;
		{
			final String login = userData.getLogin();
			if (login == null || login.isBlank()) {
				userData.setLogin(Engine.createGuid());
			}
		}
		{
			/** cache */
			final String login = userData.getLogin();
			this.cacheUserLogin.put(login, userObject.userId, CreatorLoginToGuid.TTL);
		}
		{
			/** cache */
			this.cacheUser.put(userObject.userId, userObject, CreatorUserObject.TTL);
		}
		if (userObject.groupAdd != null || userObject.groupRemove != null) {
			final Set<AccessGroup<?>> added = userObject.groupAdd != null
				? new TreeSet<>(userObject.groupAdd.values())
				: null;
			final Set<AccessGroup<?>> removed = userObject.groupRemove != null
				? new TreeSet<>(userObject.groupRemove.values())
				: null;
			this.updateGroups(user, removed, added);
			userObject.groupAdd = null;
			userObject.groupRemove = null;
		}
		this.stsUsersCommited++;
		synchronized (this.toCommitUsers) {
			this.toCommitUsers.add(userObject.data);
		}
	}

	@Override
	public ControlForm<?> createFormGroupCreation(final String path) {

		return new FormGroupProperties(path, null);
	}

	@Override
	public ControlForm<?> createFormGroupProperties(final String path, final String key) {

		return new FormGroupProperties(path, key);
	}

	@Override
	public ControlForm<?> createFormSecuritySetup(final String path) {

		return new FormFolderSecurity(path);
	}

	@Override
	public ControlForm<?> createFormUserSearch(final AccessGroup<?> group, final ControlContainer<?> container) {

		return new FormSearchUsers(group, container);
	}

	@Override
	public ControlForm<?> createFormUserSelection(final AccessGroup<?> group, final AccessUser<?> user) {

		return new FormSearchSelectUser(group);
	}

	@Override
	public ControlForm<?> createFormUsersSelection(final AccessGroup<?> group, final AccessUser<?>[] users, final Function<AccessUser<?>[], Object> resultFilter) {

		return new FormSearchSelectUsers(group, users, resultFilter);
	}

	@Override
	public final AccessGroup<?> createGroup() {

		return new GroupObject(this, Engine.createGuid());
	}

	@Override
	public final AccessUser<?> createUser() {

		final String keyGuid = Engine.createGuid();
		return this.cacheUser.getCreate(keyGuid, null, keyGuid, this.creatorUserObject);
	}

	@Override
	public final void deleteGroup(final String key) {

		if (key == null) {
			throw new NullPointerException("key can't be null!");
		}
		final RequestDeleteGroup request = new RequestDeleteGroup(this, key);
		this.searchLoader.add(request);
		request.baseValue();
		synchronized (this.groupLock) {
			this.groupList = null;
			this.groupMap = null;
		}
		Report.event("UMAN/GROUP", "SAVE_GROUP", "DELETE GROUP: groupid=" + key);
	}

	@Override
	public final boolean deleteUser(final String key) {

		final int deleted;
		try (final Connection conn = this.getConnection()) {
			try (final PreparedStatement ps = conn.prepareStatement("DELETE FROM " + this.tablePrefix + "UserAccounts WHERE UserID=?")) {
				ps.setString(1, key);
				deleted = ps.executeUpdate();
				this.stsUsersDeleted++;
			}
			try (final PreparedStatement ps = conn.prepareStatement("DELETE FROM " + this.tablePrefix + "UserProfiles WHERE UserID=?")) {
				ps.setString(1, key);
				ps.executeUpdate();
			}
			try (final PreparedStatement ps = conn.prepareStatement("DELETE FROM " + this.tablePrefix + "Acls WHERE groupid=?")) {
				ps.setString(1, key);
				ps.executeUpdate();
			}
			try (final PreparedStatement ps = conn.prepareStatement("DELETE FROM " + this.tablePrefix + "UserGroups WHERE userid=?")) {
				ps.setString(1, key);
				ps.executeUpdate();
			}
			return deleted > 0;
		} catch (final SQLException e) {
			throw new RuntimeException("userDataProvider", e);
		} finally {
			this.cacheUser.remove(key);
		}
	}

	@Override
	public final AccessGroup<?>[] getAllGroups() {

		final List<AccessGroup<?>> groupList = this.loadGroupsList();
		return groupList.toArray(new AccessGroup<?>[groupList.size()]);
	}

	@Override
	public final AccessUser<?>[] getAllUsers() {

		return this.searchByType(UserTypes.UT_AUTO, UserTypes.UT_HANDMADE, SortMode.SM_ACCESSED_DESC);
	}

	@Override
	public final AccessGroup<?> getGroup(final String key, final boolean create) {

		if (key == null || key.length() == 0) {
			if (create) {
				final GroupObject result = new GroupObject(this, Engine.createGuid());
				result.setTitle("new group");
				return result;
			}
			return null;
		}
		Map<String, GroupObject> groupMap;
		for (;;) {
			{
				groupMap = this.groupMap;
				if (groupMap != null) {
					break;
				}
			}
			synchronized (this.groupLock) {
				groupMap = this.groupMap;
				if (groupMap != null) {
					break;
				}
				final RequestLoadGroups request = new RequestLoadGroups(this);
				this.searchLoader.add(request);
				request.baseValue();
				groupMap = this.groupMap;
				break;
			}
		}

		GroupObject result = groupMap.get(key);
		if (result == null && create) {
			result = new GroupObject(this, key);
			result.setTitle("");
			return result;
		}
		return result;
	}

	@Override
	public final AccessGroup<?>[] getGroups(final AccessUser<?> user) {

		if (user.getCreated() == -1) {
			return new AccessGroup<?>[]{
					this.getGroup("def.guest", true)
			};
		}
		return this.cacheUserGroups.getCreate(user.getKey(), null, user.getKey(), this.creatorUserGroups);
	}

	@Override
	public final AccessUser<?> getUser(final String key, final boolean create) {

		if (key == null || key.length() == 0) {
			// new user always doesn't exist
			return create
				? this.createUser()
				: null;
		}
		if (create) {
			return this.cacheUser.getCreate(key, null, key, this.creatorUserObject);
		}
		final AccessUser<?> cached = this.cacheUser.getCreate(key, null, key, this.creatorUserObject);
		return cached == null
			? null
			: cached.getCreated() < 0L
				? null
				: cached;
	}

	@Override
	public AccessUser<?> getUserByAuth(final String authType, final String uniqueId) {

		if (authType == null || authType.length() == 0) {
			throw new IllegalArgumentException("authType is undefined!");
		}
		if (uniqueId == null || uniqueId.length() == 0) {
			throw new IllegalArgumentException("uniqueId is undefined!");
		}
		final AuthTypeImpl auth = this.cacheAuthType.getCreate(authType, null, authType, this.creatorAuthType);
		if (auth == null) {
			throw new IllegalArgumentException("authType " + Format.Ecma.string(authType) + " is undefined!");
		}
		final String userId = auth.getUserId(uniqueId);
		return userId != null
			? this.getUser(userId, true)
			: null;
		// return auth.getUserObject( uniqueId );
	}

	@Override
	public final AccessUser<?> getUserByLogin(final String key, final boolean create) {

		if (key == null || key.length() == 0) {
			throw new IllegalArgumentException("login is undefined!");
		}
		final String keyCorrect = key.toLowerCase().trim();
		final String userId = this.cacheUserLogin.getCreate(keyCorrect, null, keyCorrect, this.creatorUserLogin);
		if (create) {
			if (userId == AccessManagerImpl.NULL_USER_ID) {
				final AccessUser<?> result = this.createUser();
				result.setLogin(keyCorrect);
				this.cacheUserLogin.remove(keyCorrect);
				return result;
			}
		} else {
			if (userId == AccessManagerImpl.NULL_USER_ID) {
				return null;
			}
		}
		return this.getUser(userId, create);
	}

	@Override
	public final AccessUser<?>[] getUsers(final AccessGroup<?> group) {

		return this.loadUsersForGroup(group.getKey());
	}

	@Override
	public boolean isInGroup(final AccessUser<?> user, final AccessGroup<?> group) {

		if (group.getAuthLevel() == AuthLevels.AL_UNAUTHORIZED) {
			return true;
		}
		final AccessGroup<?>[] userGroups = this.getGroups(user);
		final String key = group.getKey();
		for (int i = userGroups.length - 1; i >= 0; --i) {
			if (key.equals(userGroups[i].getKey())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isInGroup(final String userId, final String groupId) {

		final AccessGroup<?> group = this.getGroup(groupId, true);
		final AccessUser<?> user = this.getUser(userId, true);
		return this.isInGroup(user, group);
	}

	@Override
	public final BaseObject load(final String key) {

		final UserProfileData profile = this.getUserProfile("systemUserInternal", key, false, false);
		return profile == null
			? BaseObject.UNDEFINED
			: profile.getData();
	}

	@Override
	public final void savePersistent(final String key, final BaseObject map) {

		final UserProfileData profile = this.getUserProfile("systemUserInternal", key, false, true);
		profile.setData(map);
		profile.setAccessed(0L);
		/** need this to happen immediately */
		this.scheduledCommitUserProfileData(new UserProfileData[]{
				profile
		});
	}

	@Override
	public final void saveTemporary(final String key, final BaseObject map, final long expirationDate) {

		final UserProfileData profile = this.getUserProfile("systemUserInternal", key, false, true);
		profile.setData(map);
		profile.setAccessed(expirationDate - 60_000L * 60L * 24L * 7L);
		/** need this to happen immediately */
		this.scheduledCommitUserProfileData(new UserProfileData[]{
				profile
		});
	}

	@Override
	public final AccessUser<?>[] search(final String login, final String email, final long logonStart, final long logonEnd, final SortMode sortMode) {

		final StringBuilder wherePart = new StringBuilder();
		if (login != null && !login.isBlank()) {
			if (wherePart.length() > 0) {
				wherePart.append(" AND ");
			}
			wherePart.append("login like ?");
		}
		if (email != null && !email.isBlank()) {
			if (wherePart.length() > 0) {
				wherePart.append(" AND ");
			}
			wherePart.append("email like ?");
		}
		if (logonStart >= 0) {
			if (wherePart.length() > 0) {
				wherePart.append(" AND ");
			}
			wherePart.append("lastlogin >= ?");
		}
		if (logonEnd >= 0) {
			if (wherePart.length() > 0) {
				wherePart.append(" AND ");
			}
			wherePart.append("lastlogin <= ?");
		}
		final StringBuilder query = new StringBuilder().append("SELECT UserID FROM ").append(this.tablePrefix).append("UserAccounts WHERE ").append(wherePart)
				.append(AccessManagerImpl.getSortMode(sortMode));
		try (final Connection conn = this.getConnection()) {
			try (final PreparedStatement ps = conn.prepareStatement(query.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
				int index = 1;
				if (login != null && !login.isBlank()) {
					ps.setString(index++, login.toLowerCase().replace('*', '%'));
				}
				if (email != null && !email.isBlank()) {
					ps.setString(index++, email.toLowerCase().replace('*', '%'));
				}
				if (logonStart >= 0) {
					ps.setTimestamp(index++, new Timestamp(logonStart));
				}
				if (logonEnd >= 0) {
					ps.setTimestamp(index++, new Timestamp(logonEnd));
				}
				try (final ResultSet rs = ps.executeQuery()) {
					final List<AccessUser<?>> array = new ArrayList<>();
					while (rs.next()) {
						array.add(this.getUser(rs.getString(1), true));
					}
					return array.toArray(new AccessUser<?>[array.size()]);
				}
			}
		} catch (final SQLException e) {
			throw new RuntimeException("userDataProvider", e);
		}
	}

	@Override
	public final AccessUser<?>[] searchByMembership(final Collection<String> groups, final SortMode sortMode) {

		final Set<String> searchGroups;
		if (groups == null) {
			searchGroups = new TreeSet<>();
		} else //
		if (groups.contains("def.guest")) {
			searchGroups = new HashSet<>(groups);
			searchGroups.remove("def.guest");
		} else {
			searchGroups = Create.tempSet(groups);
		}
		try (final Connection conn = this.getConnection()) {
			try (final PreparedStatement ps = conn.prepareStatement(
					searchGroups.isEmpty()
						? "SELECT ua.UserID " + //
								"FROM " + this.tablePrefix + "UserAccounts ua " + //
								"WHERE ua.type > 5 " + //
								"GROUP BY ua.UserID" + AccessManagerImpl.getSortModeGroupByAppend(sortMode)
						: "SELECT ua.UserID " + //
								"FROM " + this.tablePrefix + "UserAccounts ua, " + this.tablePrefix + "UserGroups ur " + //
								"WHERE ur.userid = ua.UserID AND ur.groupid in ('" + String.join("','", searchGroups) + "') AND ua.type > 5 " + //
								"GROUP BY ua.UserID" + AccessManagerImpl.getSortModeGroupByAppend(sortMode),
					ResultSet.TYPE_FORWARD_ONLY,
					ResultSet.CONCUR_READ_ONLY)) {
				try (final ResultSet rs = ps.executeQuery()) {
					final List<AccessUser<?>> array = new ArrayList<>();
					while (rs.next()) {
						array.add(this.getUser(rs.getString(1), true));
					}
					return array.toArray(new AccessUser<?>[array.size()]);
				}
			}
		} catch (final SQLException e) {
			throw new RuntimeException("userDataProvider", e);
		}
	}

	@Override
	public final AccessUser<?>[] searchByType(final int minType, final int maxType, final SortMode sortMode) {

		try (final Connection conn = this.getConnection()) {
			try (final PreparedStatement ps = conn.prepareStatement(
					"SELECT UserID FROM " + this.tablePrefix + "UserAccounts WHERE type>=" + minType + " AND type<=" + maxType + AccessManagerImpl.getSortMode(sortMode),
					ResultSet.TYPE_FORWARD_ONLY,
					ResultSet.CONCUR_READ_ONLY)) {
				try (final ResultSet rs = ps.executeQuery()) {
					final List<AccessUser<?>> array = new ArrayList<>();
					while (rs.next()) {
						array.add(this.getUser(rs.getString(1), true));
					}
					return array.toArray(new AccessUser<?>[array.size()]);
				}
			}
		} catch (final SQLException e) {
			throw new RuntimeException("userDataProvider", e);
		}
	}

	@Override
	public AccessPrincipal<?> securityCheck(final int forceLevel, final String pathOriginal, final String command) {

		this.stsSecurityChecks++;
		final String path = AccessManagerImpl.fixPath(pathOriginal);
		if ("$view_tree".equals(command)) {
			final Map<String, Set<String>> treeCache;
			synchronized (this.aclLock) {
				this.loadAcls();
				treeCache = this.aclViewTreeCache;
			}
			final ExecProcess process = Exec.currentProcess();
			{
				final AccessGroup<?>[] groups = this.getAllGroups();
				for (int i = groups.length - 1; i >= 0; --i) {
					final AccessGroup<?> group = groups[i];
					if (group.getAuthLevel() >= AuthLevels.AL_AUTHORIZED_AUTOMATICALLY) {
						break;
					}
					final Set<String> paths = treeCache.get(group.getKey());
					if (paths != null && paths.contains(path)) {
						if (group.checkExclusions()) {
							Report.audit(
									AccessManagerImpl.OWNER,
									"ACCESS-CHECK-GRANTED",
									"level=" + forceLevel + ", path=" + pathOriginal + ", permission=" + command + ", group=" + group);
							this.stsSecurityGranted++;
							return group;
						}
					}
				}
			}
			final Server server = Context.getServer(process);
			{
				server.ensureAuthorization(AuthLevels.AL_AUTHORIZED_AUTOMATICALLY);
				final String userId = Context.getUserId(process);
				final AccessGroup<?>[] groups = this.getGroups(this.getUser(userId, true));
				for (int i = groups.length - 1; i >= 0; --i) {
					final AccessGroup<?> group = groups[i];
					final int groupLevel = group.getAuthLevel();
					if (groupLevel < AuthLevels.AL_AUTHORIZED_AUTOMATICALLY) {
						continue;
					}
					if (groupLevel >= AuthLevels.AL_AUTHORIZED_NORMAL) {
						break;
					}
					final Set<String> paths = treeCache.get(group.getKey());
					if (paths != null && paths.contains(path)) {
						if (group.checkExclusions()) {
							Report.audit(
									AccessManagerImpl.OWNER,
									"ACCESS-CHECK-GRANTED",
									"level=" + forceLevel + ", path=" + pathOriginal + ", permission=" + command + ", group=" + group);
							this.stsSecurityGranted++;
							return group;
						}
					}
				}
			}
			if (forceLevel >= AuthLevels.AL_AUTHORIZED_NORMAL) {
				server.ensureAuthorization(forceLevel);
				final String userId = Context.getUserId(process);
				{
					final Set<String> paths = treeCache.get(userId);
					if (paths != null && paths.contains(path)) {
						final AccessUser<?> user = this.getUser(userId, true);
						Report.audit(
								AccessManagerImpl.OWNER,
								"ACCESS-CHECK-GRANTED",
								"level=" + forceLevel + ", path=" + pathOriginal + ", permission=" + command + ", user=" + user);
						this.stsSecurityGranted++;
						return user;
					}
				}
				final AccessGroup<?>[] groups = this.getGroups(this.getUser(userId, true));
				for (int i = groups.length - 1; i >= 0; --i) {
					final AccessGroup<?> group = groups[i];
					if (group.getAuthLevel() < AuthLevels.AL_AUTHORIZED_AUTOMATICALLY) {
						continue;
					}
					if (group == AccessGroup.SUPERVISOR || "def.supervisor".equals(group.getKey())) {
						if (group.checkExclusions()) {
							Report.audit(
									AccessManagerImpl.OWNER,
									"ACCESS-CHECK-GRANTED",
									"level=" + forceLevel + ", path=" + pathOriginal + ", permission=" + command + ", group=" + group);
							this.stsSecurityGranted++;
							return group;
						}
					} else {
						final Set<String> paths = treeCache.get(group.getKey());
						if (paths != null && paths.contains(path)) {
							if (group.checkExclusions()) {
								Report.audit(
										AccessManagerImpl.OWNER,
										"ACCESS-CHECK-GRANTED",
										"level=" + forceLevel + ", path=" + pathOriginal + ", permission=" + command + ", group=" + group);
								this.stsSecurityGranted++;
								return group;
							}
						}
					}
				}
			}
			final AclObject acl;
			synchronized (this.aclLock) {
				this.loadAcls();
				acl = this.aclByPath(path);
			}
			if (acl == null) {
				Report.audit(
						AccessManagerImpl.OWNER,
						"ACCESS-CHECK-FAILED",
						"level=" + forceLevel + ", path=" + pathOriginal + ", permission=" + command + ", reason=no_acl_in_path");
				this.stsSecurityDenied++;
				return null;
			}
			{
				final AccessGroup<?>[] groups = this.getAllGroups();
				for (int i = groups.length - 1; i >= 0; --i) {
					final AccessGroup<?> group = groups[i];
					if (group.getAuthLevel() >= AuthLevels.AL_AUTHORIZED_AUTOMATICALLY) {
						break;
					}
					final boolean permissions = acl.hasPermissionsEffectiveControl(group.getKey());
					if (permissions) {
						if (group.checkExclusions()) {
							Report.audit(
									AccessManagerImpl.OWNER,
									"ACCESS-CHECK-GRANTED",
									"level=" + forceLevel + ", path=" + pathOriginal + ", permission=" + command + ", group=" + group);
							this.stsSecurityGranted++;
							return group;
						}
					}
				}
			}
			{
				server.ensureAuthorization(AuthLevels.AL_AUTHORIZED_AUTOMATICALLY);
				final String userId = Context.getUserId(process);
				final AccessGroup<?>[] groups = this.getGroups(this.getUser(userId, true));
				for (int i = groups.length - 1; i >= 0; --i) {
					final AccessGroup<?> group = groups[i];
					final int groupLevel = group.getAuthLevel();
					if (groupLevel < AuthLevels.AL_AUTHORIZED_AUTOMATICALLY) {
						continue;
					}
					if (groupLevel >= AuthLevels.AL_AUTHORIZED_NORMAL) {
						break;
					}
					final boolean permissions = acl.hasPermissionsEffectiveControl(group.getKey());
					if (permissions) {
						if (group.checkExclusions()) {
							Report.audit(
									AccessManagerImpl.OWNER,
									"ACCESS-CHECK-GRANTED",
									"level=" + forceLevel + ", path=" + pathOriginal + ", permission=" + command + ", group=" + group);
							this.stsSecurityGranted++;
							return group;
						}
					}
				}
			}
			if (forceLevel >= AuthLevels.AL_AUTHORIZED_NORMAL) {
				server.ensureAuthorization(forceLevel);
				final String userId = Context.getUserId(process);
				{
					final boolean permissions = acl.hasPermissionsEffectiveControl(userId);
					if (permissions) {
						final AccessUser<?> user = this.getUser(userId, true);
						Report.audit(
								AccessManagerImpl.OWNER,
								"ACCESS-CHECK-GRANTED",
								"level=" + forceLevel + ", path=" + pathOriginal + ", permission=" + command + ", user=" + user);
						this.stsSecurityGranted++;
						return user;
					}
				}
				final AccessGroup<?>[] groups = this.getGroups(this.getUser(userId, true));
				for (int i = groups.length - 1; i >= 0; --i) {
					final AccessGroup<?> group = groups[i];
					if (group.getAuthLevel() < AuthLevels.AL_AUTHORIZED_AUTOMATICALLY) {
						continue;
					}
					final boolean permissions = acl.hasPermissionsEffectiveControl(group.getKey());
					if (permissions) {
						if (group.checkExclusions()) {
							Report.audit(
									AccessManagerImpl.OWNER,
									"ACCESS-CHECK-GRANTED",
									"level=" + forceLevel + ", path=" + pathOriginal + ", permission=" + command + ", group=" + group);
							this.stsSecurityGranted++;
							return group;
						}
					}
				}
			}
		} else {
			final AclObject acl;
			synchronized (this.aclLock) {
				this.loadAcls();
				acl = this.aclByPath(path);
			}
			if (acl == null) {
				Report.audit(
						AccessManagerImpl.OWNER,
						"ACCESS-CHECK-FAILED",
						"level=" + forceLevel + ", path=" + pathOriginal + ", permission=" + command + ", reason=no_acl_in_path");
				this.stsSecurityDenied++;
				return null;
			}
			final ExecProcess process = Exec.currentProcess();
			{
				final AccessGroup<?>[] groups = this.getAllGroups();
				for (int i = groups.length - 1; i >= 0; --i) {
					final AccessGroup<?> group = groups[i];
					if (group.getAuthLevel() >= AuthLevels.AL_AUTHORIZED_AUTOMATICALLY) {
						break;
					}
					final Set<String> permissions = acl.getPermissionsEffective(group.getKey());
					if (permissions != null && permissions.contains(command)) {
						if (group.checkExclusions()) {
							this.stsSecurityGranted++;
							return group;
						}
					}
				}
			}
			final Server server = Context.getServer(process);
			{
				server.ensureAuthorization(AuthLevels.AL_AUTHORIZED_AUTOMATICALLY);
				final String userId = Context.getUserId(process);
				final AccessGroup<?>[] groups = this.getGroups(this.getUser(userId, true));
				assert groups != null : "Must not be null!";
				for (int i = groups.length - 1; i >= 0; --i) {
					final AccessGroup<?> group = groups[i];
					final int groupLevel = group.getAuthLevel();
					if (groupLevel < AuthLevels.AL_AUTHORIZED_AUTOMATICALLY) {
						continue;
					}
					if (groupLevel >= AuthLevels.AL_AUTHORIZED_NORMAL) {
						break;
					}
					final Set<String> permissions = acl.getPermissionsEffective(group.getKey());
					if (permissions != null && permissions.contains(command)) {
						if (group.checkExclusions()) {
							Report.audit(
									AccessManagerImpl.OWNER,
									"ACCESS-CHECK-GRANTED",
									"level=" + forceLevel + ", path=" + pathOriginal + ", permission=" + command + ", group=" + group);
							this.stsSecurityGranted++;
							return group;
						}
					}
				}
			}
			if (forceLevel >= AuthLevels.AL_AUTHORIZED_NORMAL) {
				server.ensureAuthorization(forceLevel);
				final String userId = Context.getUserId(process);
				{
					final Set<String> permissions = acl.getPermissionsEffective(userId);
					if (permissions != null && permissions.contains(command)) {
						final AccessUser<?> user = this.getUser(userId, true);
						Report.audit(
								AccessManagerImpl.OWNER,
								"ACCESS-CHECK-GRANTED",
								"level=" + forceLevel + ", path=" + pathOriginal + ", permission=" + command + ", user=" + user);
						this.stsSecurityGranted++;
						return user;
					}
				}
				final AccessGroup<?>[] groups = this.getGroups(this.getUser(userId, true));
				assert groups != null : "Must not be null!";
				for (int i = groups.length - 1; i >= 0; --i) {
					final AccessGroup<?> group = groups[i];
					if (group.getAuthLevel() < AuthLevels.AL_AUTHORIZED_AUTOMATICALLY) {
						continue;
					}
					final Set<String> permissions = acl.getPermissionsEffective(group.getKey());
					if (permissions != null && permissions.contains(command)) {
						if (group.checkExclusions()) {
							Report.audit(
									AccessManagerImpl.OWNER,
									"ACCESS-CHECK-GRANTED",
									"level=" + forceLevel + ", path=" + pathOriginal + ", permission=" + command + ", group=" + group);
							this.stsSecurityGranted++;
							return group;
						}
					}
				}
			}
		}
		Report.audit(AccessManagerImpl.OWNER, "ACCESS-CHECK-FAILED", "level=" + forceLevel + ", path=" + pathOriginal + ", permission=" + command + ", reason=no_acl_to_match");
		this.stsSecurityDenied++;
		return null;
	}

	@Override
	public AccessPrincipal<?>[] securityGetAccessEffective(final String path, final String permission) {

		final AclObject acl = this.securityGetPermissionsFor(path);
		if (acl == null) {
			return new AccessPrincipal<?>[]{
					AccessGroup.SUPERVISOR
			};
		}
		final Set<String> principals = new TreeSet<>();
		acl.fillPrincipals(permission, principals);
		if (principals.isEmpty()) {
			return new AccessPrincipal<?>[]{
					AccessGroup.SUPERVISOR
			};
		}
		principals.add("def.supervisor");
		final String[] temp = principals.toArray(new String[principals.size()]);
		final AccessPrincipal<?>[] result = new AccessPrincipal<?>[temp.length];
		for (int i = temp.length - 1; i >= 0; --i) {
			final AccessGroup<?> group = this.getGroup(temp[i], false);
			result[i] = group == null
				? this.getUser(temp[i], true)
				: group;
		}
		return result;
	}

	@Override
	public Set<String> securityGetPermissionsEffective(final AccessPrincipal<?> principal, final String path) {

		final AclObject aclValue;
		synchronized (this.aclLock) {
			this.loadAcls();
			aclValue = this.aclByPath(AccessManagerImpl.fixPath(path));
		}
		if (aclValue == null) {
			return AccessPermissions.PERMISSIONS_NONE;
		}
		final AclObject.Entry entry = aclValue.getSettingsEffective(principal.getKey());
		return entry == null
			? AccessPermissions.PERMISSIONS_NONE
			: entry.getPermissions();
	}

	/** @param pathOriginal
	 * @return acl */
	public AclObject securityGetPermissionsFor(final String pathOriginal) {

		final String path = AccessManagerImpl.fixPath(pathOriginal);
		final AclObject acl;
		synchronized (this.aclLock) {
			acl = this.aclMap.get(path);
		}
		return acl == null
			? new AclObject(path, true)
			: acl;
	}

	/** @param acl
	 * @return acl */
	public AclObject securitySetPermissionsFor(final AclObject acl) {

		final String path = AccessManagerImpl.fixPath(acl.getPath());
		final boolean inherit = acl.isInherit();
		final String counter = Engine.createGuid();
		try (final Connection conn = this.getConnection()) {
			try {
				conn.setAutoCommit(false);
				final AclObject.Entry[] settings = acl.getSettings();
				if (settings != null && settings.length > 0) {
					try (final PreparedStatement ps = conn
							.prepareStatement("INSERT INTO " + this.tablePrefix + "Acls(path,groupid,inherit,permissions,ucounter) VALUES (?,?,?,?,?)")) {
						ps.setString(1, path);
						ps.setString(2, "def.supervisor");
						ps.setInt(
								3,
								inherit
									? 1
									: 0);
						ps.setString(4, "*");
						ps.setString(5, counter);
						ps.executeUpdate();
						for (int i = settings.length - 1; i >= 0; --i) {
							if ("def.supervisor".equals(settings[i].getKey())) {
								continue;
							}
							ps.clearParameters();
							final Set<String> params = settings[i].getPermissions();
							ps.setString(1, path);
							ps.setString(2, settings[i].getKey());
							ps.setInt(
									3,
									inherit
										? 1
										: 0);
							ps.setString(
									4,
									params == AccessPermissions.PERMISSIONS_ALL
										? "*"
										: String.join(",", params));
							ps.setString(5, counter);
							ps.executeUpdate();
						}
					}
				}
				{
					try (final PreparedStatement ps = conn.prepareStatement("DELETE FROM " + this.tablePrefix + "Acls WHERE path=? AND ucounter!=?")) {
						ps.setString(1, path);
						ps.setString(2, counter);
						ps.executeUpdate();
					}
				}
				conn.commit();
			} catch (final SQLException e) {
				try {
					conn.rollback();
				} catch (final Throwable t) {
					// ignore
				}
				throw e;
			}
		} catch (final SQLException e) {
			throw new RuntimeException("AccessManager", e);
		}
		synchronized (this.aclLock) {
			this.aclsLoaded = false;
			this.aclMap.clear();
			this.aclCache.clear();
			this.aclViewTreeCache.clear();
		}
		return acl;
	}

	@Override
	public final AccessGroup<?>[] setGroups(final AccessUser<?> user, final AccessGroup<?>[] groups) {

		// ///////////////////
		// cache 'em
		final AccessGroup<?>[] userGroups = this.getGroups(user);

		final List<AccessGroup<?>> allOld = Arrays.asList(
				userGroups == null
					? AccessManagerImpl.EMPTY_GROUP_ARRAY
					: userGroups);
		final List<AccessGroup<?>> allNew = Arrays.asList(
				groups == null
					? AccessManagerImpl.EMPTY_GROUP_ARRAY
					: groups);

		final Set<AccessGroup<?>> removed = Create.tempSet(allOld);
		removed.removeAll(allNew);

		final Set<AccessGroup<?>> added = Create.tempSet(allNew);
		added.removeAll(allOld);

		this.updateGroups(user, removed, added);
		return groups;
	}

	@Override
	public final void setPassword(final AccessUser<?> user, final String password, final PasswordType passwordType) {

		final UserObject userObject = (UserObject) user;
		userObject.setPassword(password, passwordType);
	}

	@Override
	public final void updateGroups(final AccessUser<?> user, final Set<AccessGroup<?>> removed, final Set<AccessGroup<?>> added) {

		final ReportReceiver log = Report.currentReceiverLog();
		final String counter = Engine.createGuid();
		try (final Connection conn = this.getConnection()) {
			if (removed != null && !removed.isEmpty()) {
				try (final PreparedStatement ps = conn.prepareStatement("DELETE FROM " + this.tablePrefix + "UserGroups WHERE groupid=? AND userid=?")) {
					for (final AccessGroup<?> current : removed) {
						ps.clearParameters();
						ps.setString(1, current.getKey());
						ps.setString(2, user.getKey());
						ps.executeUpdate();
						log.event("UMAN/USER", "RM_GROUP", "userid=" + user.getKey() + "&groupid=" + current.getKey());
					}
				}
			}
			if (added != null) {
				added.remove(AccessGroup.EVERYONE);
				if (!added.isEmpty()) {
					try (final PreparedStatement ps = conn.prepareStatement("INSERT INTO " + this.tablePrefix + "UserGroups(groupid,userid,ucounter) VALUES (?,?,?)")) {
						for (final AccessGroup<?> current : added) {
							ps.clearParameters();
							ps.setString(1, current.getKey());
							ps.setString(2, user.getKey());
							ps.setString(3, counter);
							ps.executeUpdate();
							log.event("UMAN/USER", "ADD_GROUP", "userid=" + user.getKey() + "&groupid=" + current.getKey());
						}
					}
				}
			}
		} catch (final SQLException e) {
			throw new RuntimeException("AccessManager", e);
		}
		// ///////////////////
		// discard 'em
		synchronized (this.cacheUserGroups) {
			this.cacheUserGroups.remove(user.getKey());
		}
	}

	/** @return */
	protected Connection getConnection() {

		return this.server.getServerConnection(this.pool);
	}

	/** @param data */
	protected final void scheduledCommitUserData(final UserData[] data) {

		final long timestampMillis = Engine.fastTime();
		final Timestamp time = new Timestamp(timestampMillis);
		try (final Connection conn = this.getConnection()) {
			final List<UserData> toInsert = new ArrayList<>(data.length);
			final List<UserData> toUpdate = new ArrayList<>(data.length);
			for (final UserData element : data) {
				(element.getCreated() < 0L
					? toInsert
					: toUpdate).add(element);
			}
			this.cudINSERT(conn, time, timestampMillis, toInsert);
			this.cudUPDATE(conn, time, timestampMillis, toUpdate);
		} catch (final SQLException e) {
			throw new RuntimeException("userDataProvider", e);
		}
	}

	/** @param data */
	protected final void scheduledCommitUserProfileData(final UserProfileData[] data) {

		final Timestamp time = new Timestamp(Engine.fastTime());
		try (final Connection conn = this.getConnection()) {
			final List<UserProfileData> toInsert = new ArrayList<>(data.length);
			final List<UserProfileData> toUpdate = new ArrayList<>(data.length);
			for (final UserProfileData element : data) {
				(element.getCreated() < 0L
					? toInsert
					: toUpdate).add(element);
			}
			this.cpdINSERT(conn, time, toInsert);
			this.cpdUPDATE(conn, time, toUpdate);
		} catch (final SQLException e) {
			throw new RuntimeException("userDataProvider", e);
		}
	}

	/** @param data */
	protected final void scheduledUpdateAccessedProfiles(final UserProfileData[] data) {

		final Timestamp time = new Timestamp(Engine.fastTime());
		try (final Connection conn = this.getConnection()) {
			final int length = data.length;
			int current = 0;
			while (length - current >= 4) {
				try (final PreparedStatement ps = conn.prepareStatement(
						"UPDATE " + this.tablePrefix + "UserProfiles SET LastAccess=? "
								+ "WHERE (UserID=? AND Scope=?) OR (UserID=? AND Scope=?) OR (UserID=? AND Scope=?) OR (UserID=? AND Scope=?)")) {
					ps.setTimestamp(1, time);
					ps.setString(2, data[current].getUserID());
					ps.setString(3, data[current++].getName());
					ps.setString(4, data[current].getUserID());
					ps.setString(5, data[current++].getName());
					ps.setString(6, data[current].getUserID());
					ps.setString(7, data[current++].getName());
					ps.setString(8, data[current].getUserID());
					ps.setString(9, data[current++].getName());
					ps.execute();
					this.stsProfilesAccessMultiUpdates++;
					this.stsProfilesAccessUpdated += 4;
				}
			}
			try (final PreparedStatement ps = conn.prepareStatement("UPDATE " + this.tablePrefix + "UserProfiles SET LastAccess=? WHERE UserID=? AND Scope=?")) {
				for (; current < length; current++) {
					ps.setTimestamp(1, time);
					ps.setString(2, data[current].getUserID());
					ps.setString(3, data[current].getName());
					ps.execute();
					this.stsProfilesAccessUpdated++;
					ps.clearParameters();
				}
			}
		} catch (final SQLException e) {
			throw new RuntimeException("userDataProvider", e);
		}
	}

	/** @param userIDs */
	protected final void scheduledUpdateAccessedUsers(final String[] userIDs) {

		final Timestamp time = new Timestamp(Engine.fastTime());
		try (final Connection conn = this.getConnection()) {
			final int length = userIDs.length;
			int current = 0;
			while (length - current >= 4) {
				try (final PreparedStatement ps = conn.prepareStatement("UPDATE " + this.tablePrefix + "UserAccounts SET lastlogin=? WHERE UserID in (?,?,?,?)")) {
					ps.setTimestamp(1, time);
					ps.setString(2, userIDs[current++]);
					ps.setString(3, userIDs[current++]);
					ps.setString(4, userIDs[current++]);
					ps.setString(5, userIDs[current++]);
					ps.execute();
					this.stsUserAccessMultiUpdates++;
					this.stsUserAccessUpdated += 4;
				}
			}
			try (final PreparedStatement ps = conn.prepareStatement("UPDATE " + this.tablePrefix + "UserAccounts SET lastlogin=? WHERE UserID=?")) {
				for (; current < length; current++) {
					ps.setTimestamp(1, time);
					ps.setString(2, userIDs[current]);
					ps.execute();
					this.stsUserAccessUpdated++;
					ps.clearParameters();
				}
			}
		} catch (final SQLException e) {
			throw new RuntimeException("userDataProvider", e);
		}
	}

	final void enqueueTask(final RequestAttachment<?, RunnerDatabaseRequestor> task) {

		// TODO: check - is it right? this '= false' was not here.
		this.stopped = false;
		this.searchLoader.add(task);
	}

	void executeCommitGroup(final Connection conn, final GroupObject groupObject) throws SQLException {

		try (final PreparedStatement ps = conn.prepareStatement("UPDATE " + this.tablePrefix + "Groups SET groupid=?,title=?,description=?,authLevel=?,data=? WHERE groupid=?")) {
			ps.setString(1, groupObject.getKey());
			ps.setString(2, groupObject.getTitle());
			ps.setString(3, groupObject.getDescription());
			ps.setInt(4, groupObject.getAuthLevel());
			ps.setString(5, groupObject.getXmlData());
			ps.setString(6, groupObject.getKey());
			if (ps.executeUpdate() != 0) {
				this.stsGroupsUpdated++;
				return;
			}
		}
		try (final PreparedStatement ps = conn.prepareStatement("INSERT INTO " + this.tablePrefix + "Groups(groupid,title,description,authLevel,data) VALUES (?,?,?,?,?)")) {
			ps.setString(1, groupObject.getKey());
			ps.setString(2, groupObject.getTitle());
			ps.setString(3, groupObject.getDescription());
			ps.setInt(4, groupObject.getAuthLevel());
			ps.setString(5, groupObject.getXmlData());
			ps.executeUpdate();
			this.stsGroupsInserted++;
		}
	}

	final void executeDeleteGroup(final Connection conn, final String key) {

		try {
			try (final PreparedStatement ps = conn.prepareStatement("DELETE FROM " + this.tablePrefix + "Groups WHERE groupid=?")) {
				ps.setString(1, key);
				ps.executeUpdate();
				this.stsGroupsDeleted++;
			}
			try (final PreparedStatement ps = conn.prepareStatement("DELETE FROM " + this.tablePrefix + "Acls WHERE groupid=?")) {
				ps.setString(1, key);
				ps.executeUpdate();
			}
			try (final PreparedStatement ps = conn.prepareStatement("DELETE FROM " + this.tablePrefix + "UserGroups WHERE groupid=?")) {
				ps.setString(1, key);
				ps.executeUpdate();
			}
		} catch (final SQLException e) {
			throw new RuntimeException("AccessManager", e);
		}
		synchronized (this.cacheUserGroups) {
			this.cacheUserGroups.clear();
		}
	}

	final void executeFillUserData(final Connection conn, final String userID, final UserData data) throws SQLException {

		this.stopped = false;
		try (final PreparedStatement ps = conn.prepareStatement(this.userByUserId)) {
			ps.setString(1, userID);
			try (final ResultSet rs = ps.executeQuery()) {
				if (!rs.next()) {
					data.setUserID(userID);
					data.setCreated(-1);
					data.setLanguage(Context.getLanguage(Exec.currentProcess()).getName());
					this.stsUsersCreated++;
					return;
				}
				data.setUserID(rs.getString(1));
				data.setLogin(rs.getString(2));
				data.setEmail(rs.getString(3));
				data.setPassHashLow(rs.getInt(4));
				data.setPassHashHigh(rs.getInt(5));
				data.setLanguage(rs.getString(6));
				data.setType(rs.getInt(7));
				data.setCreated(AccessManagerImpl.getTime(rs.getTimestamp(8)));
				data.setChanged(AccessManagerImpl.getTime(rs.getTimestamp(9)));
				this.stsUsersLoaded++;
				return;
			}
		}
	}

	final void executeFillUserProfileData(final Connection conn, final String userID, final String name, final UserProfileData data) throws SQLException {

		try (final PreparedStatement ps = conn.prepareStatement(
				"SELECT UserID,Scope,Checked,LastAccess,Profile FROM umUserProfiles WHERE UserID=? AND Scope=?",
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY)) {
			ps.setString(1, userID);
			ps.setString(2, name);
			try (final ResultSet rs = ps.executeQuery()) {
				if (!rs.next()) {
					data.setUserID(userID);
					data.setName(name);
					data.setCreated(-1L);
					return;
				}
				data.setUserID(rs.getString(1));
				data.setName(rs.getString(2));
				data.setCreated(AccessManagerImpl.getTime(rs.getTimestamp(3)));
				data.setChanged(AccessManagerImpl.getTime(rs.getTimestamp(4)));
				data.setAccessed(AccessManagerImpl.getTime(rs.getTimestamp(4)));
				data.setDataOriginal(rs.getString(5));
				if (Engine.fastTime() - data.getAccessed() > 6L * 60L * 60_000L) {
					this.toUpdateProfiles.add(data);
				}
				if (data.getCreated() <= 0) {
					data.setCreated(Engine.fastTime());
				}
				this.stsProfilesLoaded++;
			}
		}
	}

	final void executeLoadGroups(final Connection conn) throws SQLException {

		final List<AccessGroup<?>> groupList = new ArrayList<>();
		final Map<String, GroupObject> groupMap = new HashMap<>(256, 0.5f);
		try (final PreparedStatement ps = conn.prepareStatement(
				"SELECT groupid,title,description,authLevel,data FROM " + this.tablePrefix + "Groups ORDER BY authLevel DESC, title ASC",
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY)) {
			try (final ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					final GroupObject result = new GroupObject(this, rs.getString(1));
					result.setTitle(rs.getString(2));
					result.setDescription(rs.getString(3));
					final int authLevel = rs.getInt(4);
					result.setAuthLevel(
							authLevel < AuthLevels.AL_UNAUTHORIZED
								? AuthLevels.AL_AUTHORIZED_AUTOMATICALLY
								: authLevel);
					result.setXmlData(rs.getString(5));
					groupList.add(result);
					groupMap.put(result.getKey(), result);
				}
				this.stsGroupListLoaded++;
			}
		}
		{
			if (!groupMap.containsKey("def.guest")) {
				final GroupObject group = new GroupObject(this, "def.guest");
				group.setAuthLevel(AuthLevels.AL_UNAUTHORIZED);
				group.setDescription("Default group for every user");
				group.setTitle("Default - Guest");
				groupList.add(group);
				groupMap.put(group.getKey(), group);
				this.executeCommitGroup(conn, group);
			}
			if (!groupMap.containsKey("def.registered")) {
				final GroupObject group = new GroupObject(this, "def.registered");
				group.setAuthLevel(AuthLevels.AL_AUTHORIZED_AUTOMATICALLY);
				group.setDescription("Default group for every registered user");
				group.setTitle("Default - Registered");
				groupList.add(group);
				groupMap.put(group.getKey(), group);
				this.executeCommitGroup(conn, group);
			}
			if (!groupMap.containsKey("def.handmade")) {
				final GroupObject group = new GroupObject(this, "def.handmade");
				group.setAuthLevel(AuthLevels.AL_AUTHORIZED_NORMAL);
				group.setDescription("Default group for every user edited by an administrator");
				group.setTitle("Default - Handmade");
				groupList.add(group);
				groupMap.put(group.getKey(), group);
				this.executeCommitGroup(conn, group);
			}
			if (!groupMap.containsKey("def.system")) {
				final GroupObject group = new GroupObject(this, "def.system");
				group.setAuthLevel(AuthLevels.AL_AUTHORIZED_SYSTEM_EXCLUSIVE);
				group.setDescription("Default group for system users");
				group.setTitle("Default - System");
				groupList.add(group);
				groupMap.put(group.getKey(), group);
				this.executeCommitGroup(conn, group);
			}
			if (!groupMap.containsKey("def.supervisor")) {
				final GroupObject group = new GroupObject(this, "def.supervisor");
				group.setAuthLevel(AuthLevels.AL_AUTHORIZED_HIGH);
				group.setDescription("Default group for superuser");
				group.setTitle("Default - Supervisor");
				groupList.add(group);
				groupMap.put(group.getKey(), group);
				this.executeCommitGroup(conn, group);
			}
			Collections.sort(groupList, AccessManagerImpl.GROUP_DESC_COMPARATOR);
		}
		this.groupList = groupList;
		this.groupMap = groupMap;
	}

	/* Ordered by AuthLevel DESC ! */
	final AccessGroup<?>[] executeLoadUserGroups(final Connection conn, final String userID) throws SQLException {

		try (final PreparedStatement ps = conn.prepareStatement(
				"SELECT g.groupid FROM " + this.tablePrefix + "Groups g, " + this.tablePrefix + "UserGroups u WHERE u.userid=? AND g.groupid=u.groupid GROUP BY g.groupid",
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY)) {
			ps.setString(1, userID);
			try (final ResultSet rs = ps.executeQuery()) {
				this.stsUserGroupsLoaded++;
				if (!rs.next()) {
					return new AccessGroup<?>[]{
							this.executeGetGroup(conn, "def.guest", true)
					};
				}
				final List<AccessGroup<?>> array = new ArrayList<>(32);
				do {
					array.add(this.executeGetGroup(conn, rs.getString(1), true));
				} while (rs.next());
				Collections.sort(array, AccessManagerImpl.GROUP_DESC_COMPARATOR);
				array.add(this.executeGetGroup(conn, "def.guest", true));
				return array.toArray(new AccessGroup<?>[array.size()]);
			}
		}
	}

	final String executeLoginToGuid(final Connection conn, final String login) throws SQLException {

		try (final PreparedStatement ps = conn
				.prepareStatement("SELECT UserID FROM " + this.tablePrefix + "UserAccounts WHERE login=?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
			ps.setString(1, login);
			try (final ResultSet rs = ps.executeQuery()) {
				this.stsUserLoginsResolved++;
				return rs.next()
					? rs.getString(1)
					: null;
			}
		}
	}

	final UserProfileData getUserProfile(final String userId, final String name, final boolean fresh, final boolean create) {

		if (create) {
			final UserProfileData result = this.cacheUserProfile.get(
					userId, //
					name,
					userId,
					name,
					fresh
						? this.creatorUserProfileFreshCreate
						: this.creatorUserProfileTryCreate//
			);
			if (result == AccessManagerImpl.NULL_USER_PROFILE) {
				// do not put to cache - not yet commited!
				return new UserProfileData(userId, name, -1L);
			}
			return result;
		}
		// check cache in order to get _commited_ profile
		final UserProfileData result = fresh
			? this.cacheUserProfile.get(
					userId, //
					name)
			: this.cacheUserProfile.get(
					userId, //
					name,
					userId,
					name,
					this.creatorUserProfileTryLoad);
		return result == AccessManagerImpl.NULL_USER_PROFILE
			? null
			: result;
	}

	final void register() {

		final ExecProcess context = Exec.currentProcess();
		((StatusRegistry) context.baseGet(ProvideStatus.REGISTRY_CONTEXT_KEY, BaseObject.UNDEFINED).baseValue())
				.register(new UserManagerStatus(this, this.cacheUser, this.cacheUserProfile, this.cacheUserGroups, this.cacheUserLogin));
	}

	final void removing() {

		synchronized (this) {
			if (!this.stopped) {
				this.stopped = true;
				this.scheduledJobs.stop();
			}
		}
	}

	final void start() {

		Act.launchService(Exec.createProcess(null, "SEARCH: " + this.toString()), this.searchLoader);
	}

	final void statusFill(final StatusInfo data) {

		data.put("Users: Cache size", Format.Compact.toDecimal(this.cacheUser.size()));
		data.put("Users: loaded (hits)", Format.Compact.toDecimal(this.stsUsersLoaded));
		data.put("Users: loaded (misses)", Format.Compact.toDecimal(this.stsUsersCreated));
		data.put("Users: Updated", Format.Compact.toDecimal(this.stsUsersUpdated));
		data.put("Users: Inserted", Format.Compact.toDecimal(this.stsUsersInserted));
		data.put("Users: Commited", Format.Compact.toDecimal(this.stsUsersCommited));
		data.put("Users: Deleted", Format.Compact.toDecimal(this.stsUsersDeleted));
		data.put("Users: Login access updated", Format.Compact.toDecimal(this.stsUserAccessUpdated));
		data.put("Users: Login access multi updates", Format.Compact.toDecimal(this.stsUserAccessMultiUpdates));
		data.put("Users: Logins resolved", Format.Compact.toDecimal(this.stsUserLoginsResolved));
		data.put("Groups: Group list loaded", Format.Compact.toDecimal(this.stsGroupListLoaded));
		data.put("Groups: User list loaded", Format.Compact.toDecimal(this.stsGroupUsersLoaded));
		data.put("Groups: Deleted", Format.Compact.toDecimal(this.stsGroupsDeleted));
		data.put("UserGroups: Cache size", Format.Compact.toDecimal(this.cacheUserGroups.size()));
		data.put("UserGroups: User group list loaded", Format.Compact.toDecimal(this.stsUserGroupsLoaded));
		data.put("UserGroups: Updated", Format.Compact.toDecimal(this.stsGroupsUpdated));
		data.put("UserGroups: Inserted", Format.Compact.toDecimal(this.stsGroupsInserted));
		data.put("UserLogin: Cache size", Format.Compact.toDecimal(this.cacheUserLogin.size()));
		data.put("UserProfiles: Cache size", Format.Compact.toDecimal(this.cacheUserProfile.size()));
		data.put("UserProfiles: Loaded", Format.Compact.toDecimal(this.stsProfilesLoaded));
		data.put("UserProfiles: Updated", Format.Compact.toDecimal(this.stsProfilesUpdated));
		data.put("UserProfiles: Inserted", Format.Compact.toDecimal(this.stsProfilesInserted));
		data.put("UserProfiles: Commited", Format.Compact.toDecimal(this.stsProfilesCommited));
		data.put("UserProfiles: Access updated", Format.Compact.toDecimal(this.stsProfilesAccessUpdated));
		data.put("UserProfiles: Access multi updates", Format.Compact.toDecimal(this.stsProfilesAccessMultiUpdates));
		data.put("Security: Permission checks", Format.Compact.toDecimal(this.stsSecurityChecks));
		data.put("Security: Access checks passed", Format.Compact.toDecimal(this.stsSecurityGranted));
		data.put("Security: Access checks failed", Format.Compact.toDecimal(this.stsSecurityDenied));
		data.put("Security: ACL list loaded", Format.Compact.toDecimal(this.stsAclListLoaded));
		this.searchLoader.statusFill(data);
	}

	final void stop() {

		if (this.searchLoader != null) {
			this.searchLoader.stop();
			this.searchLoader = null;
		}
	}

	final void storeUserProfile(final String userId, final String name, final boolean fresh, final BaseObject data) {

		final UserProfileData profile = this.getUserProfile(userId, name, fresh, true);
		profile.setUserID(userId);
		profile.setName(name);
		profile.setData(data);
		this.stsProfilesCommited++;
		this.commit(profile);
	}
}
