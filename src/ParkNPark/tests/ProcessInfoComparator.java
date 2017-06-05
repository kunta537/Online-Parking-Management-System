package ParkNPark.tests;

import java.util.Comparator;

/**
 * Comparator that can compare two ProcessInfo objects with each other
 */
class ProcessInfoComparator<I> implements Comparator<I> {
	public int compare(I o1, I o2) {
		return ((ProcessInfo) o1).name.compareTo(((ProcessInfo) o2).name);
	}
}