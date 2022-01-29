/*
 * Created on 14.05.2004
 */
package ru.myx.al.api.access.security_setup;

import java.util.Collections;
import java.util.function.Function;

import ru.myx.ae1.access.AccessManager;
import ru.myx.ae1.access.AccessUser;
import ru.myx.ae1.control.Control;
import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae3.access.AccessPermission;
import ru.myx.ae3.access.AccessPermissions;
import ru.myx.ae3.access.AccessPreset;
import ru.myx.ae3.access.AccessPrincipal;
import ru.myx.ae3.act.Context;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseArrayDynamic;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.control.AbstractContainer;
import ru.myx.ae3.control.ControlBasic;
import ru.myx.ae3.control.command.ControlCommand;
import ru.myx.ae3.control.command.ControlCommandset;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.help.Convert;
import ru.myx.al.api.access.AclObject;

class ContainerGroupList extends AbstractContainer<ContainerGroupList> {
	
	private static final ControlCommand<?> CMD_ADD_GROUP = Control
			.createCommand("addgrp", MultivariantString.getString("Add group(s)", Collections.singletonMap("ru", "Добавление групп(ы)"))).setCommandPermission("$modify_security")
			.setCommandIcon("command-add-group");
	
	private static final ControlCommand<?> CMD_ADD_USER = Control
			.createCommand("addusr", MultivariantString.getString("Add users(s)", Collections.singletonMap("ru", "Добавление пользовател(я/ей)")))
			.setCommandPermission("$modify_security").setCommandIcon("command-add-user");
	
	private static final ControlCommand<?> CMD_CLEAR_ALL = Control.createCommand("clearall", MultivariantString.getString("Clear", Collections.singletonMap("ru", "Очистить")))
			.setCommandPermission("$modify_security").setCommandIcon("command-dispose");
	
	private final AccessPermissions available;
	
	private final String path;
	
	private final BaseArrayDynamic<ControlBasic<?>> groupList;
	
