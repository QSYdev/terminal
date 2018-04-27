package main.java.test;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Scanner;

import main.java.libterminal.lib.protocol.QSYPacket.CommandArgs;
import main.java.libterminal.lib.routine.Color;
import main.java.libterminal.lib.terminal.Terminal;
import main.java.libterminal.patterns.observer.Event.ConnectedNode;
import main.java.libterminal.patterns.observer.Event.DisconnectedNode;
import main.java.libterminal.patterns.observer.Event.ExternalEvent;
import main.java.libterminal.patterns.observer.Event.Touche;
import main.java.libterminal.patterns.observer.EventListener;
import main.java.libterminal.patterns.visitor.event.ExternalEventVisitor;

public class TerminalTest {

	private static Terminal terminal;

	public static void main(String[] args) throws UnknownHostException, InterruptedException {
		StressTask streesTask = null;
		final EventTask task = new EventTask();
		final Thread thread = new Thread(task, "Task");
		thread.start();

		terminal = new Terminal((Inet4Address) Inet4Address.getByName("192.168.1.106"));
		terminal.addListener(task);
		final CommandArgs params = new CommandArgs(1, Color.CYAN, 500, 1, false, true);

		final Scanner scanner = new Scanner(System.in);
		char command = 0;
		do {
			command = scanner.next().charAt(0);
			switch (command) {
			case 's':
				terminal.start();
				break;
			case 't':
				terminal.stop();
				break;
			case 'n':
				terminal.searchNodes();
				break;
			case 'e':
				if (streesTask == null)
					streesTask = new StressTask();
				break;
			case 'f':
				terminal.finalizeNodesSearch();
				break;
			case 'c':
				terminal.sendCommand(params);
				break;
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
					final ExternalEvent event = getEvent();
					event.accept(this);
				} catch (final InterruptedException e) {
					running = false;
				}
			}
		}

		@Override
		public void visit(final ConnectedNode event) {
			System.out.println("Se ha conectado el nodo " + event.getPhysicalId());
		}

		@Override
		public void visit(final DisconnectedNode event) {
			System.err.println("Se ha desconectado el nodo " + event.getPhysicalId());
		}

		@Override
		public void visit(final Touche event) {
			System.out.println("Se ha tocado el nodo " + event.getToucheArgs().getPhysicalId());
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
					for (int i = 1; i <= terminal.connectedNodesAmount(); i++) {
						terminal.sendCommand(new CommandArgs(i, Color.CYAN, 500, 3));
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
