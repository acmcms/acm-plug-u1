/*
 * Created on 14.05.2004
 */
package ru.myx.al.api.access;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import ru.myx.ae1.access.AccessGroup;
import ru.myx.ae1.access.AccessManager;
import ru.myx.ae1.access.AccessUser;
import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae3.access.AccessPermission;
import ru.myx.ae3.access.AccessPermissions;
import ru.myx.ae3.access.AccessPreset;
import ru.myx.ae3.act.Context;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.control.AbstractBasic;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.help.Create;

/**
 * @author myx
 * 		
 */
public class AclObject {
	
	/**
	 * @author myx
	 * 		
	 */
	public static class Entry extends AbstractBasic<Entry> {
		
		private final String principalId;
		
		private final Set<String> permissions;
		
		private final AccessPermission[] nodePermissions;
		
		private int hasControl = -1;
		
		/**
		 * @param principalId
		 * @param permissions
		 * @param nodePermissions
		 */
		public Entry(final String principalId, final Set<String> permissions, final AccessPermissions nodePermissions) {
			
			this.principalId = principalId;
			this.permissions = permissions;
			this.nodePermissions = nodePermissions == null
				? null
				: nodePermissions.getAllPermissions();
		}
		
		@Override
		public BaseObject getData() {
			
			final AccessGroup<?> group = Context.getServer(Exec.currentProcess()).getAccessManager().getGroup(this.principalId, true);
			final BaseObject data = new BaseNativeObject()//
					.putAppend("title", group.getTitle())//
					.putAppend(
							"description", //
							this.permissions == AccessPermissions.PERMISSIONS_ALL
								? "Full access"
								: this.permissions == AccessPermissions.PERMISSIONS_NONE
									? "No access"
									: "preset or custom")//
									;
			return data;
		}
		
		/**
		 * @param available
		 * @return entry
		 */
		public AclObject.Entry getDescriberEntry(final AccessPermissions available) {
			
			return new EntryDescriber(this.principalId, this.permissions, available);
		}
		
		/**
		 * @author myx
		 * 		
		 */
		
		@Override
		public String getKey() {
			
			return this.principalId;
		}
		
		/**
		 * @return set
		 */
		public Set<String> getPermissions() {
			
			return this.permissions;
		}
		
		/**
		 * @return boolean
		 */
		public boolean hasPermissionsControl() {
			
			if (this.hasControl == -1) {
				if (this.permissions == AccessPermissions.PERMISSIONS_ALL) {
					this.hasControl = 1;
				} else //
				if (this.permissions == AccessPermissions.PERMISSIONS_NONE) {
					this.hasControl = 0;
				} else {
					final int hasControl = 0;
					if (this.nodePermissions != null) {
						for (final String permName : this.permissions) {
							for (int j = this.nodePermissions.length - 1; j >= 0; j--) {
								if (permName.equals(this.nodePermissions[j].getKey())) {
									if (this.nodePermissions[j].isForControl()) {
										this.hasControl = 1;
										return true;
									}
									break;
								}
							}
						}
					}
					this.hasControl = hasControl;
				}
			}
			return this.hasControl > 0;
		}
	}
	
	/**
	 * @author myx
	 * 		
	 */
	public static class EntryDescriber extends AclObject.Entry {
		
		private static final Object ACCESS_FULL = MultivariantString.getString("Full access", Collections.singletonMap("ru", "Полный доступ"));
		
		private static final Object ACCESS_NONE = MultivariantString.getString("No access", Collections.singletonMap("ru", "Нет доступа"));
		
		private static final Object ACCESS_PRESET = MultivariantString.getString("Preset", Collections.singletonMap("ru", "Профиль"));
		
		private static final Object ACCESS_CUSTOM = MultivariantString.getString("Custom", Collections.singletonMap("ru", "Настройка"));
		
		private final String principalId;
		
		private final Set<String> permissions;
		
		private final AccessPermissions available;
		
		/**
		 * @param principalId
		 * @param permissions
		 * @param available
		 */
		public EntryDescriber(final String principalId, final Set<String> permissions, final AccessPermissions available) {
			
			super(principalId, permissions, available);
			this.principalId = principalId;
			this.permissions = permissions;
			this.available = available;
		}
		
		@Override
		public BaseObject getData() {
			
			final AccessManager manager = Context.getServer(Exec.currentProcess()).getAccessManager();
			final AccessUser<?> user = manager.getUser(this.principalId, true);
			final BaseObject data = new BaseNativeObject();
			if (user.getCreated() > 0) {
				data.baseDefine("title", (BaseObject) MultivariantString.getString("User: " + user.getLogin(), Collections.singletonMap("ru", "Польз.: " + user.getLogin())));
			} else {
				final AccessGroup<?> group = manager.getGroup(this.principalId, true);
				data.baseDefine("title", (BaseObject) MultivariantString.getString("Group: " + group.getTitle(), Collections.singletonMap("ru", "Группа: " + group.getTitle())));
			}
			final String description;
			if (this.permissions == AccessPermissions.PERMISSIONS_ALL) {
				description = EntryDescriber.ACCESS_FULL.toString();
			} else //
			if (this.permissions == AccessPermissions.PERMISSIONS_NONE || this.permissions.isEmpty()) {
				description = EntryDescriber.ACCESS_NONE.toString();
			} else {
				description = this.getSettingDescription();
			}
			data.baseDefine("description", description);
			return data;
		}
		
		@Override
		public AclObject.Entry getDescriberEntry(final AccessPermissions available) {
			
			return new EntryDescriber(this.principalId, this.permissions, available);
		}
		
