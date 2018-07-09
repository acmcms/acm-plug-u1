/**
 * 
 */
package ru.myx.al.api.access.group_properties;

import java.util.Collections;
import java.util.Iterator;

import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae3.base.BaseHostLookup;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.base.BasePrimitive;

final class LookupAddressFormatter extends BaseHostLookup {
	
	
	@Override
	public BaseObject baseGetLookupValue(final BaseObject key) {
		
		
		final String string = key.baseToJavaString();
		return string.endsWith("*")
			? MultivariantString.getString("prefix - " + string, Collections.singletonMap("ru", "преффикс - " + string))
			: string.startsWith("*")
				? MultivariantString.getString("suffix - " + string, Collections.singletonMap("ru", "суффикс - " + string))
				: MultivariantString.getString("address - " + string, Collections.singletonMap("ru", "адрес - " + string));
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
		
		
		return "[Lookup: Addres formatter]";
	}
}
