package main.java.libterminal.lib.terminal;

import java.io.IOException;
import java.net.InetAddress;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

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
import main.java.libterminal.patterns.observer.Event.KeepAliveError;
import main.java.libterminal.patterns.observer.EventListener;
import main.java.libterminal.patterns.observer.EventSource;
import main.java.libterminal.patterns.visitor.event.InternalEventVisitor;

public final class Terminal implements AutoCloseable {

	private final InetAddress interfaceAddress;
	private final EventSource<ExternalEvent> eventSource;

	private volatile MulticastReceiver mutlticastReceiver;
	private volatile KeepAlive keepAlive;
	private volatile Sender sender;
	private volatile Receiver receiver;
	private volatile TerminalTask task;

	private volatile Boolean running;

	public Terminal(final InetAddress interfaceAddress) {
		this.eventSource = new EventSource<>();
		this.interfaceAddress = interfaceAddress;

		this.running = false;
	}

	public void start() {
		synchronized (running) {
			if (!running) {
				running = true;
				try {
					mutlticastReceiver = new MulticastReceiver(interfaceAddress, (InetAddress) InetAddress.getByName(QSYPacket.MULTICAST_ADDRESS), QSYPacket.MULTICAST_PORT);
					keepAlive = new KeepAlive();
					sender = new Sender();
					receiver = new Receiver();

					task = new TerminalTask();
					mutlticastReceiver.addListener(task);
					keepAlive.addListener(task);
					receiver.addListener(task);
				} catch (final IOException e) {
					stop();
					e.printStackTrace();
				}
			}
		}

	}

	public void stop() {
		synchronized (running) {
			if (running) {
				running = false;

				if (task != null) {
					task.close();
				}

				if (receiver != null) {
					receiver.close();
				}

				if (sender != null) {
					sender.close();
				}

				if (keepAlive != null) {
					keepAlive.removeListener(task);
					keepAlive.close();
				}

				if (mutlticastReceiver != null) {
					mutlticastReceiver.removeListener(task);
					mutlticastReceiver.close();
				}

				mutlticastReceiver = null;
				keepAlive = null;
				receiver = null;
				sender = null;
				task = null;
			}
		}
	}

	public int connectedNodesAmount() {
		synchronized (running) {
			if (running)
				return task.getConnectedNodesAmount();
			else
				return 0;
		}
	}

	@Override
	public void close() {
		stop();
	}

	public void addListener(final EventListener<ExternalEvent> listener) {
		eventSource.addListener(listener);
	}

	public void removeListener(final EventListener<ExternalEvent> listener) {
		eventSource.removeListener(listener);
	}

	public void searchNodes() {
		synchronized (running) {
			if (running) {
				mutlticastReceiver.acceptPackets(true);
			}
		}
	}

	public void finalizeNodesSearch() {
		synchronized (running) {
			if (running) {
				mutlticastReceiver.acceptPackets(false);
			}
		}
	}

	public void sendCommand(final CommandArgs params) {
		synchronized (running) {
			if (running) {
				try {
					sender.command(QSYPacket.createCommandPacket(params));
				} catch (final IllegalArgumentException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private final class TerminalTask extends EventListener<InternalEvent> implements Runnable, InternalEventVisitor, AutoCloseable {

		private final TreeMap<Integer, Node> nodes;
		private final AtomicInteger connectedNodesAmount;

		private final Thread thread;
		private boolean running;

		public TerminalTask() {
			this.nodes = new TreeMap<>();
			connectedNodesAmount = new AtomicInteger(nodes.size());
			this.running = true;
			thread = new Thread(this, "Terminal");
			thread.start();
		}

		public int getConnectedNodesAmount() {
			return connectedNodesAmount.get();
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
			connectedNodesAmount.set(nodes.size());
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
