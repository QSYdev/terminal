package main.java.libterminal.lib.terminal;

import java.net.InetAddress;
import java.util.TreeMap;

import main.java.libterminal.lib.keepalive.KeepAlive;
import main.java.libterminal.lib.network.MulticastReceiver;
import main.java.libterminal.lib.network.Receiver;
import main.java.libterminal.lib.network.Sender;
import main.java.libterminal.lib.node.Node;
import main.java.libterminal.lib.protocol.QSYPacket;
import main.java.libterminal.lib.protocol.QSYPacket.CommandArgs;
import main.java.libterminal.lib.protocol.QSYPacket.ToucheArgs;
import main.java.libterminal.patterns.observer.Event;
import main.java.libterminal.patterns.observer.Event.ExternalEvent;
import main.java.libterminal.patterns.observer.Event.IncomingPacket;
import main.java.libterminal.patterns.observer.Event.InternalEvent;
import main.java.libterminal.patterns.observer.Event.InternalException;
import main.java.libterminal.patterns.observer.Event.KeepAliveError;
import main.java.libterminal.patterns.observer.EventListener;
import main.java.libterminal.patterns.observer.EventSourceI;
import main.java.libterminal.patterns.visitor.event.InternalEventVisitor;

public final class Terminal implements EventSourceI<ExternalEvent>, AutoCloseable {

	private final InetAddress interfaceAddress;
	private final EventSource<ExternalEvent> eventSource;

	private volatile MulticastReceiver mutlticastReceiver;
	private volatile KeepAlive keepAlive;
	private volatile Sender sender;
	private volatile Receiver receiver;
	private volatile TerminalTask task;

	private volatile boolean running;
	private volatile boolean closed;
	private volatile int connectedNodes;

	public Terminal(InetAddress interfaceAddress) {
		this.interfaceAddress = interfaceAddress;
		this.eventSource = new EventSource<>();
		this.closed = false;
		this.running = false;
		this.connectedNodes = 0;
	}

	public synchronized void start() {
		try {
			running = true;
			mutlticastReceiver = new MulticastReceiver(interfaceAddress, (InetAddress) InetAddress.getByName(QSYPacket.MULTICAST_ADDRESS), QSYPacket.MULTICAST_PORT);
			keepAlive = new KeepAlive();
			sender = new Sender();
			receiver = new Receiver();

			task = new TerminalTask();
			mutlticastReceiver.addListener(task);
			keepAlive.addListener(task);
			sender.addListener(task);
			receiver.addListener(task);
		} catch (Exception e) {
			stop();
			e.printStackTrace();
		}
	}

	public synchronized void stop() {
		running = false;
		try {
			if (task != null)
				task.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			if (receiver != null)
				receiver.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			if (sender != null)
				sender.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			if (keepAlive != null) {
				keepAlive.removeListener(task);
				keepAlive.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			if (mutlticastReceiver != null) {
				mutlticastReceiver.removeListener(task);
				mutlticastReceiver.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		mutlticastReceiver = null;
		keepAlive = null;
		receiver = null;
		sender = null;
		task = null;
	}

	public synchronized int connectedNodesAmount() {
		return connectedNodes;
	}

	protected synchronized void setConnectedNodes(int connectedNodes) {
		this.connectedNodes = connectedNodes;
	}

	public synchronized boolean isRunning() {
		return running;
	}

	@Override
	public synchronized void close() {
		if (!closed) {
			closed = true;
			stop();
		}
	}

	public synchronized void searchNodes() {
		mutlticastReceiver.acceptPackets(true);
	}

	public synchronized void finalizeNodesSearch() {
		mutlticastReceiver.acceptPackets(false);
	}

	public synchronized void sendCommand(CommandArgs params) {
		try {
			sender.command(QSYPacket.createCommandPacket(params));
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	@Override
	public synchronized void addListener(EventListener<ExternalEvent> eventListener) {
		eventSource.addListener(eventListener);
	}

	@Override
	public synchronized void removeListener(EventListener<ExternalEvent> eventListener) {
		eventSource.removeListener(eventListener);
	}

	protected synchronized void sendEvent(ExternalEvent event) {
		eventSource.sendEvent(event);
	}

	private final class TerminalTask extends EventListener<InternalEvent> implements Runnable, InternalEventVisitor, AutoCloseable {

		private final TreeMap<Integer, Node> nodes;

		private final Thread thread;
		private boolean running;

		public TerminalTask() {
			this.nodes = new TreeMap<>();
			this.running = true;
			thread = new Thread(this, "Terminal");
			thread.start();
		}

		@Override
		public void run() {
			while (running) {
				try {
					final InternalEvent event = getEvent();
					event.accept(this);
				} catch (final InterruptedException e) {
					running = false;
				}
			}
			for (final Node node : nodes.values())
				node.close();
			nodes.clear();
			setConnectedNodes(nodes.size());
		}

		@Override
		public void close() {
			thread.interrupt();
			try {
				thread.join();
			} catch (final InterruptedException e) {
				// Gravisimo problema.
				e.printStackTrace();
			}
		}

		@Override
		public void visit(final KeepAliveError event) {
			if (nodes.containsKey(event.getPhysicalId())) {
				final Node node = nodes.get(event.getPhysicalId());
				removeNode(node);
			}
		}

		@Override
		public void visit(final IncomingPacket event) {
			final QSYPacket packet = event.getPacket();

			switch (packet.getType()) {
			case Hello:
				final int physicalId = packet.getPhysicalId();
				if (!nodes.containsKey(physicalId))
					createNode(packet);
				break;
			case Touche:
				keepAlive.touche(packet.getPhysicalId());
				eventSource.sendEvent(new Event.Touche(new ToucheArgs(packet.getPhysicalId(), packet.getDelay(), packet.getColor())));
				break;
			case Keepalive:
				keepAlive.keepAlive(packet.getPhysicalId());
				break;
			default:
				break;
			}
		}

		@Override
		public void visit(InternalException internalError) {
			// TODO
		}

		private void createNode(final QSYPacket packet) {
			try {
				final Node node = new Node(packet);
				nodes.put(node.getPhysicalId(), node);
				connectedNodesAmount.set(nodes.size());
				keepAlive.newNode(node.getPhysicalId());
				sender.newNode(node.getPhysicalId(), node.getNodeSocketChannel());
				receiver.newNode(node.getPhysicalId(), node.getNodeSocketChannel());
				eventSource.sendEvent(new Event.ConnectedNode(node.getPhysicalId(), node.getNodeAddress()));
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}

		private void removeNode(final Node node) {
			receiver.removeNode(node.getPhysicalId(), node.getNodeSocketChannel());
			sender.removeNode(node.getPhysicalId());
			keepAlive.removeNode(node.getPhysicalId());
			nodes.remove(node.getPhysicalId());
			connectedNodesAmount.set(nodes.size());
			node.close();
			eventSource.sendEvent(new Event.DisconnectedNode(node.getPhysicalId(), node.getNodeAddress()));
		}

	}

}
