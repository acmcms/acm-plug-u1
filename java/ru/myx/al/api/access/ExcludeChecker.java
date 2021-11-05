/*
 * Created on 28.06.2004
 */
package ru.myx.al.api.access;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ru.myx.ae1.access.AuthLevels;
import ru.myx.ae3.act.Context;
import ru.myx.ae3.base.BaseArray;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.serve.ServeRequest;

/**
 * @author myx
 * 		
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
class ExcludeChecker {
	
	private static final Comparator<String> COMPARATOR_PREFFIX_SORT = new Comparator<>() {
		
		@Override
		public int compare(final String o1, final String o2) {
			
			return o1.length() - o2.length();
		}
	};
	
	static final ExcludeChecker getChecker(final int authLevel, final BaseObject exclude) {
		
		assert exclude != null : "NULL java object!";
		if (exclude.baseIsPrimitive()) {
			return null;
		}
		final BaseObject excludeAddress = exclude.baseGet("address", BaseObject.UNDEFINED);
		assert excludeAddress != null : "NULL java value";
		final BaseObject excludeUser = exclude.baseGet("user", BaseObject.UNDEFINED);
		assert excludeUser != null : "NULL java value";
		final List<String> addresses = new ArrayList<>();
		final List<String> prefixes = new ArrayList<>();
		final List<String> suffixes = new ArrayList<>();
		final List<String> users = new ArrayList<>();
		if (excludeAddress == BaseObject.UNDEFINED) {
			// ignore
		} else {
			final BaseArray array = excludeAddress.baseArray();
			if (array == null) {
				ExcludeChecker.tryExclude(excludeAddress.baseToJavaString(), addresses, prefixes, suffixes);
			} else {
				final int length = array.length();
				for (int i = 0; i < length; ++i) {
					ExcludeChecker.tryExclude(array.baseGet(i, BaseObject.UNDEFINED).baseToJavaString(), addresses, prefixes, suffixes);
				}
			}
		}
		if (authLevel >= AuthLevels.AL_AUTHORIZED_AUTOMATICALLY) {
			if (excludeUser == BaseObject.UNDEFINED) {
				// ignore
			} else {
				final BaseArray array = excludeUser.baseArray();
				if (array == null) {
					users.add(excludeUser.baseToJavaString());
				} else {
					final int length = array.length();
					for (int i = 0; i < length; ++i) {
						users.add(array.baseGet(i, BaseObject.UNDEFINED).baseToJavaString());
					}
				}
			}
		}
		if (addresses.size() > 0 || prefixes.size() > 0 || users.size() > 0) {
			final Set<String> checkAddresses = addresses.size() == 0
				? null
				: new HashSet<>(addresses);
			final String[] checkPrefixes = prefixes.size() == 0
				? null
				: (String[]) prefixes.toArray(new String[prefixes.size()]);
			if (checkPrefixes != null) {
				Arrays.sort(checkPrefixes, ExcludeChecker.COMPARATOR_PREFFIX_SORT);
			}
			final String[] checkSuffixes = suffixes.size() == 0
				? null
				: (String[]) suffixes.toArray(new String[suffixes.size()]);
			if (checkSuffixes != null) {
				Arrays.sort(checkSuffixes, ExcludeChecker.COMPARATOR_PREFFIX_SORT);
			}
			final Set<String> checkUsers = users.size() == 0
				? null
				: new HashSet<>(users);
			return new ExcludeChecker(checkAddresses, checkPrefixes, checkSuffixes, checkUsers);
		}
		return null;
	}
	
	private static final void tryExclude(final String address, final List<String> addresses, final List<String> prefixes, final List<String> suffixes) {
		
		assert address != null : "unexpected NULL value";
		if (address.length() == 0) {
			return;
		}
		if (address.endsWith("*")) {
			prefixes.add(address.substring(0, address.length() - 2));
			return;
		}
		if (address.charAt(0) == '*') {
			suffixes.add(address.substring(1));
			return;
		}
		addresses.add(address);
	}
	
	private final Set<String> checkAddresses;
	
	private final String[] checkAddressPrefixes;
	
	private final String[] checkAddressSuffixes;
	
	private final Set<String> checkUsers;
	
	private ExcludeChecker(final Set<String> checkAddresses, final String[] checkAddressPrefixes, final String[] checkAddressSuffixes, final Set<String> checkUsers) {
		this.checkAddresses = checkAddresses;
		this.checkAddressPrefixes = checkAddressPrefixes;
		this.checkAddressSuffixes = checkAddressSuffixes;
		this.checkUsers = checkUsers;
	}
	
	boolean check() {
		
		final ServeRequest query = Context.getRequest(Exec.currentProcess());
		final String address = query.getSourceAddress();
		if (this.checkAddresses != null && (address == null || this.checkAddresses.contains(address))) {
			return false;
		}
		if (this.checkUsers != null) {
			if (this.checkUsers.contains(query.getUserID())) {
				return false;
			}
		}
		if (this.checkAddressPrefixes != null) {
			if (address == null) {
				return false;
			}
			for (int i = this.checkAddressPrefixes.length - 1; i >= 0; --i) {
				if (address.startsWith(this.checkAddressPrefixes[i])) {
					return false;
				}
			}
		}
		if (this.checkAddressSuffixes != null) {
			if (address == null) {
				return false;
			}
			for (int i = this.checkAddressSuffixes.length - 1; i >= 0; --i) {
				if (address.endsWith(this.checkAddressSuffixes[i])) {
					return false;
				}
			}
		}
		return true;
	}
}