	ContainerGroupList(final AccessPermissions available, final String path, final BaseArrayDynamic<ControlBasic<?>> groupList) {
		
		this.available = available;
		this.path = path;
		this.groupList = groupList;
		final AccessManager manager = Context.getServer(Exec.currentProcess()).getAccessManager();
		for (int i = groupList.length() - 1; i >= 0; --i) {
			final ControlBasic<?> record = (ControlBasic<?>) groupList.baseGet(i, null);
			if (manager.getGroup(record.getKey(), false) == null) {
				final AccessUser<?> user = manager.getUser(record.getKey(), true);
				if (user.getCreated() <= 0) {
					groupList.baseRemove(i);
				}
			}
		}
	}
	
	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) throws Exception {
		
		if (command == ContainerGroupList.CMD_ADD_GROUP) {
			return new FormAddGroup(this.path, this.groupList, this.available);
		}
		if (command == ContainerGroupList.CMD_ADD_USER) {
			final AccessManager manager = Context.getServer(Exec.currentProcess()).getAccessManager();
			final BaseArrayDynamic<ControlBasic<?>> groupList = this.groupList;
			final String path = this.path;
			final AccessPermissions available = this.available;
			return manager.createFormUsersSelection(null, new AccessUser<?>[0], new Function<AccessUser<?>[], Object>() {
				
				@Override
				public Object apply(final AccessUser<?>[] selected) {

					if (selected != null && selected.length > 0) {
						for (final AccessUser<?> element : selected) {
							groupList.add(new AclObject.EntryDescriber(element.getKey(), manager.securityGetPermissionsEffective(element, path), available));
						}
					}
					return null;
				}
			});
		}
		if (command == ContainerGroupList.CMD_CLEAR_ALL) {
			this.groupList.baseClear();
			return null;
		}
		final String key = Base.getString(command.getAttributes(), "key", "");
		final int index = this.getIndex(key);
		if ("tofa".equals(command.getKey())) {
			final AclObject.Entry record = (AclObject.Entry) this.groupList.baseGet(index, BaseObject.UNDEFINED);
			this.groupList.baseSet(
					index, //
					new AclObject.EntryDescriber(
							record.getKey(), //
							AccessPermissions.PERMISSIONS_ALL,
							this.available));
			return null;
		}
		if ("tona".equals(command.getKey())) {
			final AclObject.Entry record = (AclObject.Entry) this.groupList.baseGet(index, BaseObject.UNDEFINED);
			this.groupList.baseSet(
					index, //
					new AclObject.EntryDescriber(
							record.getKey(), //
							AccessPermissions.PERMISSIONS_NONE,
							this.available));
			return null;
		}
		if ("tops".equals(command.getKey())) {
			return new FormChoosePreset(
					this.path, //
					this.available,
					(AclObject.Entry) this.groupList.baseGet(index, BaseObject.UNDEFINED),
					this.groupList,
					index);
		}
		if ("tocs".equals(command.getKey())) {
			return new FormChooseCustom(
					this.path, //
					this.available,
					(AclObject.Entry) this.groupList.baseGet(index, BaseObject.UNDEFINED),
					this.groupList,
					index);
		}
		if ("dele".equals(command.getKey())) {
			this.groupList.baseRemove(index);
			return null;
		}
		return super.getCommandResult(command, arguments);
	}
	
	@Override
	public ControlCommandset getCommands() {
		
		final ControlCommandset result = Control.createOptions();
		// if(groupList.size() <
		// Know.currentServer().getAccessManager().getAllGroups().length){
		// result.addCommand(CMD_ADD_GROUP);
		// }
		result.add(ContainerGroupList.CMD_ADD_GROUP);
		result.add(ContainerGroupList.CMD_ADD_USER);
		if (this.groupList.length() > 0) {
			result.add(ContainerGroupList.CMD_CLEAR_ALL);
		}
		return result;
	}
	
	@Override
	public ControlCommandset getContentCommands(final String key) {
		
		final int index = this.getIndex(key);
		final ControlCommandset result = Control.createOptions();
		final AclObject.Entry record = (AclObject.Entry) this.groupList.baseGet(index, BaseObject.UNDEFINED);
		if (record.getPermissions() != AccessPermissions.PERMISSIONS_ALL) {
			result.add(
					Control.createCommand(
							"tofa", //
							MultivariantString.getString(
									"Full Access", //
									Collections.singletonMap("ru", "Полный")))//
							.setCommandIcon("command-grant")//
							.setAttribute("key", key));
		}
		if (record.getPermissions() != AccessPermissions.PERMISSIONS_NONE) {
			result.add(
					Control.createCommand(
							"tona", //
							MultivariantString.getString(
									"No Access", //
									Collections.singletonMap("ru", "Нет доступа")))//
							.setCommandIcon("command-deny")//
							.setAttribute("key", key));
		}
		if (this.available != null) {
			final AccessPreset[] presets = this.available.getPresets();
			if (presets != null && presets.length > 0) {
				result.add(
						Control.createCommand(
								"tops", //
								MultivariantString.getString(
										"Preset...", //
										Collections.singletonMap("ru", "Профиль...")))//
								.setCommandIcon("command-setup-preset")//
								.setAttribute("key", key));
			}
			final AccessPermission[] permissions = this.available.getAllPermissions();
			if (permissions != null && permissions.length > 0) {
				result.add(
						Control.createCommand(
								"tocs", //
								MultivariantString.getString(
										"Custom...", //
										Collections.singletonMap("ru", "Настройка...")))//
								.setCommandIcon("command-setup-custom")//
								.setAttribute("key", key));
			}
		}
		result.add(
				Control.createCommand(
						"dele", //
						MultivariantString.getString(
								"Delete", //
								Collections.singletonMap("ru", "Удалить")))//
						.setCommandIcon("command-delete")//
						.setAttribute("key", key));
		return result;
	}
	
	private int getIndex(final String key) {
		
		final int intIndex = Convert.Any.toInt(key, -1);
		if (intIndex == -1) {
			final int length = this.groupList.length();
			for (int i = 0; i < length; ++i) {
				final BaseObject groupObject = this.groupList.baseGet(i, BaseObject.UNDEFINED);
				if (groupObject instanceof AccessPrincipal<?>) {
					if (key.equals(((AccessPrincipal<?>) groupObject).getKey())) {
						return i;
					}
				} else //
				if (groupObject instanceof ControlBasic<?>) {
					if (key.equals(((ControlBasic<?>) groupObject).getKey())) {
						return i;
					}
				} else {
					if (key.equals(groupObject.baseToJavaString())) {
						return i;
					}
				}
			}
			return -1;
		}
		return intIndex;
	}
}
