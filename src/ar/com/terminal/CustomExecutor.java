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
	protected void toucheEvent(int physicalId, int stepIndex, Color color, long delay) {
		return;
	}

	@Override
	protected boolean hasNextStep() {
		return steps.hasNext();
	}

	@Override
	protected Step getNextStep() {
		return steps.next();
	}

	@Override
	protected void stepTimeOutEvent(int stepIndex) {
		return;
	}

}
