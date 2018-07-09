/*
 * Created on 10.04.2006
 */
package ru.myx.al.api.access;

import java.util.Comparator;

final class ComparatorAclAscending implements Comparator<AclObject> {
	@Override
	public int compare(final AclObject o1, final AclObject o2) {
		return o1.getPath().length() - o2.getPath().length();
	}
}
