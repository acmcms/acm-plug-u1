/*
 * Created on 21.04.2004
 * 
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ru.myx.al.api.access.user_selection;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import ru.myx.ae1.access.Access;
import ru.myx.ae1.access.AccessGroup;
import ru.myx.ae1.access.AccessUser;
import ru.myx.ae1.access.SortMode;
import ru.myx.ae1.control.Control;
import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae1.know.Server;
import ru.myx.ae3.act.Context;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseArray;
import ru.myx.ae3.base.BaseArrayDynamic;
import ru.myx.ae3.base.BaseFunctionActAbstract;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.control.AbstractContainer;
import ru.myx.ae3.control.AbstractForm;
import ru.myx.ae3.control.ControlBasic;
import ru.myx.ae3.control.ControlContainer;
import ru.myx.ae3.control.ControlLookupStatic;
import ru.myx.ae3.control.command.ControlCommand;
import ru.myx.ae3.control.command.ControlCommandset;
import ru.myx.ae3.control.field.ControlFieldFactory;
import ru.myx.ae3.control.fieldset.ControlFieldset;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.help.Convert;

/**
 * @author myx
 * 		
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class FormSearchSelectUser extends AbstractForm<FormSearchSelectUser> {
	
	private final class ResultContainer extends AbstractContainer<ResultContainer> {
		
		ResultContainer() {
			// empty
		}
		
		@Override
		public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) throws Exception {
			
			FormSearchSelectUser.this.invalid = true;
			return super.getCommandResult(command, arguments);
		}
		
		@Override
		public ControlCommandset getContentCommands(final String key) {
			
			final int index = Convert.Any.toInt(key, -1);
			if (index == -1) {
				return super.getContentCommands(key);
			}
			final BaseArray list = Convert.MapEntry.toCollection(this.getData(), "result", null);
			final ControlBasic<?> user = (ControlBasic<?>) list.baseGet(index, null);
			return super.getContentCommands(user.getKey());
		}
	}
	
	static ControlFieldset<?> FIELDSET_USER_LISTING = ControlFieldset.createFieldset()
			.addField(
					ControlFieldFactory.createFieldString("login", MultivariantString.getString("Login", Collections.singletonMap("ru", "Логин")), "")
							.setFieldHint(MultivariantString.getString("Use '*' for pattern search", Collections.singletonMap("ru", "Используйте '*' для поиска по маске"))))
			.addField(
					ControlFieldFactory.createFieldString("email", MultivariantString.getString("E-mail", Collections.singletonMap("ru", "E-mail")), "")
							.setFieldHint(MultivariantString.getString("Use '*' for pattern search", Collections.singletonMap("ru", "Используйте '*' для поиска по маске"))))
			.addField(ControlFieldFactory.createFieldDate("added", MultivariantString.getString("Added", Collections.singletonMap("ru", "Добавлен")), 0L).setConstant())
			.addField(ControlFieldFactory.createFieldDate("logged", MultivariantString.getString("Logged", Collections.singletonMap("ru", "Входил")), 0L).setConstant());
			
	private static final BaseObject STR_LOGIN_PART = MultivariantString.getString("Login/E-mail part", Collections.singletonMap("ru", "Логин/E-mail"));
	
	private static final BaseObject STR_HINT_LOGIN_PART = MultivariantString.getString(
			"Search results will contain all users with login or email containing value specified.",
			Collections.singletonMap("ru", "Результаты поиска будут содержать пользователей, чей логин или e-mail содержит указанную подстроку"));
			
	private static final BaseObject STR_GROUPS = MultivariantString.getString("Group membership", Collections.singletonMap("ru", "Членство в группах"));
	
	private static final BaseObject STR_HINT_GROUPS = MultivariantString.getString(
			"Search results will contain all users having any of groups specified.",
			Collections.singletonMap("ru", "Результаты поиска будут содержать пользователей входящих в любую из указанных групп"));
			
	private static final BaseObject STR_LAST_LOGIN = MultivariantString.getString("Last login", Collections.singletonMap("ru", "Посл. вход"));
	
	private static final BaseObject STR_RESULTS = MultivariantString.getString("Search results", Collections.singletonMap("ru", "Результаты поиска"));
	
	private static final BaseObject STR_SELECTION = MultivariantString.getString("Selection", Collections.singletonMap("ru", "Выбор"));
	
	private static final ControlCommand<?> CMD_QUERY = Control.createCommand("query", MultivariantString.getString("Search", Collections.singletonMap("ru", "Искать")))
			.setCommandPermission("view").setCommandIcon("command-search");
			
	boolean invalid = false;
	
	private boolean show = false;
	
	private final Set<String> groups;
	
	private final ControlContainer<?> containerResult;
	
	/**
	 * @param group
	 */
	public FormSearchSelectUser(final AccessGroup<?> group) {
		if (group == null) {
			this.groups = null;
		} else {
			this.groups = new TreeSet<>(Collections.singleton(group.getKey()));
		}
		this.containerResult = new ResultContainer();
		this.setAttributeIntern("id", "search_users");
		this.setAttributeIntern("title", MultivariantString.getString("Select user: search", Collections.singletonMap("ru", "Выбор пользователя: поиск")));
		this.recalculate();
	}
	
	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject parameters) {
		
		if (command == FormSearchSelectUser.CMD_QUERY) {
			this.show = true;
			this.refresh();
			return this;
		}
		throw new IllegalArgumentException("Unknown command: " + command.getKey());
	}
	
	@Override
	public ControlCommandset getCommands() {
		
		return Control.createOptionsSingleton(FormSearchSelectUser.CMD_QUERY);
	}
	
	@Override
	public BaseObject getData() {
		
		if (this.show && this.invalid) {
			this.invalid = true;
			this.refresh();
		}
		return super.getData();
	}
	
	@Override
	public ControlFieldset<?> getFieldset() {
		
		final Object nodeUsers = this.containerResult;
		if (this.groups != null) {
			return this.show
				? ControlFieldset.createFieldset()
						.addField(ControlFieldFactory.createFieldString("login", FormSearchSelectUser.STR_LOGIN_PART, "").setFieldHint(FormSearchSelectUser.STR_HINT_LOGIN_PART))
						.addField(
								ControlFieldFactory.createFieldInteger("date", MultivariantString.getString("Last login", Collections.singletonMap("ru", "Посл. вход")), -1)
										.setFieldType("select").setAttribute(
												"lookup",
												new ControlLookupStatic(
														"-1,Ignore;0,Never logged in;12,Not logged for a year;6,Not logged for 6 month;3,Not logged for 3 months;2,Not logged for 2 months;1,Not logged for 1 month",
														";",
														",")))
						.addField(
								Control.createFieldList("result", FormSearchSelectUser.STR_RESULTS, null)
										.setAttribute("content_fieldset", FormSearchSelectUser.FIELDSET_USER_LISTING)
										.setAttribute("content_handler", new BaseFunctionActAbstract<>(Object.class, Object.class) {
											
											@Override
											public Object apply(final Object arg) {
												
												return nodeUsers;
											}
										}))
						.addField(ControlFieldFactory.createFieldString("selection", FormSearchSelectUser.STR_SELECTION, "").setConstant())
				: ControlFieldset.createFieldset()
						.addField(ControlFieldFactory.createFieldString("login", FormSearchSelectUser.STR_LOGIN_PART, "").setFieldHint(FormSearchSelectUser.STR_HINT_LOGIN_PART))
						.addField(
								ControlFieldFactory.createFieldInteger("date", FormSearchSelectUser.STR_LAST_LOGIN, -1).setFieldType("select").setAttribute(
										"lookup",
										new ControlLookupStatic(
												"-1,Ignore;0,Never logged in;12,Not logged for a year;6,Not logged for 6 month;3,Not logged for 3 months;2,Not logged for 2 months;1,Not logged for 1 month",
												";",
												",")))
						.addField(ControlFieldFactory.createFieldString("selection", FormSearchSelectUser.STR_SELECTION, "").setConstant());
		}
		return this.show
			? ControlFieldset.createFieldset()
					.addField(ControlFieldFactory.createFieldString("login", FormSearchSelectUser.STR_LOGIN_PART, "").setFieldHint(FormSearchSelectUser.STR_HINT_LOGIN_PART))
					.addField(
							ControlFieldFactory.createFieldSet("groups", FormSearchSelectUser.STR_GROUPS, null)
									.setFieldHint("Search results will contain all users having groups specified.").setFieldVariant("select").setAttribute("lookup", Access.GROUPS))
					.addField(
							ControlFieldFactory.createFieldInteger("date", MultivariantString.getString("Last login", Collections.singletonMap("ru", "Посл. вход")), -1)
									.setFieldType("select").setAttribute(
											"lookup",
											new ControlLookupStatic(
													"-1,Ignore;0,Never logged in;12,Not logged for a year;6,Not logged for 6 month;3,Not logged for 3 months;2,Not logged for 2 months;1,Not logged for 1 month",
													";",
													",")))
					.addField(
							Control.createFieldList("result", FormSearchSelectUser.STR_RESULTS, null).setAttribute("content_fieldset", FormSearchSelectUser.FIELDSET_USER_LISTING)
									.setAttribute("content_handler", new BaseFunctionActAbstract<>(Object.class, Object.class) {
										
										@Override
										public Object apply(final Object arg) {
											
											return nodeUsers;
										}
									}))
					.addField(ControlFieldFactory.createFieldString("selection", FormSearchSelectUser.STR_SELECTION, "").setConstant())
			: ControlFieldset.createFieldset()
					.addField(ControlFieldFactory.createFieldString("login", FormSearchSelectUser.STR_LOGIN_PART, "").setFieldHint(FormSearchSelectUser.STR_HINT_LOGIN_PART))
					.addField(
							ControlFieldFactory.createFieldSet("groups", FormSearchSelectUser.STR_GROUPS, null).setFieldHint(FormSearchSelectUser.STR_HINT_GROUPS)
									.setFieldVariant("select").setAttribute("lookup", Access.GROUPS))
					.addField(
							ControlFieldFactory.createFieldInteger("date", FormSearchSelectUser.STR_LAST_LOGIN, -1).setFieldType("select").setAttribute(
									"lookup",
									new ControlLookupStatic(
											"-1,Ignore;0,Never logged in;12,Not logged for a year;6,Not logged for 6 month;3,Not logged for 3 months;2,Not logged for 2 months;1,Not logged for 1 month",
											";",
											",")))
					.addField(ControlFieldFactory.createFieldString("selection", FormSearchSelectUser.STR_SELECTION, "").setConstant());
	}
	
	private final void refresh() {
		
		if (!this.show) {
			return;
		}
		Set<AccessUser<?>> ids = null;
		final String login = Base.getString(super.getData(), "login", "").trim();
		final String email = Base.getString(super.getData(), "email", "").trim();
		final Server server = Context.getServer(Exec.currentProcess());
		if (login.length() > 0 || email.length() > 0) {
			final AccessUser<?>[] users = server.getAccessManager().search(login.toLowerCase(), email.toLowerCase(), -1, -1, SortMode.SM_LOGIN);
			ids = new HashSet<>(Arrays.asList(users));
		}
		if (this.groups != null) {
			if (this.groups.size() > 0) {
				final AccessUser<?>[] users = server.getAccessManager().searchByMembership(this.groups, SortMode.SM_LOGIN);
				if (ids == null) {
					ids = new HashSet<>(Arrays.asList(users));
				} else {
					ids.retainAll(Arrays.asList(users));
				}
			}
		} else {
			final Set<String> groups = new TreeSet<>();
			final BaseArray set = Convert.MapEntry.toCollection(super.getData(), "groups", null);
			if (set != null) {
				final int length = set.length();
				for (int i = 0; i < length; ++i) {
					final String id = set.baseGet(i, BaseObject.UNDEFINED).baseToJavaString();
					groups.add(id);
				}
			}
			if (groups.size() > 0) {
				final AccessUser<?>[] users = server.getAccessManager().searchByMembership(groups, SortMode.SM_LOGIN);
				if (ids == null) {
					ids = new HashSet<>(Arrays.asList(users));
				} else {
					ids.retainAll(Arrays.asList(users));
				}
			}
		}
		final int date = Convert.MapEntry.toInt(super.getData(), "date", -1);
		if (date != -1) {
			long end = 0;
			if (date > 0) {
				final java.util.GregorianCalendar gc = new java.util.GregorianCalendar();
				gc.add(java.util.Calendar.MONTH, -date);
				end = gc.getTime().getTime();
			}
			final AccessUser<?>[] users = server.getAccessManager().search(null, null, 0, end, SortMode.SM_LOGIN);
			if (ids == null) {
				ids = new HashSet<>(Arrays.asList(users));
			} else {
				ids.retainAll(Arrays.asList(users));
			}
		}
		final BaseArrayDynamic<ControlBasic<?>> result = BaseObject.createArray();
		if (ids != null && !ids.isEmpty()) {
			for (final AccessUser<?> user : ids) {
				final String userId = user.getKey();
				final String title = user.getLogin();
				final BaseObject data = new BaseNativeObject()//
						.putAppend("login", user.getLogin().toLowerCase())//
						.putAppend("email", user.getEmail().toLowerCase())//
						.putAppend("added", Base.forDateMillis(user.getCreated()))//
						.putAppend("logged", Base.forDateMillis(user.getChanged()))//
						;
				result.add(Control.createBasic(userId, title, data));
			}
		}
		super.getData().baseDefine("result", result);
		this.show = true;
	}
}
