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
import ru.myx.ae3.control.AbstractForm;
import ru.myx.ae3.control.command.ControlCommand;
import ru.myx.ae3.control.command.ControlCommandset;
import ru.myx.ae3.control.field.ControlFieldFactory;
import ru.myx.ae3.control.fieldset.ControlFieldset;

/**
 * @author myx
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
class FormAddExcludeAddress extends AbstractForm<FormAddExcludeAddress> {
	private static final ControlFieldset<?>					FIELDSET	= ControlFieldset
																				.createFieldset()
																				.addField( ControlFieldFactory
																						.createFieldString( "exclusion",
																								MultivariantString
																										.getString( "Address",
																												Collections
																														.singletonMap( "ru",
																																"Адрес" ) ),
																								"" )
																						.setFieldHint( MultivariantString
																								.getString( "Enter exclusion address here, use '*' character at the beginning or at the end of address to specify suffix or prefix.\r\nExample:\r\n\t127.0.0.1\r\n\t127.*\r\n\t*@microsoft.com",
																										Collections
																												.singletonMap( "ru",
																														"Ведите адрес для исключения из группы, используйте символ '*' в начале или в конце строки чтобы указать суффикс или преффикс.\r\nПримеры:\r\n\t127.0.0.1\r\n\t127.*\r\n\t*@microsoft.com" ) ) ) );
	
	private final BaseArrayDynamic<BasePrimitiveString>	exclusions;
	
	private static final ControlCommand<?>					CMD_OK		= Control.createCommand( "ok", " OK " )
																				.setCommandIcon( "command-add" );
	
	FormAddExcludeAddress(final BaseArrayDynamic<BasePrimitiveString> exclusions) {
		this.exclusions = exclusions;
		this.setAttributeIntern( "id", "new_exclude" );
		this.setAttributeIntern( "title",
				MultivariantString.getString( "Exclude: address addition",
						Collections.singletonMap( "ru", "Исключения: добавление адреса" ) ) );
		this.recalculate();
	}
	
	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) throws Exception {
		if (command == FormAddExcludeAddress.CMD_OK) {
			final BaseObject exclusion = this.getData().baseGet( "exclusion", BaseObject.UNDEFINED );
			assert exclusion != null : "NULL java value";
			if (exclusion != BaseObject.UNDEFINED) {
				final BasePrimitiveString string = exclusion.baseToString();
				if (!this.exclusions.contains( string )) {
					this.exclusions.add( string );
				}
			}
			return null;
		}
		return super.getCommandResult( command, arguments );
	}
	
	@Override
	public ControlCommandset getCommands() {
		return Control.createOptionsSingleton( FormAddExcludeAddress.CMD_OK );
	}
	
	@Override
	public ControlFieldset<?> getFieldset() {
		return FormAddExcludeAddress.FIELDSET;
	}
}
