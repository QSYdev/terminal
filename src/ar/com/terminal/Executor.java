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

	private final Terminal terminal;
	private final EventSource<InternalEvent> eventSource;

	private final BiMap biMap;
	private final boolean[] touchedNodes;

	private volatile Step currentStep;
	private volatile ExpressionTree expressionTree;
	private volatile int stepIndex;

	private final Thread executionTimeOutTask;
	private final Thread stepTimeOutTask;
	private final Thread preInitTask;

	public Executor(Terminal terminal, ArrayList<Integer> nodesAssociations, long executionTimeOut) {
		this.terminal = terminal;
		this.eventSource = new EventSource<>();

		this.biMap = new BiMap(nodesAssociations);
		this.touchedNodes = new boolean[nodesAssociations.size()];

		this.executionTimeOutTask = new Thread(new ExecutionTimeOutTask(executionTimeOut), "ExecutionTimeOut");
		this.stepTimeOutTask = new Thread(new StepTimeOutTask(), "StepTimeOut");
		this.preInitTask = new Thread(new PreInitTask(), "PreInit");
		this.preInitTask.start();
	}

	public void touche(int physicalId, int stepIndex, Color color, long delay) {
		synchronized (this) {
			int logicalId = biMap.getLogicalId(physicalId);
			if (stepIndex == this.stepIndex) {
				touchedNodes[logicalId] = true;
				// TODO results.touche(logicalId, stepIndex, color, delay);
				if (expressionTree.evaluateExpressionTree(touchedNodes)) {
					finalizeStep();
					if (hasNextStep()) {
						currentStep = getNextStep();
						prepareStep();
					} else {
						// TODO results.finish();
						++stepIndex; // Se hace para cortar las demas entradas.
						eventSource.sendEvent(new InternalEvent.ExecutionFinished());
					}
				}
			}
		}
	}

	/*
	 * Determina si el id fisico que se envia esta siendo utilizado para esta
	 * ejecucion. El Metodo tiene que ir en un bloque synchronized para proteger las
	 * variables internas de bimap.
	 */
	public final boolean contains(int physicalId) {
		synchronized (this) {
			return biMap.contains(physicalId);
		}
	}

	private void startExecution() {
		synchronized (this) {
			// TODO results.start();
			eventSource.sendEvent(new InternalEvent.ExecutionStarted());
			currentStep = getNextStep();
			turnAllNodes(Color.NO_COLOR);
			prepareStep();
			executionTimeOutTask.start();
		}
	}

	/*
	 * Envia un comando a todos los nodos con el color proporcionado. El Metodo
	 * tiene que ir en un bloque synchronized para proteger las variables internas
	 * de bimap.
	 */
	private void turnAllNodes(Color color) {
		synchronized (this) {
			for (int i = 0; i < biMap.size(); i++) {
				terminal.sendCommand(new CommandArgs(biMap.getPhysicalId(i), color, 0, 0), true);
			}
		}
	}

	/*
	 * Este metodo no necesita proteccion puesto que va a ser implementado por sus
	 * herederos.
	 */
	protected abstract Step getNextStep();

	/*
	 * Este metodo no necesita proteccion puesto que va a ser implementado por sus
	 * herederos.
	 */
	protected abstract boolean hasNextStep();

	/*
	 * Este metodo no es thread-safe.
	 */
	private void prepareStep() {
		++stepIndex;
		long maxDelay = 0;
		for (NodeConfiguration configuration : currentStep.getNodeConfigurationList()) {
			int physicalId = biMap.getPhysicalId(configuration.getLogicalId());
			long delay = configuration.getDelay();
			if (delay > maxDelay) {
				maxDelay = delay;
			}
			terminal.sendCommand(new CommandArgs(physicalId, configuration.getColor(), delay, stepIndex), true);
		}

		expressionTree = new ExpressionTree(currentStep.getExpression());
		try {
			Field f = Thread.class.getDeclaredField("target");
			f.setAccessible(true);
			((StepTimeOutTask) f.get(stepTimeOutTask)).setStepTimeOut(currentStep.getTimeOut(), stepIndex);
			f.setAccessible(false);
		} catch (Exception e) {
		}
	}

	/*
	 * Este metodo no es thread-safe.
	 */
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

		}
	}

	private void stepTimeOut(int stepIndex) {
		synchronized (this) {
			if (stepIndex == this.stepIndex) {
				eventSource.sendEvent(new InternalEvent.StepTimeOut());
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
	public abstract void close();

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

		public int getLogicalId(int physicalId) {
			return logicalIdNodes.get(physicalId);
		}

		public int getPhysicalId(int logicalId) {
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
					// TODO executionTimeOut
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
			try {
				while (running.get()) {
					Entry<Long, Integer> entry = messages.take();
					if (entry.getKey() > 0) {
						Thread.sleep(entry.getKey());
						stepTimeOut(entry.getValue());
					}
				}
			} catch (InterruptedException e) {
			}
		}

	}
}
