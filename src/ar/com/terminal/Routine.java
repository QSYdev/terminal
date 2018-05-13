package ar.com.terminal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import ar.com.terminal.Routine.Step;

public final class Routine implements Iterable<Step> {

	private final byte playersCount;
	private final byte numberOfNodes;
	private final long totalTimeOut;
	private String name;
	private final ArrayList<Step> steps;

	public Routine(byte playersCount, byte numberOfNodes, long totalTimeOut, ArrayList<Step> steps, String name) {
		this.playersCount = playersCount;
		this.numberOfNodes = numberOfNodes;
		this.totalTimeOut = totalTimeOut;
		this.steps = steps;
		this.name = name;
	}

	public byte getPlayersCount() {
		return playersCount;
	}

	public byte getNumberOfNodes() {
		return numberOfNodes;
	}

	public long getTotalTimeOut() {
		return totalTimeOut;
	}

	public ArrayList<Step> getSteps() {
		return steps;
	}

	public String getName() {
		return name;
	}

	@Override
	public Iterator<Step> iterator() {
		return steps.iterator();
	}

	public static final class Step {

		private final String expression;
		private final long timeOut;
		private final boolean stopOnTimeout;
		private final LinkedList<NodeConfiguration> nodeConfigurationList;

		public Step(LinkedList<NodeConfiguration> nodeConfigurationList, long timeOut, String expression, boolean stopOnTimeout) {
			this.expression = expression;
			this.timeOut = timeOut;
			this.nodeConfigurationList = nodeConfigurationList;
			this.stopOnTimeout = stopOnTimeout;
		}

		public String getExpression() {
			return expression;
		}

		public long getTimeOut() {
			return timeOut;
		}

		public boolean stopOnTimeOut() {
			return stopOnTimeout;
		}

		public LinkedList<NodeConfiguration> getNodeConfigurationList() {
			return nodeConfigurationList;
		}

	}

	public static final class NodeConfiguration {

		private final int logicalId;
		private final Color color;
		private final long delay;

		public NodeConfiguration(int logicalId, long delay, Color color) {
			this.logicalId = logicalId;
			this.color = color;
			this.delay = delay;
		}

		public int getLogicalId() {
			return logicalId;
		}

		public Color getColor() {
			return color;
		}

		public long getDelay() {
			return delay;
		}
	}
}
