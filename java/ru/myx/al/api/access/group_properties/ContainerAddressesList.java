/*
 * Created on 28.06.2004
 */
package ru.myx.al.api.access.group_properties;

import java.util.Collections;

import ru.myx.ae1.control.Control;
import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae3.base.BaseArrayDynamic;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.base.BasePrimitiveString;
import ru.myx.ae3.control.AbstractContainer;
import ru.myx.ae3.control.command.ControlCommand;
import ru.myx.ae3.control.command.ControlCommandset;
import ru.myx.ae3.help.Convert;

/**
 * @author myx
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
class ContainerAddressesList extends AbstractContainer<ContainerAddressesList> {
	private final BaseArrayDynamic<BasePrimitiveString>	exclusions;
	
	private static final ControlCommand<?>					CMD_ADD	= Control.createCommand( "add",
																			MultivariantString.getString( "Add",
																					Collections.singletonMap( "ru",
																							"Добавить" ) ) )
																			.setCommandIcon( "command-add" );
	
	ContainerAddressesList(final BaseArrayDynamic<BasePrimitiveString> exclusions) {
		this.exclusions = exclusions;
	}
	
	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) throws Exception {
		
		if (command == ContainerAddressesList.CMD_ADD) {
			return new FormAddExcludeAddress( this.exclusions );
		}
		if ("remove".equals( command.getKey() )) {
			final int index = Convert.MapEntry.toInt( command.getAttributes(), "index", -1 );
			if (index >= 0) {
				this.exclusions.remove( index );
				return null;
			}
			return "Item: " + command.getAttributes().baseGet( "key", BaseObject.UNDEFINED ) + " not found!";
		}
		return super.getCommandResult( command, arguments );
	}
	
	@Override
	public ControlCommandset getCommands() {
		return Control.createOptionsSingleton( ContainerAddressesList.CMD_ADD );
	}
	
	@Override
	public ControlCommandset getContentCommands(final String key) {
		return Control.createOptionsSingleton( Control.createCommand( "remove",
				MultivariantString.getString( "Remove", Collections.singletonMap( "ru", "Удалить" ) ) )
				.setAttribute( "index", key ) );
	}
}
