/*
 * Created on 28.04.2004
 */
package ru.myx.al.api.access.security_setup;

import java.util.Collections;

import ru.myx.ae1.access.AccessGroup;
import ru.myx.ae1.access.AccessManager;
import ru.myx.ae1.control.Control;
import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae3.access.AccessPermissions;
import ru.myx.ae3.act.Context;
import ru.myx.ae3.base.BaseArray;
import ru.myx.ae3.base.BaseArrayDynamic;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.control.AbstractForm;
import ru.myx.ae3.control.ControlBasic;
import ru.myx.ae3.control.ControlLookupStatic;
import ru.myx.ae3.control.command.ControlCommand;
import ru.myx.ae3.control.command.ControlCommandset;
import ru.myx.ae3.control.field.ControlFieldFactory;
import ru.myx.ae3.control.fieldset.ControlFieldset;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.help.Create;
import ru.myx.al.api.access.AclObject;

/**
 * @author myx
 * 		
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
class FormAddGroup extends AbstractForm<FormAddGroup> {
	
	private static final String buildTitle(final AccessGroup<?> group) {
		
		if (group == null) {
			return "-= error: null =-";
		}
		final String title = group.getTitle();
		final String description = group.getDescription();
		if (description != null && description.length() > 0) {
			return title + " (" + description + ')';
		}
		return title;
	}
	
	private final ControlFieldset<?> fieldset;
	
	private final String path;
	
	private final BaseArrayDynamic<ControlBasic<?>> groupList;
	
	private final AccessPermissions available;
	
	private static final ControlCommand<?> CMD_SAVE = Control.createCommand("ok", " OK ").setCommandPermission("$modify_security").setCommandIcon("command-save-ok");
	
	FormAddGroup(final String path, final BaseArrayDynamic<ControlBasic<?>> groupList, final AccessPermissions available) {
		this.setAttributeIntern("id", "add_group");
		this.setAttributeIntern(
				"title", //
				MultivariantString.getString("Add group(s)", Collections.singletonMap("ru", "Добавление групп(ы)")));
		this.setAttributeIntern("path", path);
		this.recalculate();
		this.available = available;
		final ControlLookupStatic groupsLookup = new ControlLookupStatic();
		final AccessGroup<?>[] groups = Context.getServer(Exec.currentProcess()).getAccessManager().getAllGroups();
		for (final AccessGroup<?> group : groups) {
			boolean found = false;
			for (int j = groupList.length() - 1; j >= 0; j--) {
				final ControlBasic<?> record = (ControlBasic<?>) groupList.baseGet(j, null);
				if (group.getKey().equals(record.getKey())) {
					found = true;
					break;
				}
			}
			if (!found) {
				groupsLookup.putAppend(group.getKey(), FormAddGroup.buildTitle(group));
			}
		}
		this.fieldset = ControlFieldset.createFieldset()//
				.addField(ControlFieldFactory.createFieldString(
						"path", //
						MultivariantString.getString(
								"Current path", //
								Collections.singletonMap("ru", "Текущий путь")),
						path).setConstant())//
				.addField(
						ControlFieldFactory
								.createFieldSet(
										"groups", //
										MultivariantString.getString(
												"Groups", //
												Collections.singletonMap("ru", "Группы")),
										Create.tempSet())//
								.setFieldVariant("select")//
								.setAttribute("lookup", groupsLookup));
		this.path = path;
		this.groupList = groupList;
	}
	
	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) {
		
		if (command == FormAddGroup.CMD_SAVE) {
			final BaseArray set = Convert.MapEntry.toCollection(this.getData(), "groups", null);
			if (set != null) {
				final AccessManager manager = Context.getServer(Exec.currentProcess()).getAccessManager();
				final int length = set.length();
				for (int i = 0; i < length; ++i) {
					final String id = set.baseGet(i, BaseObject.UNDEFINED).baseToJavaString();
					final AccessGroup<?> group = manager.getGroup(id, true);
					this.groupList.add(new AclObject.EntryDescriber(id, manager.securityGetPermissionsEffective(group, this.path), this.available));
				}
			}
			return null;
		}
		throw new IllegalArgumentException("Unknown command: " + command.getKey());
	}
	
	@Override
	public ControlCommandset getCommands() {
		
		return Control.createOptionsSingleton(FormAddGroup.CMD_SAVE);
	}
	
	@Override
	public ControlFieldset<?> getFieldset() {
		
		return this.fieldset;
	}
}
