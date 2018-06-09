package test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Scanner;

import ar.com.terminal.Color;
import ar.com.terminal.Event.ExternalEvent;
import ar.com.terminal.Event.ExternalEvent.ConnectedNode;
import ar.com.terminal.Event.ExternalEvent.DisconnectedNode;
import ar.com.terminal.Event.ExternalEvent.ExecutionFinished;
import ar.com.terminal.Event.ExternalEvent.ExecutionInterrupted;
import ar.com.terminal.Event.ExternalEvent.ExecutionStarted;
import ar.com.terminal.Event.ExternalEvent.ExternalEventVisitor;
import ar.com.terminal.Event.ExternalEvent.StepTimeOut;
import ar.com.terminal.Event.ExternalEvent.Touche;
import ar.com.terminal.EventListener;
import ar.com.terminal.QSYPacket.CommandArgs;
import ar.com.terminal.Routine;
import ar.com.terminal.RoutineManager;
import ar.com.terminal.Terminal;

public final class TerminalTest {

	private static Terminal terminal;
	private static Routine routine;
	static {
		// ArrayList<Step> steps = new ArrayList<>(2);
		// {
		// LinkedList<NodeConfiguration> nodeConfigurationList = new LinkedList<>();
		// nodeConfigurationList.add(new NodeConfiguration(0, 500, Color.RED));
		// nodeConfigurationList.add(new NodeConfiguration(1, 500, Color.GREEN));
		// steps.add(new Step(nodeConfigurationList, 0, "0|1", false));
		// }
		// {
		// LinkedList<NodeConfiguration> nodeConfigurationList = new LinkedList<>();
		// nodeConfigurationList.add(new NodeConfiguration(0, 500, Color.YELLOW));
		// nodeConfigurationList.add(new NodeConfiguration(1, 500, Color.BLUE));
		// steps.add(new Step(nodeConfigurationList, 0, "0&1", false));
		// }
		// routine = new Routine(2, 2, 0, steps, "Prueba");
		try {
			routine = RoutineManager.loadRoutine("/Users/agus/eclipse-workspace/desktop/resources/routine1.json");
			RoutineManager.validateRoutine(routine);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		StressTask streesTask = null;
		final EventTask task = new EventTask();
		final Thread thread = new Thread(task, "Task");
		thread.start();

		terminal = new Terminal("192.168.1.112");
		terminal.addListener(task);

		Scanner scanner = new Scanner(System.in);
		char command = 0;
		do {
			try {
				command = scanner.next().charAt(0);
				switch (command) {
				case 's':
					terminal.start();
					break;
				case 'n':
					terminal.searchNodes();
					break;
				case 'e':
					if (streesTask == null)
						streesTask = new StressTask();
					break;
				case 'f':
					terminal.finalizeNodesSearching();
					break;
				case 'c':
					System.out.println(terminal.getConnectedNodes());
					break;
				case 'r':
					terminal.startCustomRoutine(routine);
					break;
				case 'y':
					terminal.stopRoutine();
					break;
				case 'p':
					ArrayList<Color> playersAndColors = new ArrayList<>();
					playersAndColors.add(Color.RED);
					playersAndColors.add(Color.GREEN);
					playersAndColors.add(Color.BLUE);
					terminal.startPlayerExecution(3, playersAndColors, false, 500, 1000, true, 10, 0);
					break;
				case 'b':
					terminal.sendCommand(new CommandArgs(19, Color.RED, 500, 1));
					break;
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		} while (command != 'q');

		if (streesTask != null)
			streesTask.close();
		scanner.close();
		terminal.close();
		thread.interrupt();
		thread.join();

	}

	private static final class EventTask extends EventListener<ExternalEvent> implements Runnable, ExternalEventVisitor {

		private volatile boolean running;

		public EventTask() {
			this.running = true;
		}

		@Override
		public void run() {
			while (running) {
				try {
					ExternalEvent event = getEvent();
					event.accept(this);
				} catch (final InterruptedException e) {
					running = false;
				}
			}
		}

		@Override
		public void visit(ConnectedNode event) {
			System.out.println("Se ha conectado el nodo " + event.getPhysicalId());
		}

		@Override
		public void visit(DisconnectedNode event) {
			System.err.println("Se ha desconectado el nodo " + event.getPhysicalId());
		}

		@Override
		public void visit(Touche event) {
			System.out.println("Se ha tocado el nodo " + event.getToucheArgs().getPhysicalId());
		}

		@Override
		public void visit(ExecutionStarted event) {
			System.out.println("Rutina Iniciada");
		}

		@Override
		public void visit(ExecutionFinished event) {
			System.out.println("Rutina terminada");
		}

		@Override
		public void visit(StepTimeOut event) {
			System.out.println("Step Time Out");
		}

		@Override
		public void visit(ExecutionInterrupted event) {
			System.out.println(event.getReason());
		}

	}

	private static final class StressTask implements Runnable, AutoCloseable {

		private boolean running;
		private final Thread thread;

		public StressTask() {
			this.running = true;
			this.thread = new Thread(this, "Stress Task");
			thread.start();
		}

		@Override
		public void run() {
			while (running) {
				try {
					Thread.sleep(1000);
					for (int i = 1; i <= terminal.getConnectedNodes(); i++) {
						terminal.sendCommand(new CommandArgs(17 + i, Color.CYAN, 500, 3));
					}
				} catch (InterruptedException e) {
					running = false;
				}
			}
		}

		@Override
		public void close() {
			thread.interrupt();
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

}
