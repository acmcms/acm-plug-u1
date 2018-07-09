/**
 * 
 */
package ru.myx.al.api.access.group_properties;

import java.util.Iterator;

import ru.myx.ae1.access.AccessUser;
import ru.myx.ae3.act.Context;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseHostLookup;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.base.BasePrimitive;
import ru.myx.ae3.exec.Exec;

final class LookupUserFormatter extends BaseHostLookup {
	
	
	@Override
	public final BaseObject baseGetLookupValue(final BaseObject key) {
		
		
		final String string = key.baseToJavaString();
		final AccessUser<?> user = Context.getServer(Exec.currentProcess()).getAccessManager().getUser(string, false);
		return user == null
			? key
			: Base.forString(user.getLogin() + " (" + user.getEmail() + ')');
	}
	
	@Override
	public boolean baseHasKeysOwn() {
		
		
		return false;
	}
	
	@Override
	public Iterator<String> baseKeysOwn() {
		
		
		return BaseObject.ITERATOR_EMPTY;
	}
	
	@Override
	public Iterator<? extends CharSequence> baseKeysOwnAll() {
		
		
		return this.baseKeysOwn();
	}
	
	@Override
	public Iterator<? extends BasePrimitive<?>> baseKeysOwnPrimitive() {
		
		
		return BaseObject.ITERATOR_EMPTY_PRIMITIVE;
	}
	
	@Override
	public String toString() {
		
		
		return "[Lookup: User formatter]";
	}
}
