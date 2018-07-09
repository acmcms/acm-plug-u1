/*
 * Created on 21.04.2004
 * 
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ru.myx.al.api.access.user_selection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import ru.myx.ae1.access.Access;
import ru.myx.ae1.access.AccessGroup;
import ru.myx.ae1.access.AccessManager;
import ru.myx.ae1.access.AccessUser;
import ru.myx.ae1.access.SortMode;
import ru.myx.ae1.control.Control;
import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae1.know.Server;
import java.util.function.Function;
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
public class FormSearchSelectUsers extends AbstractForm<FormSearchSelectUsers> {
	
	private final class ResultContainer extends AbstractContainer<ResultContainer> {
		
		ResultContainer() {
			// empty
		}
		
		@Override
		public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) throws Exception {
			
			FormSearchSelectUsers.this.invalid = true;
			if ("remove".equals(command.getKey())) {
				FormSearchSelectUsers.this.selection.remove(Base.getString(command.getAttributes(), "key", null));
				return null;
			}
			if ("add".equals(command.getKey())) {
				FormSearchSelectUsers.this.selection.add(Base.getString(command.getAttributes(), "key", null));
				return null;
			}
			return super.getCommandResult(command, arguments);
		}
		
		@Override
		public ControlCommandset getContentCommands(final String key) {
			
			if (FormSearchSelectUsers.this.selection.contains(key)) {
				return Control.createOptionsSingleton(
						Control.createCommand("remove", MultivariantString.getString("Remove from selection", Collections.singletonMap("ru", "Удалить из выбора")))
								.setCommandIcon("command-delete").setAttribute("key", key));
			}
			return Control.createOptionsSingleton(
					Control.createCommand("add", MultivariantString.getString("Add to selection", Collections.singletonMap("ru", "Добавить в выбор"))).setCommandIcon("command-add")
							.setAttribute("key", key));
		}
	}
	
	private final class SelectionContainer extends AbstractContainer<SelectionContainer> {
		
		SelectionContainer() {
			// empty
		}
		
		@Override
		public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) throws Exception {
			
			FormSearchSelectUsers.this.invalid = true;
			if ("remove".equals(command.getKey())) {
				FormSearchSelectUsers.this.selection.remove(Base.getString(command.getAttributes(), "key", null));
				return null;
			}
			return super.getCommandResult(command, arguments);
		}
		
		@Override
		public ControlCommandset getContentCommands(final String key) {
			
			if (FormSearchSelectUsers.this.selection.contains(key)) {
				return Control.createOptionsSingleton(
						Control.createCommand("remove", MultivariantString.getString("Remove from selection", Collections.singletonMap("ru", "Удалить из выбора")))
								.setCommandIcon("command-delete").setAttribute("key", key));
			}
			return null;
		}
	}
	
	static ControlFieldset<?> FIELDSET_USER_LISTING = ControlFieldset.createFieldset()
			.addField(ControlFieldFactory.createFieldString("login", MultivariantString.getString("Login", Collections.singletonMap("ru", "Логин")), ""))
			.addField(ControlFieldFactory.createFieldString("email", MultivariantString.getString("E-mail", Collections.singletonMap("ru", "E-mail")), ""))
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
			
	private static final ControlCommand<?> CMD_OK = Control.createCommand("ok", MultivariantString.getString("Select", Collections.singletonMap("ru", "Выбрать")))
			.setCommandPermission("view").setCommandIcon("command-save");
			
	boolean invalid = false;
	
	private boolean first = true;
	
	private boolean show = false;
	
	/**
	 * 
	 */
	protected final Set<String> selection;
	
	private final Function<AccessUser<?>[], Object> function;
	
	private final Set<String> groups;
	
	private final ResultContainer containerResult;
	
	private final SelectionContainer containerSelection;
	
	/**
	 * @param group
	 * @param selection
	 * @param function
	 */
	public FormSearchSelectUsers(final AccessGroup<?> group, final AccessUser<?>[] selection, final Function<AccessUser<?>[], Object> function) {
		if (group == null) {
			this.groups = null;
		} else {
			this.groups = new TreeSet<>(Collections.singleton(group.getKey()));
		}
		this.selection = new TreeSet<>();
		if (selection != null) {
			for (int i = selection.length - 1; i >= 0; --i) {
				this.selection.add(selection[i].getKey());
			}
		}
		this.function = function;
		this.containerResult = new ResultContainer();
		this.containerSelection = new SelectionContainer();
		this.setAttributeIntern("id", "search_users");
		this.setAttributeIntern("title", MultivariantString.getString("Select users: search", Collections.singletonMap("ru", "Выбор пользователей: поиск")));
		this.recalculate();
	}
	
	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject parameters) {
		
		if (command == FormSearchSelectUsers.CMD_QUERY) {
			this.show = true;
			this.refresh();
			return this;
		}
		if (command == FormSearchSelectUsers.CMD_OK) {
			final AccessManager manager = Context.getServer(Exec.currentProcess()).getAccessManager();
			final List<AccessUser<?>> result = new ArrayList<>();
			for (final String userId : this.selection) {
				result.add(manager.getUser(userId, true));
			}
			try {
				return this.function.apply(result.toArray(new AccessUser<?>[result.size()]));
			} catch (final RuntimeException e) {
				throw e;
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		}
		throw new IllegalArgumentException("Unknown command: " + command.getKey());
	}
	
	@Override
	public ControlCommandset getCommands() {
		
		final ControlCommandset result = Control.createOptions();
		result.add(FormSearchSelectUsers.CMD_QUERY);
		result.add(FormSearchSelectUsers.CMD_OK);
		return result;
	}
	
	@Override
	public BaseObject getData() {
		
		if (this.first || this.invalid) {
			this.first = false;
			final AccessManager manager = Context.getServer(Exec.currentProcess()).getAccessManager();
			final BaseArrayDynamic<ControlBasic<?>> result = BaseObject.createArray();
			if (this.selection != null && !this.selection.isEmpty()) {
				for (final String userId : this.selection) {
					final AccessUser<?> user = manager.getUser(userId, true);
					final String title = user.getLogin();
					final BaseObject data = new BaseNativeObject()//
							.putAppend("login", user.getLogin())//
							.putAppend("email", user.getEmail())//
							.putAppend("added", Base.forDateMillis(user.getCreated()))//
							.putAppend("logged", Base.forDateMillis(user.getChanged()))//
							;
					result.add(Control.createBasic(userId, title, data));
				}
			}
			super.getData().baseDefine("selection", result);
		}
		if (this.show && this.invalid) {
			this.invalid = true;
			this.refresh();
		}
		return super.getData();
	}
	
	@Override
	public ControlFieldset<?> getFieldset() {
		
		final ResultContainer containerResult = this.containerResult;
		final SelectionContainer containerSelection = this.containerSelection;
		if (this.groups != null) {
			return this.show
				? ControlFieldset.createFieldset()
						.addField(ControlFieldFactory.createFieldString("login", FormSearchSelectUsers.STR_LOGIN_PART, "").setFieldHint(FormSearchSelectUsers.STR_HINT_LOGIN_PART))
						.addField(
								ControlFieldFactory.createFieldInteger("date", MultivariantString.getString("Last login", Collections.singletonMap("ru", "Посл. вход")), -1)
										.setFieldType("select").setAttribute(
												"lookup",
												new ControlLookupStatic(
														"-1,Ignore;0,Never logged in;12,Not logged for a year;6,Not logged for 6 month;3,Not logged for 3 months;2,Not logged for 2 months;1,Not logged for 1 month",
														";",
														",")))
						.addField(
								Control.createFieldList("result", FormSearchSelectUsers.STR_RESULTS, null)
										.setAttribute("content_fieldset", FormSearchSelectUsers.FIELDSET_USER_LISTING)
										.setAttribute("content_handler", new BaseFunctionActAbstract<Void, ResultContainer>(Void.class, ResultContainer.class) {
											
											@Override
											public ResultContainer apply(final Void arg) {
												
												return containerResult;
											}
										}))
						.addField(
								Control.createFieldList("selection", FormSearchSelectUsers.STR_SELECTION, null)
										.setAttribute("content_fieldset", FormSearchSelectUsers.FIELDSET_USER_LISTING)
										.setAttribute("content_handler", new BaseFunctionActAbstract<Void, SelectionContainer>(Void.class, SelectionContainer.class) {
											
											@Override
											public SelectionContainer apply(final Void arg) {
												
												return containerSelection;
											}
										}))
				: ControlFieldset.createFieldset()
						.addField(ControlFieldFactory.createFieldString("login", FormSearchSelectUsers.STR_LOGIN_PART, "").setFieldHint(FormSearchSelectUsers.STR_HINT_LOGIN_PART))
						.addField(
								ControlFieldFactory.createFieldInteger("date", FormSearchSelectUsers.STR_LAST_LOGIN, -1).setFieldType("select").setAttribute(
										"lookup",
										new ControlLookupStatic(
												"-1,Ignore;0,Never logged in;12,Not logged for a year;6,Not logged for 6 month;3,Not logged for 3 months;2,Not logged for 2 months;1,Not logged for 1 month",
												";",
												",")))
						.addField(
								Control.createFieldList("selection", FormSearchSelectUsers.STR_SELECTION, null)
										.setAttribute("content_fieldset", FormSearchSelectUsers.FIELDSET_USER_LISTING)
										.setAttribute("content_handler", new BaseFunctionActAbstract<Void, SelectionContainer>(Void.class, SelectionContainer.class) {
											
											@Override
											public SelectionContainer apply(final Void arg) {
												
												return containerSelection;
											}
										}));
		}
		return this.show
			? ControlFieldset.createFieldset()
					.addField(ControlFieldFactory.createFieldString("login", FormSearchSelectUsers.STR_LOGIN_PART, "").setFieldHint(FormSearchSelectUsers.STR_HINT_LOGIN_PART))
					.addField(
							ControlFieldFactory.createFieldSet("groups", FormSearchSelectUsers.STR_GROUPS, null)
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
							Control.createFieldList("result", FormSearchSelectUsers.STR_RESULTS, null).setAttribute("content_fieldset", FormSearchSelectUsers.FIELDSET_USER_LISTING)
									.setAttribute("content_handler", new BaseFunctionActAbstract<Void, ResultContainer>(Void.class, ResultContainer.class) {
										
										@Override
										public ResultContainer apply(final Void arg) {
											
											return containerResult;
										}
									}))
					.addField(
							Control.createFieldList("selection", FormSearchSelectUsers.STR_SELECTION, null)
									.setAttribute("content_fieldset", FormSearchSelectUsers.FIELDSET_USER_LISTING)
									.setAttribute("content_handler", new BaseFunctionActAbstract<Void, SelectionContainer>(Void.class, SelectionContainer.class) {
										
										@Override
										public SelectionContainer apply(final Void arg) {
											
											return containerSelection;
										}
									}))
			: ControlFieldset.createFieldset()
					.addField(ControlFieldFactory.createFieldString("login", FormSearchSelectUsers.STR_LOGIN_PART, "").setFieldHint(FormSearchSelectUsers.STR_HINT_LOGIN_PART))
					.addField(
							ControlFieldFactory.createFieldSet("groups", FormSearchSelectUsers.STR_GROUPS, null).setFieldHint(FormSearchSelectUsers.STR_HINT_GROUPS)
									.setFieldVariant("select").setAttribute("lookup", Access.GROUPS))
					.addField(
							ControlFieldFactory.createFieldInteger("date", FormSearchSelectUsers.STR_LAST_LOGIN, -1).setFieldType("select").setAttribute(
									"lookup",
									new ControlLookupStatic(
											"-1,Ignore;0,Never logged in;12,Not logged for a year;6,Not logged for 6 month;3,Not logged for 3 months;2,Not logged for 2 months;1,Not logged for 1 month",
											";",
											",")))
					.addField(
							Control.createFieldList("selection", FormSearchSelectUsers.STR_SELECTION, null)
									.setAttribute("content_fieldset", FormSearchSelectUsers.FIELDSET_USER_LISTING)
									.setAttribute("content_handler", new BaseFunctionActAbstract<Void, SelectionContainer>(Void.class, SelectionContainer.class) {
										
										@Override
										public SelectionContainer apply(final Void arg) {
											
											return containerSelection;
										}
									}));
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
						.putAppend("login", user.getLogin())//
						.putAppend("email", user.getEmail())//
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
