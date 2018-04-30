package main.java.libterminal.lib.keepalive;

import java.util.TreeMap;

import main.java.libterminal.lib.node.Node;
import main.java.libterminal.lib.protocol.QSYPacket;
import main.java.libterminal.lib.protocol.QSYPacket.ToucheArgs;
import main.java.libterminal.patterns.observer.Event.ExternalEvent;
import main.java.libterminal.patterns.observer.Event.InternalEvent;
import main.java.libterminal.patterns.observer.Event.InternalEvent.IncomingPacket;
import main.java.libterminal.patterns.observer.Event.InternalEvent.InternalException;
import main.java.libterminal.patterns.observer.Event.InternalEvent.KeepAliveError;
import main.java.libterminal.patterns.observer.EventListener;
import main.java.libterminal.patterns.observer.EventSourceI;
import main.java.libterminal.patterns.visitor.event.InternalEventVisitor;

public final class MainController implements EventSourceI<ExternalEvent>, AutoCloseable {

	private final EventSource<ExternalEvent> eventSource;
	private final TreeMap<Integer, Node> nodes;
	private final Thread mainControllerTask;

	private volatile boolean closed;

	public MainController() {
		this.eventSource = new EventSource<>();
		this.nodes = new TreeMap<>();
		this.closed = false;
		this.mainControllerTask = new Thread(new MainControllerTask(), "MainController");
		this.mainControllerTask.start();
	}

	public int getConnectedNodesCount() {
		synchronized (nodes) {
			return nodes.size();
		}
	}

	@Override
	public void addListener(EventListener<ExternalEvent> eventListener) {
		eventSource.addListener(eventListener);
	}

	@Override
	public void removeListener(EventListener<ExternalEvent> eventListener) {
		eventSource.removeListener(eventListener);
	}

	@Override
	public void close() throws Exception {
		if (!closed) {
			closed = true;
			mainControllerTask.interrupt();
			try {
				mainControllerTask.join();
			} finally {
				for (Node node : nodes.values()) {
					try {
						node.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				nodes.clear();
			}
		}
	}

	private final class MainControllerTask extends EventListener<InternalEvent> implements Runnable, InternalEventVisitor {

		private volatile boolean running = true;

		@Override
		public void run() {
			while (running) {
				try {
					InternalEvent event = getEvent();
					event.accept(this);
				} catch (InterruptedException e) {
					running = false;
				} catch (Exception e) {
					eventSource.sendEvent(new ExternalEvent.InternalException(e));
				}
			}
		}

		@Override
		public void visit(IncomingPacket event) throws Exception {
			QSYPacket packet = event.getPacket();

			switch (packet.getType()) {
			case Hello:
				int physicalId = packet.getPhysicalId();
				if (!nodes.containsKey(physicalId))
					createNode(packet);
				break;
			case Touche:
				keepAlive.touche(packet.getPhysicalId());
				eventSource.sendEvent(new ExternalEvent.Touche(new ToucheArgs(packet.getPhysicalId(), packet.getDelay(), packet.getColor())));
				break;
			case Keepalive:
				keepAlive.keepAlive(packet.getPhysicalId());
				break;
			default:
				break;
			}
		}

		@Override
		public void visit(InternalException internalError) throws Exception {
		}

		@Override
		public void visit(KeepAliveError event) throws Exception {
		}
	}
}
