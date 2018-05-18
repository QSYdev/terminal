package ar.com.terminal;

import java.lang.reflect.Field;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import ar.com.terminal.Event.InternalEvent;
import ar.com.terminal.QSYPacket.CommandArgs;
import ar.com.terminal.Routine.NodeConfiguration;
import ar.com.terminal.Routine.Step;

abstract class Executor extends EventSourceI<InternalEvent> implements AutoCloseable {

	private static volatile int STEP_INDEX = 0;

	private final Terminal terminal;
	private final EventSource<InternalEvent> eventSource;

	private final BiMap biMap;
	private final boolean[] touchedNodes;

	private volatile Step currentStep;
	private volatile ExpressionTree expressionTree;
	private volatile boolean routineFinished;
	private volatile int stepIndex;

	private final Thread executionTimeOutTask;
	private final Thread stepTimeOutTask;
	private final Thread preInitTask;

	private volatile boolean closed;

	public Executor(Terminal terminal, ArrayList<Integer> nodesAssociations, long executionTimeOut) {
		this.terminal = terminal;
		this.eventSource = new EventSource<>();

		this.stepIndex = -1;
		this.closed = false;
		this.routineFinished = false;

		this.biMap = new BiMap(nodesAssociations);
		this.touchedNodes = new boolean[nodesAssociations.size()];

		this.executionTimeOutTask = new Thread(new ExecutionTimeOutTask(executionTimeOut), "ExecutionTimeOut");
		this.stepTimeOutTask = new Thread(new StepTimeOutTask(), "StepTimeOut");
		this.preInitTask = new Thread(new PreInitTask(), "PreInit");

		this.stepTimeOutTask.start();
		this.preInitTask.start();
	}

	public void touche(int physicalId, int stepIndex, Color color, long delay) {
		synchronized (this) {
			if (routineFinished)
				return;

			Integer logicalId = biMap.getLogicalId(physicalId);
			if (logicalId != null && stepIndex == this.stepIndex) {
				touchedNodes[logicalId] = true;
				// TODO results.touche(logicalId, stepIndex, color, delay);
				if (expressionTree.evaluateExpressionTree(touchedNodes)) {
					finalizeStep();
					if (hasNextStep()) {
						currentStep = getNextStep();
						prepareStep();
					} else {
						// TODO results.finish();
						routineFinished = true;
						eventSource.sendEvent(new InternalEvent.ExecutionFinished());
					}
				}
			}
		}
	}

	public boolean contains(int physicalId) {
		synchronized (this) {
			return (routineFinished) ? false : biMap.contains(physicalId);
		}
	}

	private void startExecution() {
		synchronized (this) {
			if (!routineFinished) {
				eventSource.sendEvent(new InternalEvent.ExecutionStarted());
				// TODO results.start();
				currentStep = getNextStep();
				turnAllNodes(Color.NO_COLOR);
				prepareStep();
				executionTimeOutTask.start();
			}
		}
	}

	private void turnAllNodes(Color color) {
		synchronized (this) {
			if (!routineFinished) {
				for (int i = 0; i < biMap.size(); i++)
					terminal.sendCommand(new CommandArgs(biMap.getPhysicalId(i), color, 0, 0), true);
			}
		}
	}

	protected abstract Step getNextStep();

	protected abstract boolean hasNextStep();

	private void prepareStep() {
		stepIndex = STEP_INDEX = (++STEP_INDEX > Short.MAX_VALUE) ? 1 : STEP_INDEX;

		long maxDelay = 0;
		for (NodeConfiguration configuration : currentStep.getNodeConfigurationList()) {
			int physicalId = biMap.getPhysicalId(configuration.getLogicalId());
			long delay = configuration.getDelay();
			if (delay > maxDelay)
				maxDelay = delay;
			terminal.sendCommand(new CommandArgs(physicalId, configuration.getColor(), delay, stepIndex), true);
		}

		expressionTree = new ExpressionTree(currentStep.getExpression());
		try {
			Field f = Thread.class.getDeclaredField("target");
			f.setAccessible(true);
			((StepTimeOutTask) f.get(stepTimeOutTask)).setStepTimeOut(currentStep.getTimeOut() + maxDelay, stepIndex);
			f.setAccessible(false);
		} catch (Exception e) {
		}
	}

	private void finalizeStep() {
		for (NodeConfiguration nodeConfiguration : currentStep.getNodeConfigurationList()) {
			int logicalId = nodeConfiguration.getLogicalId();
			if (!touchedNodes[logicalId]) {
				int physicalId = biMap.getPhysicalId(nodeConfiguration.getLogicalId());
				terminal.sendCommand(new CommandArgs(physicalId, Color.NO_COLOR, 0, 0), true);
			}
		}

		for (int i = 0; i < touchedNodes.length; i++)
			touchedNodes[i] = false;

		stepTimeOutTask.interrupt();
		expressionTree = null;
	}

