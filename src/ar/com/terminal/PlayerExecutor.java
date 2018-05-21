package ar.com.terminal;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;

import ar.com.terminal.Routine.NodeConfiguration;
import ar.com.terminal.Routine.Step;

final class PlayerExecutor extends Executor {

	private final ArrayList<Color> playersAndColors;
	private final ArrayList<Color> stepsWinners;

	private final boolean waitForAllPlayers;
	private final long stepTimeOut;
	private final long stepDelay;
	private final int numberOfSteps;
	private final boolean stopOnStepTimeOut;
	private final int numberOfNodes;

	private volatile int stepIndex;

	public PlayerExecutor(Terminal terminal, ArrayList<Integer> nodesAssociations, ArrayList<Color> playersAndColors, boolean waitForAllPlayers, long stepDelay, long stepTimeOut,
			boolean stopOnStepTimeOut, int numberOfSteps, long executionTimeOut) {
		super(terminal, nodesAssociations, executionTimeOut);

		this.playersAndColors = playersAndColors;
		this.stepsWinners = new ArrayList<>();

		this.waitForAllPlayers = waitForAllPlayers;
		this.stepTimeOut = stepTimeOut;
		this.stepDelay = stepDelay;
		this.numberOfSteps = numberOfSteps;
		this.stopOnStepTimeOut = stopOnStepTimeOut;
		this.numberOfNodes = nodesAssociations.size();

		this.stepIndex = 0;
	}

	@Override
	protected void toucheEvent(int physicalId, int stepIndex, Color color, long delay) {
		/* Solo se guarda el color ganador del step */
		if (stepsWinners.size() < this.stepIndex)
			stepsWinners.add(color);
	}

	@Override
	protected Step getNextStep() {
		char booleanOperator = (waitForAllPlayers) ? '&' : '|';
		LinkedList<Integer> usedIds = new LinkedList<>();

		for (int i = 0; i < numberOfNodes; i++)
			usedIds.add(i);

		LinkedList<NodeConfiguration> nodesConfiguration = new LinkedList<>();
		StringBuilder sb = new StringBuilder();

		for (Color color : playersAndColors) {
			int id = usedIds.remove(ThreadLocalRandom.current().nextInt(0, usedIds.size()));
			nodesConfiguration.add(new NodeConfiguration(id, stepDelay, color));
			sb.append(id);
			sb.append(booleanOperator);
		}
		sb.deleteCharAt(sb.length() - 1);

		String expression = sb.toString();

		++stepIndex;
		return new Step(nodesConfiguration, stepTimeOut, expression, stopOnStepTimeOut);
	}

	@Override
	protected boolean hasNextStep() {
		return numberOfSteps == 0 || stepIndex < numberOfSteps;
	}

	@Override
	protected void stepTimeOutEvent(int stepIndex) {
		stepsWinners.add(Color.NO_COLOR);
	}

}