		@Override
		public String getKey() {
			
			return this.principalId;
		}
		
		@Override
		public Set<String> getPermissions() {
			
			return this.permissions;
		}
		
		private final String getSettingDescription() {
			
			if (this.available == null) {
				return EntryDescriber.ACCESS_CUSTOM.toString();
			}
			final AccessPreset[] presets = this.available.getPresets();
			if (presets != null && presets.length > 0) {
				for (int i = presets.length - 1; i >= 0; --i) {
					final AccessPreset preset = presets[i];
					final Collection<String> presetPermissions;
					if (preset.getPermissions() == null) {
						presetPermissions = Collections.emptyList();
					} else {
						presetPermissions = Arrays.asList(preset.getPermissions());
					}
					if (this.permissions.containsAll(presetPermissions) && presetPermissions.containsAll(this.permissions)) {
						return EntryDescriber.ACCESS_PRESET + ": " + preset.getTitle();
					}
				}
			}
			final StringBuilder result = new StringBuilder().append(EntryDescriber.ACCESS_CUSTOM).append(": ");
			final AccessPermission[] nodePermissions = this.available.getAllPermissions();
			boolean first = true;
			for (final AccessPermission element : nodePermissions) {
				if (this.permissions.contains(element.getKey())) {
					if (first) {
						result.append(element.getTitle());
						first = false;
					} else {
						result.append(", ").append(element.getTitle());
					}
				}
			}
			return result.toString();
		}
		
		@Override
		public String toString() {
			
			return "[object " + this.baseClass() + "(" + "id=" + this.principalId + ")]";
		}
		
	}
	
	private static final AclObject.Entry NULL_ENTRY = new AclObject.Entry(null, null, null);
	
	private final String path;
	
	private final boolean inherit;
	
	private AclObject parent;
	
	private Map<String, AclObject.Entry> settingsMap;
	
	private Map<String, AclObject.Entry> settingsCache;
	
	private AclObject.Entry[] settings;
	
	/**
	 * @param path
	 * @param inherit
	 */
	public AclObject(final String path, final boolean inherit) {
		
		this.path = path;
		this.inherit = inherit;
	}
	
	/**
	 * @param entry
	 */
	public void addSetting(final AclObject.Entry entry) {
		
		synchronized (this) {
			if (this.settingsMap == null) {
				this.settingsMap = Create.tempMap();
				this.settingsCache = Create.tempMap();
				if (this.settings != null) {
					for (int i = this.settings.length - 1; i >= 0; --i) {
						this.settingsMap.put(this.settings[i].getKey(), this.settings[i]);
						this.settingsCache.put(this.settings[i].getKey(), this.settings[i]);
					}
				}
			}
			this.settings = null;
			this.settingsMap.put(entry.getKey(), entry);
			this.settingsCache.put(entry.getKey(), entry);
		}
	}
	
	void fillPrincipals(final String permission, final Set<String> target) {
		
		if (this.settings != null) {
			for (int i = this.settings.length - 1; i >= 0; --i) {
				final AclObject.Entry entry = this.settings[i];
				if (entry.getPermissions().contains(permission)) {
					target.add(entry.getKey());
				}
			}
		}
		if (this.inherit && this.parent != null) {
			this.parent.fillPrincipals(permission, target);
		}
	}
	
	AclObject getParent() {
		
		return this.parent;
	}
	
	/**
	 * @return string
	 */
	public String getPath() {
		
		return this.path;
	}
	
	Set<String> getPermissionsEffective(final String principalKey) {
		
		final AclObject.Entry entry = this.getSettingsEffective(principalKey);
		return entry == null
			? AccessPermissions.PERMISSIONS_NONE
			: entry.getPermissions();
	}
	
	/**
	 * @return all entries
	 */
	public AclObject.Entry[] getSettings() {
		
		synchronized (this) {
			if (this.settings != null) {
				return this.settings;
			}
			if (this.settingsMap == null) {
				return this.settings = new AclObject.Entry[0];
			}
			return this.settings = this.settingsMap.values().toArray(new AclObject.Entry[this.settingsMap.size()]);
		}
	}
	
	AclObject.Entry getSettingsEffective(final String principalKey) {
		
		if (this.settingsMap == null) {
			this.settingsMap = Create.tempMap();
			this.settingsCache = Create.tempMap();
			if (this.settings != null) {
				for (int i = this.settings.length - 1; i >= 0; --i) {
					this.settingsMap.put(this.settings[i].getKey(), this.settings[i]);
					this.settingsCache.put(this.settings[i].getKey(), this.settings[i]);
				}
			}
		}
		final Object entry = this.settingsCache.get(principalKey);
		if (entry == null && this.parent != null) {
			if (this.inherit) {
				final AclObject.Entry parental = this.parent.getSettingsEffective(principalKey);
				this.settingsCache.put(principalKey, parental == null
					? AclObject.NULL_ENTRY
					: parental);
				return parental;
			}
			return null;
		}
		return entry == AclObject.NULL_ENTRY
			? null
			: (AclObject.Entry) entry;
	}
	
	boolean hasPermissionsEffectiveControl(final String principalKey) {
		
		final AclObject.Entry entry = this.getSettingsEffective(principalKey);
		return entry == null
			? false
			: entry.hasPermissionsControl();
	}
	
	/**
	 * @return boolean
	 */
	public boolean isInherit() {
		
		return this.inherit;
	}
	
	void setParent(final AclObject parent) {
		
		this.parent = parent;
	}
	
	@Override
	public String toString() {
		
		return "ACL{path=" + this.path + ", parent=" + this.parent + ", entries=" + this.settingsMap + "}";
	}
}
