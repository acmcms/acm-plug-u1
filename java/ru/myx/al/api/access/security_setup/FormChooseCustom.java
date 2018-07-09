/*
 * Created on 17.05.2004
 */
package ru.myx.al.api.access.security_setup;

import java.util.Collections;
import java.util.Set;

import ru.myx.ae1.control.Control;
import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae3.access.AccessPermission;
import ru.myx.ae3.access.AccessPermissions;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseArray;
import ru.myx.ae3.base.BaseArrayDynamic;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.control.AbstractForm;
import ru.myx.ae3.control.ControlBasic;
import ru.myx.ae3.control.ControlLookupStatic;
import ru.myx.ae3.control.command.ControlCommand;
import ru.myx.ae3.control.command.ControlCommandset;
import ru.myx.ae3.control.field.ControlFieldFactory;
import ru.myx.ae3.control.fieldset.ControlFieldset;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.help.Create;
import ru.myx.al.api.access.AclObject;

/**
 * @author myx
 * 		
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
class FormChooseCustom extends AbstractForm<FormChooseCustom> {
	
	private final ControlFieldset<?> fieldset;
	
	private final AccessPermissions available;
	
	private final BaseArrayDynamic<ControlBasic<?>> groupList;
	
	private final int index;
	
	private final AclObject.Entry entry;
	
	private static final ControlCommand<?> CMD_SAVE = Control.createCommand("ok", " OK ").setCommandPermission("$modify_security").setCommandIcon("command-save-ok");
	
	FormChooseCustom(final String path, final AccessPermissions available, final AclObject.Entry entry, final BaseArrayDynamic<ControlBasic<?>> groupList, final int index) {
		this.setAttributeIntern("id", "choose_custom");
		this.setAttributeIntern("title", MultivariantString.getString("Custom permissions", Collections.singletonMap("ru", "Настройка прав")));
		this.setAttributeIntern("path", path);
		this.recalculate();
		this.entry = entry;
		this.available = available;
		this.groupList = groupList;
		this.index = index;
		final ControlLookupStatic lookup = new ControlLookupStatic();
		final AccessPermission[] permissions = available.getAllPermissions();
		for (final AccessPermission permission : permissions) {
			lookup.putAppend(permission.getKey(), permission.getTitle());
		}
		this.fieldset = ControlFieldset.createFieldset().addField(
				ControlFieldFactory.createFieldString("path", MultivariantString.getString("Current path", Collections.singletonMap("ru", "Текущий путь")), path).setConstant())
				.addField(
						ControlFieldFactory.createFieldSet("set", MultivariantString.getString("Permissions", Collections.singletonMap("ru", "Права")), entry.getPermissions())
								.setFieldVariant("select").setAttribute("lookup", lookup));
		this.setData(new BaseNativeObject("set", Base.forArray(entry.getPermissions().toArray())));
	}
	
	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) {
		
		if (command == FormChooseCustom.CMD_SAVE) {
			final BaseArray set = Convert.MapEntry.toCollection(this.getData(), "set", null);
			final Set<String> permissions;
			if (set == null || set.isEmpty()) {
				permissions = AccessPermissions.PERMISSIONS_NONE;
			} else {
				permissions = Create.tempSet();
				final int length = set.length();
				for (int i = 0; i < length; ++i) {
					permissions.add(set.baseGet(i, BaseObject.UNDEFINED).baseToJavaString());
				}
			}
			final AclObject.Entry newEntry = new AclObject.EntryDescriber(this.entry.getKey(), permissions, this.available);
			if (this.index == -1) {
				this.groupList.add(newEntry);
			} else {
				this.groupList.set(this.index, newEntry);
			}
			return null;
		}
		throw new IllegalArgumentException("Unknown command: " + command.getKey());
	}
	
	@Override
	public ControlCommandset getCommands() {
		
		return Control.createOptionsSingleton(FormChooseCustom.CMD_SAVE);
	}
	
	@Override
	public ControlFieldset<?> getFieldset() {
		
		return this.fieldset;
	}
}
