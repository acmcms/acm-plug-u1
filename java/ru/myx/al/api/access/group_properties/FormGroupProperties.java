/*
 * Created on 13.04.2004
 * 
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ru.myx.al.api.access.group_properties;

import java.util.Collections;

import ru.myx.ae1.access.Access;
import ru.myx.ae1.access.AccessGroup;
import ru.myx.ae1.access.AccessManager;
import ru.myx.ae1.access.AuthLevels;
import ru.myx.ae1.control.Control;
import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae3.act.Context;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseArray;
import ru.myx.ae3.base.BaseArrayDynamic;
import ru.myx.ae3.base.BaseFunctionActAbstract;
import ru.myx.ae3.base.BaseHostLookup;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.base.BasePrimitiveString;
import ru.myx.ae3.control.AbstractForm;
import ru.myx.ae3.control.command.ControlCommand;
import ru.myx.ae3.control.command.ControlCommandset;
import ru.myx.ae3.control.field.ControlFieldFactory;
import ru.myx.ae3.control.fieldset.ControlFieldset;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.xml.Xml;
import ru.myx.al.api.access.GroupObject;

/**
 * @author myx
 * 		
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public final class FormGroupProperties extends AbstractForm<FormGroupProperties> {
	
	private static final BaseHostLookup LOOKUP_ADDRESS_FORMATTER = new LookupAddressFormatter();
	
	private static final BaseHostLookup LOOKUP_USER_FORMATTER = new LookupUserFormatter();
	
	private static final ControlCommand<?> CMD_CREATE = Control.createCommand("create", " OK ").setCommandPermission("create").setCommandIcon("command-create");
	
	private static final ControlCommand<?> CMD_SAVE = Control.createCommand("save", " OK ").setCommandPermission("modify").setCommandIcon("command-save");
	
	private final ControlFieldset<?> fieldset;
	
	private final AccessGroup<?> group;
	
	private final boolean edit;
	
	private final BaseArrayDynamic<BasePrimitiveString> excludeAddressesList;
	
	private final BaseArrayDynamic<BasePrimitiveString> excludeUserList;
	
	/**
	 * @param path
	 * @param key
	 */
	public FormGroupProperties(final String path, final String key) {
		if (key == null || key.length() == 0) {
			this.edit = false;
			this.setAttributeIntern("id", "new_group");
			this.setAttributeIntern("title", MultivariantString.getString("Create group", Collections.singletonMap("ru", "Создание группы")));
		} else {
			this.edit = true;
			this.setAttributeIntern("id", "edit_group");
			this.setAttributeIntern("title", MultivariantString.getString("Edit group", Collections.singletonMap("ru", "Редактирование группы")));
		}
		this.setAttributeIntern("path", path);
		this.recalculate();
		final AccessManager accessManager = Context.getServer(Exec.currentProcess()).getAccessManager();
		this.group = this.edit
			? accessManager.getGroup(key, true)
			: accessManager.createGroup();
		final BaseArrayDynamic<BasePrimitiveString> excludeAddressesList = BaseObject.createArray();
		final BaseArrayDynamic<BasePrimitiveString> excludeUserList = BaseObject.createArray();
		this.excludeAddressesList = excludeAddressesList;
		this.excludeUserList = excludeUserList;
		{
			final GroupObject groupObject = (GroupObject) this.group;
			try {
				final BaseObject data = Xml.toBase("groupReadXml", groupObject.getXmlData(), null, null, null);
				final BaseObject exclude = data.baseGet("exclude", BaseObject.UNDEFINED);
				assert exclude != null : "NULL java value";
				if (exclude.baseIsPrimitive()) {
					// ignore
				} else {
					final BaseObject address = exclude.baseGet("address", BaseObject.UNDEFINED);
					assert address != null : "NULL java value";
					if (address == BaseObject.UNDEFINED) {
						// ignore
					} else {
						final BaseArray array = address.baseArray();
						if (array == null) {
							excludeAddressesList.add(address.baseToString());
						} else {
							final int length = array.length();
							for (int i = 0; i < length; ++i) {
								excludeAddressesList.add(array.baseGet(i, BaseObject.UNDEFINED).baseToString());
							}
						}
					}
					final BaseObject user = exclude.baseGet("user", BaseObject.UNDEFINED);
					assert user != null : "NULL java value";
					if (user == BaseObject.UNDEFINED) {
						// ignore
					} else {
						final BaseArray array = user.baseArray();
						if (array == null) {
							excludeUserList.add(user.baseToString());
						} else {
							final int length = array.length();
							for (int i = 0; i < length; ++i) {
								excludeUserList.add(array.baseGet(i, BaseObject.UNDEFINED).baseToString());
							}
						}
					}
				}
			} catch (final Throwable t) {
				// ignore
			}
		}
		final ContainerAddressesList containerAddressesList = new ContainerAddressesList(this.excludeAddressesList);
		final ContainerUserList containerUserList = new ContainerUserList(this.excludeUserList);
		final BaseObject excludeMap = new BaseNativeObject()//
				.putAppend("address", this.excludeAddressesList)//
				.putAppend("user", this.excludeUserList)//
				;
		final BaseObject data = new BaseNativeObject()//
				.putAppend("id", this.group.getKey())//
				.putAppend("title", this.group.getTitle())//
				.putAppend("description", this.group.getDescription())//
				.putAppend("authLevel", this.group.getAuthLevel())//
				.putAppend("exclude", excludeMap)//
				;
		this.setData(data);
		final ControlFieldset<?> excludeFieldset = ControlFieldset.createFieldset()
				.addField(
						Control.createFieldList("address", MultivariantString.getString("Addresses", Collections.singletonMap("ru", "Aдреса")), this.excludeAddressesList)
								.setAttribute("lookup", FormGroupProperties.LOOKUP_ADDRESS_FORMATTER)
								.setAttribute("content_handler", new BaseFunctionActAbstract<Void, ContainerAddressesList>(Void.class, ContainerAddressesList.class) {
									
									@Override
									public ContainerAddressesList apply(final Void arg) {
										
										return containerAddressesList;
									}
								}))
				.addField(
						Control.createFieldList("user", MultivariantString.getString("Users", Collections.singletonMap("ru", "Пользователи")), this.excludeUserList)
								.setFieldHint(
										MultivariantString.getString(
												"NOTE: This exclusion works only when auth level for this group is 'automatic' of higher!",
												Collections.singletonMap(
														"ru",
														"ВНИМАНИЕ: Действует только если у данной группы выбран уровень авторизации 'автоматический' или выше!")))
								.setAttribute("lookup", FormGroupProperties.LOOKUP_USER_FORMATTER)
								.setAttribute("content_handler", new BaseFunctionActAbstract<Void, ContainerUserList>(Void.class, ContainerUserList.class) {
									
									@Override
									public ContainerUserList apply(final Void arg) {
										
										return containerUserList;
									}
								}));
		this.fieldset = ControlFieldset.createFieldset()
				.addField(ControlFieldFactory.createFieldGuid("id", MultivariantString.getString("Name", Collections.singletonMap("ru", "Имя"))).setConstant())
				.addField(ControlFieldFactory.createFieldString("title", MultivariantString.getString("Title", Collections.singletonMap("ru", "Название")), "", 1, 255))
				.addField(
						ControlFieldFactory.createFieldString("description", MultivariantString.getString("Description", Collections.singletonMap("ru", "Описание")), "")
								.setFieldType(
										"text"))
				.addField(
						ControlFieldFactory
								.createFieldInteger(
										"authLevel",
										MultivariantString.getString("Auth level", Collections.singletonMap("ru", "Авторизация")),
										AuthLevels.AL_AUTHORIZED_NORMAL)
								.setFieldHint(
										MultivariantString.getString(
												"Minimal authorization level for membership checks.",
												Collections.singletonMap("ru", "Минимальный уровень авторизации для проверки членства в этой группе.")))
								.setFieldType("select").setAttribute("lookup", Access.AUTHORIZATION_TYPES))
				.addField(
						ControlFieldFactory.createFieldMap("exclude", MultivariantString.getString("Exclude", Collections.singletonMap("ru", "Исключения")), excludeMap)
								.setFieldVariant("fieldset").setAttribute("fieldset", excludeFieldset));
	}
	
	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) {
		
		if (command == FormGroupProperties.CMD_SAVE || command == FormGroupProperties.CMD_CREATE) {
			this.group.setTitle(Base.getString(this.getData(), "title", "-= untitled =-"));
			this.group.setDescription(Base.getString(this.getData(), "description", ""));
			this.group.setAuthLevel(Convert.MapEntry.toInt(this.getData(), "authLevel", AuthLevels.AL_UNAUTHORIZED));
			{
				final GroupObject groupObject = (GroupObject) this.group;
				BaseObject data;
				try {
					data = Xml.toBase("groupGetXml", groupObject.getXmlData(), null, null, null);
				} catch (final Throwable t) {
					data = new BaseNativeObject();
				}
				final BaseObject excludeMap = this.getData().baseGet("exclude", BaseObject.UNDEFINED);
				data.baseDefine("exclude", excludeMap);
				groupObject.setXmlData(Xml.toXmlString("data", data, false));
			}
			Context.getServer(Exec.currentProcess()).getAccessManager().commitGroup(this.group);
			return null;
		}
		throw new IllegalArgumentException("Unknown command: " + command.getKey());
	}
	
	@Override
	public ControlCommandset getCommands() {
		
		return Control.createOptionsSingleton(this.edit
			? FormGroupProperties.CMD_SAVE
			: FormGroupProperties.CMD_CREATE);
	}
	
	@Override
	public ControlFieldset<?> getFieldset() {
		
		return this.fieldset;
	}
}
