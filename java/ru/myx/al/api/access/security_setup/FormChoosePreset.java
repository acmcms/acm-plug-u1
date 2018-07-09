/*
 * Created on 17.05.2004
 */
package ru.myx.al.api.access.security_setup;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import ru.myx.ae1.control.Control;
import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae3.access.AccessPermissions;
import ru.myx.ae3.access.AccessPreset;
import ru.myx.ae3.base.BaseArrayDynamic;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.control.AbstractForm;
import ru.myx.ae3.control.ControlBasic;
import ru.myx.ae3.control.ControlLookupStatic;
import ru.myx.ae3.control.command.ControlCommand;
import ru.myx.ae3.control.command.ControlCommandset;
import ru.myx.ae3.control.field.ControlFieldFactory;
import ru.myx.ae3.control.fieldset.ControlFieldset;
import ru.myx.ae3.help.Convert;
import ru.myx.al.api.access.AclObject;

/**
 * @author myx
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
class FormChoosePreset extends AbstractForm<FormChoosePreset> {
	private final ControlFieldset<?>					fieldset;
	
	private final String								path;
	
	private final AccessPermissions						available;
	
	private final BaseArrayDynamic<ControlBasic<?>>	groupList;
	
	private final int									index;
	
	private final AclObject.Entry						entry;
	
	private static final ControlCommand<?>				CMD_SAVE	= Control.createCommand( "ok", " OK " )
																			.setCommandPermission( "$modify_security" )
																			.setCommandIcon( "command-save-ok" );
	
	private static final ControlCommand<?>				CMD_CUSTOM	= Control
																			.createCommand( "custom",//
																					MultivariantString
																							.getString( "Custom",
																									Collections
																											.singletonMap( "ru",
																													"Права" ) ) )
																			.setCommandPermission( "$modify_security" )
																			.setCommandIcon( "command-next" );
	
	FormChoosePreset(final String path,
			final AccessPermissions available,
			final AclObject.Entry entry,
			final BaseArrayDynamic<ControlBasic<?>> groupList,
			final int index) {
		this.setAttributeIntern( "id", "choose_preset" );
		this.setAttributeIntern( "title",
				MultivariantString.getString( "Choose preset", Collections.singletonMap( "ru", "Выбор профиля" ) ) );
		this.setAttributeIntern( "path", path );
		this.recalculate();
		this.entry = entry;
		this.path = path;
		this.available = available;
		this.groupList = groupList;
		this.index = index;
		final ControlLookupStatic lookup = new ControlLookupStatic();
		final AccessPreset[] presets = available.getPresets();
		lookup.putAppend( "-2", //
				MultivariantString.getString( "No access", Collections.singletonMap( "ru", "Нет доступа" ) ) );
		lookup.putAppend( "-3", //
				MultivariantString.getString( "Full access", Collections.singletonMap( "ru", "Полный доступ" ) ) );
		for (int i = 0; i < presets.length; ++i) {
			lookup.putAppend( String.valueOf( i ), presets[i].getTitle() );
		}
		this.fieldset = ControlFieldset
				.createFieldset()
				.addField( ControlFieldFactory
						.createFieldString( "path",
								MultivariantString.getString( "Current path",
										Collections.singletonMap( "ru", "Текущий путь" ) ),
								path ).setConstant() )
				.addField( ControlFieldFactory
						.createFieldInteger( "preset",
								MultivariantString.getString( "Presets", Collections.singletonMap( "ru", "Профили" ) ),
								-1 ).setFieldType( "select" ).setFieldVariant( "bigselect" )
						.setAttribute( "lookup", lookup ) );
	}
	
	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) {
		if (command == FormChoosePreset.CMD_SAVE) {
			final int preset = Convert.MapEntry.toInt( this.getData(), "preset", 0 );
			if (preset == -1) {
				return MultivariantString.getString( "No value selected!",
						Collections.singletonMap( "ru", "Значение не выбрано!" ) );
			}
			final Set<String> permissions;
			if (preset == -2) {
				permissions = AccessPermissions.PERMISSIONS_NONE;
			} else //
			if (preset == -3) {
				permissions = AccessPermissions.PERMISSIONS_ALL;
			} else {
				final AccessPreset presetObject = this.available.getPresets()[preset];
				permissions = new TreeSet<>( Arrays.asList( presetObject.getPermissions() ) );
			}
			final AclObject.Entry newEntry = new AclObject.EntryDescriber( this.entry.getKey(),
					permissions,
					this.available );
			if (this.index == -1) {
				this.groupList.add( newEntry );
			} else {
				this.groupList.set( this.index, newEntry );
			}
			return null;
		}
		if (command == FormChoosePreset.CMD_CUSTOM) {
			final int preset = Convert.MapEntry.toInt( this.getData(), "preset", 0 );
			if (preset == -1) {
				return MultivariantString.getString( "No value selected!",
						Collections.singletonMap( "ru", "Значение не выбрано!" ) );
			}
			final Set<String> permissions;
			if (preset == -2) {
				permissions = AccessPermissions.PERMISSIONS_NONE;
			} else //
			if (preset == -3) {
				permissions = AccessPermissions.PERMISSIONS_ALL;
			} else {
				final AccessPreset presetObject = this.available.getPresets()[preset];
				permissions = new TreeSet<>( Arrays.asList( presetObject.getPermissions() ) );
			}
			final AclObject.Entry newEntry = new AclObject.EntryDescriber( this.entry.getKey(),
					permissions,
					this.available );
			return new FormChooseCustom( this.path, this.available, newEntry, this.groupList, this.index );
		}
		throw new IllegalArgumentException( "Unknown command: " + command.getKey() );
	}
	
	@Override
	public ControlCommandset getCommands() {
		final ControlCommandset result = Control.createOptions();
		result.add( FormChoosePreset.CMD_SAVE );
		result.add( FormChoosePreset.CMD_CUSTOM );
		return result;
	}
	
	@Override
	public ControlFieldset<?> getFieldset() {
		return this.fieldset;
	}
}
