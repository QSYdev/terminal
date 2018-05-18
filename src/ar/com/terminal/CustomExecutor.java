package ar.com.terminal;

import java.util.ArrayList;
import java.util.Iterator;

import ar.com.terminal.Routine.Step;

final class CustomExecutor extends Executor {

	private final Iterator<Step> steps;

	public CustomExecutor(Terminal terminal, ArrayList<Integer> nodesAssociations, Routine routine) {
		super(terminal, nodesAssociations, routine.getTotalTimeOut());
		this.steps = routine.iterator();
	}

	@Override
	protected Step getNextStep() {
		return steps.next();
	}

	@Override
	protected boolean hasNextStep() {
		return steps.hasNext();
	}

}