	private void executionTimeOut() {
		synchronized (this) {
			if (!routineFinished) {
				// TODO results.executionTimeOut();
				routineFinished = true;
				eventSource.sendEvent(new InternalEvent.ExecutionFinished());
			}
		}
	}

	private void stepTimeOut(int stepIndex) {
		synchronized (this) {
			if (!routineFinished && stepIndex == this.stepIndex) {
				// TODO results.stepTimeout(stepIndex);
				eventSource.sendEvent(new InternalEvent.StepTimeOut());
				finalizeStep();
				if (hasNextStep() && !currentStep.stopOnTimeOut()) {
					currentStep = getNextStep();
					prepareStep();
				} else {
					// TODO results.finish();
					routineFinished = true;
					eventSource.sendEvent(new InternalEvent.ExecutionFinished());
				}
			}
		}
	}

	@Override
	public void addListener(EventListener<InternalEvent> eventListener) {
		eventSource.addListener(eventListener);
	}

	@Override
	public void removeListener(EventListener<InternalEvent> eventListener) {
		eventSource.removeListener(eventListener);
	}

	@Override
	public void close() {
		synchronized (this) {
			if (closed)
				return;

			closed = true;
			routineFinished = true;
			preInitTask.interrupt();
			try {
				Field f = Thread.class.getDeclaredField("target");
				f.setAccessible(true);
				((StepTimeOutTask) f.get(stepTimeOutTask)).setStepTimeOut(-1, 0);
				f.setAccessible(false);
			} catch (Exception e) {
			}
			stepTimeOutTask.interrupt();
			executionTimeOutTask.interrupt();
		}

		try {
			preInitTask.join();
		} catch (InterruptedException e) {
		}

		try {
			stepTimeOutTask.join();
		} catch (InterruptedException e) {
		}

		try {
			executionTimeOutTask.join();
		} catch (InterruptedException e) {
		}

		eventSource.close();
	}

	protected static final class BiMap {

		private final ArrayList<Integer> physicalIdNodes;
		private final TreeMap<Integer, Integer> logicalIdNodes;

		public BiMap(ArrayList<Integer> physicalIdNodes) {
			this.physicalIdNodes = physicalIdNodes;
			this.logicalIdNodes = new TreeMap<>();
			for (int i = 0; i < physicalIdNodes.size(); i++) {
				int physicalId = physicalIdNodes.get(i);
				logicalIdNodes.put(physicalId, i);
			}
		}

		public boolean contains(int physicalId) {
			return logicalIdNodes.containsKey(physicalId);
		}

		public Integer getLogicalId(int physicalId) {
			return logicalIdNodes.get(physicalId);
		}

		public Integer getPhysicalId(int logicalId) {
			return physicalIdNodes.get(logicalId);
		}

		public int size() {
			return physicalIdNodes.size();
		}

	}

	private final class PreInitTask implements Runnable {

		@Override
		public void run() {
			try {
				for (int j = 0; j < 2; j++) {
					turnAllNodes(Color.RED);
					Thread.sleep(500);
					turnAllNodes(Color.NO_COLOR);
					Thread.sleep(500);
				}

				for (int j = 0; j < 2; j++) {
					turnAllNodes(Color.GREEN);
					Thread.sleep(150);
					turnAllNodes(Color.NO_COLOR);
					Thread.sleep(150);
				}
				startExecution();
			} catch (InterruptedException e) {
			}
		}

	}

	private final class ExecutionTimeOutTask implements Runnable {

		private final long executionTimeOut;

		public ExecutionTimeOutTask(long executionTimeOut) {
			this.executionTimeOut = executionTimeOut;
		}

		@Override
		public void run() {
			try {
				if (executionTimeOut > 0) {
					Thread.sleep(executionTimeOut);
					executionTimeOut();
				}
			} catch (InterruptedException e) {
			}
		}
	}

	private final class StepTimeOutTask implements Runnable {

		private final AtomicBoolean running = new AtomicBoolean(true);
		private final LinkedBlockingQueue<Entry<Long, Integer>> messages = new LinkedBlockingQueue<>();

		public void setStepTimeOut(long stepTimeOut, int stepIndex) {
			if (stepTimeOut < 0)
				running.set(false);
			else
				messages.add(new SimpleImmutableEntry<>(stepTimeOut, stepIndex));
		}

		@Override
		public void run() {
			while (running.get()) {
				try {
					Entry<Long, Integer> entry = messages.take();
					if (entry.getKey() > 0) {
						Thread.sleep(entry.getKey());
						stepTimeOut(entry.getValue());
					}
				} catch (InterruptedException e) {
				}
			}
		}

	}
}
