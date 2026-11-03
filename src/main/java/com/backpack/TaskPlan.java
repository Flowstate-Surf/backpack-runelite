package com.backpack;

final class TaskPlan
{
	private final Goal current;
	private final Goal next;
	TaskPlan(Goal current, Goal next) { this.current = current; this.next = next; }
	Goal getCurrent() { return current; }
	Goal getNext() { return next; }
}
