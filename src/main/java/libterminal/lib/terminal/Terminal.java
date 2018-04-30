package main.java.libterminal.lib.terminal;

import java.io.IOException;
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
import main.java.libterminal.patterns.observer.Event.ExternalEvent;
import main.java.libterminal.patterns.observer.Event.InternalEvent;
import main.java.libterminal.patterns.observer.Event.InternalEvent.IncomingPacket;
import main.java.libterminal.patterns.observer.Event.InternalEvent.InternalException;
import main.java.libterminal.patterns.observer.Event.InternalEvent.KeepAliveError;
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
	private volatile TerminalTask terminalTask;
	private volatile Thread task;

	private volatile boolean running;
	private volatile boolean closed;

	public Terminal(InetAddress interfaceAddress) {
		this.interfaceAddress = interfaceAddress;
		this.eventSource = new EventSource<>();
		this.closed = false;
		this.running = false;
	}

	public synchronized void start() {
		if (running)
			return;

		try {
			running = true;
			mutlticastReceiver = new MulticastReceiver(interfaceAddress, (InetAddress) InetAddress.getByName(QSYPacket.MULTICAST_ADDRESS), QSYPacket.MULTICAST_PORT);
			keepAlive = new KeepAlive();
			sender = new Sender();
			receiver = new Receiver();

			task = new Thread(terminalTask = new TerminalTask(), "Terminal");
			mutlticastReceiver.addListener(terminalTask);
			keepAlive.addListener(terminalTask);
			sender.addListener(terminalTask);
			receiver.addListener(terminalTask);
			task.start();
		} catch (Exception e) {
			stop();
			e.printStackTrace();
		}
	}

	public synchronized void stop() {
		if (!running)
			return;

		running = false;
		try {
			if (task != null) {
				task.interrupt();
				task.join();
			}
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
				keepAlive.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			if (mutlticastReceiver != null) {
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

	@Override
	public synchronized void close() {
		if (!closed) {
			closed = true;
			stop();
			eventSource.close();
		}
	}

	public synchronized int getConnectedNodes() {
		return (running) ? terminalTask.getConnectedNodes() : 0;
	}

	public synchronized void searchNodes() {
		if (running)
			mutlticastReceiver.acceptPackets(true);
	}

	public synchronized void finalizeNodesSearch() {
		if (running)
			mutlticastReceiver.acceptPackets(false);
	}

	public synchronized void sendCommand(CommandArgs params) {
		if (running)
			sender.command(QSYPacket.createCommandPacket(params));
	}

	@Override
	public void addListener(EventListener<ExternalEvent> eventListener) {
		eventSource.addListener(eventListener);
	}

	@Override
	public void removeListener(EventListener<ExternalEvent> eventListener) {
		eventSource.removeListener(eventListener);
	}

	private final class TerminalTask extends EventListener<InternalEvent> implements Runnable, InternalEventVisitor, AutoCloseable {

		private final TreeMap<Integer, Node> nodes;
		private volatile boolean running;

		public TerminalTask() {
			this.nodes = new TreeMap<>();
			this.running = true;
		}

		@Override
		public void run() {
			try {
				while (running) {
					try {
						InternalEvent event = getEvent();
						event.accept(this);
					} catch (InterruptedException e) {
						running = false;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				eventSource.sendEvent(new ExternalEvent.InternalException(e));
				new Thread(() -> {
					Terminal.this.close();
				}).start();
			} finally {
				close();
			}
		}

		@Override
		public synchronized void close() {
			for (Node node : nodes.values()) {
				try {
					removeNode(node, false);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			nodes.clear();
		}

		@Override
		public synchronized void visit(KeepAliveError event) throws IOException {
			if (nodes.containsKey(event.getPhysicalId())) {
				Node node = nodes.get(event.getPhysicalId());
				removeNode(node);
			}
		}

		@Override
		public synchronized void visit(IncomingPacket event) throws IOException {
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
			throw internalError.getException();
		}

		private void createNode(QSYPacket packet) throws IOException {
			Node node = new Node(packet);
			nodes.put(node.getPhysicalId(), node);
			keepAlive.newNode(node.getPhysicalId());
			sender.newNode(node.getPhysicalId(), node.getNodeSocketChannel());
			receiver.newNode(node.getPhysicalId(), node.getNodeSocketChannel());
			eventSource.sendEvent(new ExternalEvent.ConnectedNode(node.getPhysicalId(), node.getNodeAddress()));
		}

		private void removeNode(Node node, boolean notify) throws IOException {
			receiver.removeNode(node.getPhysicalId(), node.getNodeSocketChannel());
			sender.removeNode(node.getPhysicalId());
			keepAlive.removeNode(node.getPhysicalId());
			nodes.remove(node.getPhysicalId());
			node.close();
			if (notify)
				eventSource.sendEvent(new ExternalEvent.DisconnectedNode(node.getPhysicalId(), node.getNodeAddress()));
		}

		private void removeNode(Node node) throws IOException {
			removeNode(node, true);
		}

		public synchronized int getConnectedNodes() {
			return nodes.size();
		}

	}

}
