/*
 * Created on 28.04.2004
 */
package ru.myx.al.api.access.security_setup;

import java.util.Collections;

import ru.myx.ae1.access.Access;
import ru.myx.ae1.control.Control;
import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae3.access.AccessPermissions;
import ru.myx.ae3.act.Context;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseArrayDynamic;
import ru.myx.ae3.base.BaseFunctionActAbstract;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.control.AbstractForm;
import ru.myx.ae3.control.ControlActor;
import ru.myx.ae3.control.ControlBasic;
import ru.myx.ae3.control.command.ControlCommand;
import ru.myx.ae3.control.command.ControlCommandset;
import ru.myx.ae3.control.field.ControlFieldFactory;
import ru.myx.ae3.control.fieldset.ControlFieldset;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.help.Convert;
import ru.myx.al.api.access.AccessManagerImpl;
import ru.myx.al.api.access.AclObject;
import ru.myx.al.api.access.AclObject.Entry;

/**
 * @author myx
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class FormFolderSecurity extends AbstractForm<FormFolderSecurity> {
	private static final ControlFieldset<?>				FIELDSET_GROUP_LIST	= ControlFieldset
																					.createFieldset()
																					.addField( ControlFieldFactory
																							.createFieldString( "title",
																									MultivariantString
																											.getString( "Group / User",
																													Collections
																															.singletonMap( "ru",
																																	"Группа / Пользователь" ) ),
																									"" ).setConstant() )
																					.addField( ControlFieldFactory
																							.createFieldString( "description",
																									MultivariantString
																											.getString( "Description",
																													Collections
																															.singletonMap( "ru",
																																	"Описание" ) ),
																									"" ).setConstant() );
	
	private static final ControlCommand<?>				CMD_SAVE			= Control
																					.createCommand( "ok", " OK " )
																					.setCommandPermission( "$modify_security" )
																					.setCommandIcon( "command-save-ok" );
	
	private static final ControlCommand<?>				CMD_APPLY			= Control
																					.createCommand( "apply",
																							MultivariantString
																									.getString( "Apply",
																											Collections
																													.singletonMap( "ru",
																															"Применить" ) ) )
																					.setCommandPermission( "$modify_security" )
																					.setCommandIcon( "command-apply" );
	
	private static final ControlCommand<?>				CMD_REMOVE			= Control
																					.createCommand( "remove",
																							MultivariantString
																									.getString( "Remove",
																											Collections
																													.singletonMap( "ru",
																															"Удалить" ) ) )
																					.setCommandPermission( "$modify_security" )
																					.setCommandIcon( "command-dispose" );
	
	private final ControlFieldset<?>					fieldset;
	
	private final String								path;
	
	private final BaseArrayDynamic<ControlBasic<?>>	groupList;
	
	/**
	 * @param path
	 */
	public FormFolderSecurity(final String path) {
		this.path = path;
		this.setAttributeIntern( "id", "security" );
		this.setAttributeIntern( "title",
				MultivariantString.getString( "Security settings",
						Collections.singletonMap( "ru", "Настройки безопасности" ) ) );
		this.setAttributeIntern( "path", path );
		this.recalculate();
		final ControlActor<?> node = Control.relativeNode( Context.getServer( Exec.currentProcess() ).getControlRoot(),
				path );
		final AccessPermissions available;
		if (node == null) {
			available = Access.createPermissionsLocal();
		} else {
			final AccessPermissions nodePermissions = node.getCommandPermissions();
			available = nodePermissions == null
					? Access.createPermissionsLocal()
					: nodePermissions;
		}
		final AccessManagerImpl manager = (AccessManagerImpl) Context.getServer( Exec.currentProcess() )
				.getAccessManager();
		final AclObject acl = manager.securityGetPermissionsFor( path );
		this.groupList = BaseObject.createArray();
		if (acl.getSettings() != null && acl.getSettings().length > 0) {
			final AclObject.Entry[] settings = acl.getSettings();
			for (final Entry element : settings) {
				this.groupList.add( available == null
						? element
						: element.getDescriberEntry( available ) );
			}
		}
		final BaseObject data = new BaseNativeObject()//
				.putAppend( "inherit", acl.isInherit() )//
				.putAppend( "groups", this.groupList )//
		;
		this.setData( data );
		this.fieldset = ControlFieldset.createFieldset()
				.addField( ControlFieldFactory
						.createFieldString( "path",
								MultivariantString.getString( "Current path",
										Collections.singletonMap( "ru", "Текущий путь" ) ),
								path ).setConstant() );
		if (path != null && path.length() > 1) {
			this.fieldset.addField( ControlFieldFactory.createFieldBoolean( "inherit",
					MultivariantString.getString( "Inherit permissions from parent",
							Collections.singletonMap( "ru", "Наследовать настройки сверху" ) ),
					true ) );
		}
		final BaseArrayDynamic<ControlBasic<?>> groupList = this.groupList;
		this.fieldset.addField( Control
				.createFieldList( "groups",
						MultivariantString.getString( "Settings", Collections.singletonMap( "ru", "Настройки" ) ),
						groupList )
				.setAttribute( "content_fieldset", FormFolderSecurity.FIELDSET_GROUP_LIST )
				.setAttribute( "content_handler",
						new BaseFunctionActAbstract<Void, ContainerGroupList>( Void.class, ContainerGroupList.class ) {
							@Override
							public ContainerGroupList apply(final Void listing) {
								return new ContainerGroupList( available, path, groupList );
							}
						} ) );
	}
	
	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) {
		if (command == FormFolderSecurity.CMD_SAVE || command == FormFolderSecurity.CMD_APPLY) {
			final AccessManagerImpl manager = (AccessManagerImpl) Context.getServer( Exec.currentProcess() )
					.getAccessManager();
			final AclObject acl = new AclObject( this.path,
					Convert.MapEntry.toBoolean( this.getData(), "inherit", true ) );
			final int length = this.groupList.length();
			for (int i = 0; i < length; ++i) {
				final ControlBasic<?> basic = (ControlBasic<?>) this.groupList.baseGet( i, null );
				acl.addSetting( (AclObject.Entry) basic );
			}
			manager.securitySetPermissionsFor( acl );
			return command == FormFolderSecurity.CMD_SAVE
					? null
					: this;
		}
		if (command == FormFolderSecurity.CMD_REMOVE) {
			final AccessManagerImpl manager = (AccessManagerImpl) Context.getServer( Exec.currentProcess() )
					.getAccessManager();
			final AclObject acl = new AclObject( this.path,
					Convert.MapEntry.toBoolean( this.getData(), "inherit", true ) );
			manager.securitySetPermissionsFor( acl );
			return null;
		}
		throw new IllegalArgumentException( "Unknown command: " + command.getKey() );
	}
	
	@Override
	public ControlCommandset getCommands() {
		final ControlCommandset result = Control.createOptions();
		result.add( FormFolderSecurity.CMD_SAVE );
		result.add( FormFolderSecurity.CMD_APPLY );
		result.add( FormFolderSecurity.CMD_REMOVE );
		return result;
	}
	
	@Override
	public ControlFieldset<?> getFieldset() {
		return this.fieldset;
	}
}
