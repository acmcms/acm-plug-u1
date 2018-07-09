/*
 * Created on 28.06.2004
 */
package ru.myx.al.api.access.group_properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.myx.ae1.access.AccessManager;
import ru.myx.ae1.access.AccessUser;
import ru.myx.ae1.control.Control;
import ru.myx.ae1.control.MultivariantString;
import java.util.function.Function;
import ru.myx.ae3.act.Context;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseArrayDynamic;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.base.BasePrimitiveString;
import ru.myx.ae3.control.AbstractContainer;
import ru.myx.ae3.control.command.ControlCommand;
import ru.myx.ae3.control.command.ControlCommandset;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.help.Convert;

/** @author myx */
class ContainerUserList extends AbstractContainer<ContainerUserList> {

	private static final ControlCommand<?> CMD_ADD = Control.createCommand("add", MultivariantString.getString("Add", Collections.singletonMap("ru", "Добавить")))
			.setCommandIcon("command-add");

	private final BaseArrayDynamic<BasePrimitiveString> exclusions;

	ContainerUserList(final BaseArrayDynamic<BasePrimitiveString> exclusions) {
		this.exclusions = exclusions;
	}

	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) throws Exception {

		if (command == ContainerUserList.CMD_ADD) {
			final AccessManager manager = Context.getServer(Exec.currentProcess()).getAccessManager();
			final BaseArrayDynamic<BasePrimitiveString> exclusions = this.exclusions;
			final List<AccessUser<?>> users = new ArrayList<>();
			final int length = exclusions.length();
			for (int i = 0; i < length; ++i) {
				final AccessUser<?> user = manager.getUser(exclusions.baseGet(i, BaseObject.UNDEFINED).baseToJavaString(), false);
				if (user != null) {
					users.add(user);
				}
			}
			return manager.createFormUsersSelection(null, users.toArray(new AccessUser<?>[users.size()]), new Function<AccessUser<?>[], Object>() {

				@Override
				public Object apply(AccessUser<?>[] users1) {

					exclusions.baseClear();
					if (users1 != null && users1.length > 0) {
						for (int i = users1.length - 1; i >= 0; --i) {
							exclusions.add(Base.forString(users1[i].getKey()));
						}
					}
					return null;
				}
			});
		}
		if ("remove".equals(command.getKey())) {
			final int index = Convert.MapEntry.toInt(command.getAttributes(), "index", -1);
			if (index >= 0) {
				this.exclusions.remove(index);
				return null;
			}
			return "Item: " + command.getAttributes().baseGet("key", BaseObject.UNDEFINED) + " not found!";
		}
		return super.getCommandResult(command, arguments);
	}

	@Override
	public ControlCommandset getCommands() {

		return Control.createOptionsSingleton(ContainerUserList.CMD_ADD);
	}

	@Override
	public ControlCommandset getContentCommands(final String key) {

		return Control.createOptionsSingleton(
				Control.createCommand("remove", MultivariantString.getString("Remove", Collections.singletonMap("ru", "Удалить"))).setAttribute("index", key));
	}
}
