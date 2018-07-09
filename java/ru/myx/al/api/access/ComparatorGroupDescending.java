/*
 * Created on 10.04.2006
 */
package ru.myx.al.api.access;

import java.util.Comparator;

import ru.myx.ae1.access.AccessGroup;

final class ComparatorGroupDescending implements Comparator<AccessGroup<?>> {
	@Override
	public final int compare(final AccessGroup<?> o1, final AccessGroup<?> o2) {
		return o2.getAuthLevel() - o1.getAuthLevel();
	}
}
