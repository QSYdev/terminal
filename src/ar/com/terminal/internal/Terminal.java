package ar.com.terminal.internal;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.TreeMap;

import ar.com.terminal.internal.InternalEvent.CloseSignal;
import ar.com.terminal.internal.InternalEvent.IncomingPacket;
import ar.com.terminal.internal.InternalEvent.InternalException;
import ar.com.terminal.internal.InternalEvent.KeepAliveError;
import ar.com.terminal.shared.EventListener;
import ar.com.terminal.shared.ExternalEvent;
import ar.com.terminal.shared.QSYPacket;
import ar.com.terminal.shared.QSYPacket.CommandArgs;
import ar.com.terminal.shared.QSYPacket.ToucheArgs;

public final class Terminal implements EventSourceI<ExternalEvent>, AutoCloseable {

	private final InetAddress interfaceAddress;
	private final EventSource<ExternalEvent> eventSource;
	private final TreeMap<Integer, Node> nodes;

	private volatile MulticastReceiver mutlticastReceiver;
	private volatile KeepAlive keepAlive;
	private volatile Sender sender;
	private volatile Receiver receiver;
	private volatile MainController mainController;

	private volatile boolean running;
	private volatile boolean closed;

	public Terminal(String interfaceAddress) throws UnknownHostException {
		this.interfaceAddress = (Inet4Address) Inet4Address.getByName(interfaceAddress);
		this.eventSource = new EventSource<>();
		this.nodes = new TreeMap<>();
		this.closed = false;
		this.running = false;
	}

	public synchronized void start() {
		if (closed || running)
			return;

		try {
			running = true;
			mainController = new MainController(this);
			mutlticastReceiver = new MulticastReceiver(interfaceAddress, (InetAddress) InetAddress.getByName(QSYPacket.MULTICAST_ADDRESS), QSYPacket.MULTICAST_PORT);
			keepAlive = new KeepAlive();
			sender = new Sender();
			receiver = new Receiver();

			keepAlive.addListener(mainController);
			sender.addListener(mainController);
			receiver.addListener(mainController);
			mutlticastReceiver.addListener(mainController);
		} catch (Exception e) {
			e.printStackTrace();
			eventSource.sendEvent(new ExternalEvent.InternalException(e));
		}
	}

	@Override
	public void close() {
		synchronized (this) {
			if (!closed) {
				closed = true;
				running = false;
			} else {
				return;
			}
		}
		if (mainController != null) {
			mainController.close();
			mainController = null;
		}
	}

	public synchronized int getConnectedNodes() {
		return (running) ? nodes.size() : 0;
	}

	public synchronized void searchNodes() {
		if (running)
			mutlticastReceiver.acceptPackets(true);
	}

	public synchronized void finalizeNodesSearching() {
		if (running)
			mutlticastReceiver.acceptPackets(false);
	}

	public synchronized void sendCommand(CommandArgs params) {
		if (running)
			sender.command(QSYPacket.createCommandPacket(params));
	}

	synchronized void visit(KeepAliveError event) {
		if (!running)
			return;

		try {
			if (nodes.containsKey(event.getPhysicalId())) {
				Node node = nodes.get(event.getPhysicalId());
				removeNode(node);
			}
		} catch (Exception e) {
			eventSource.sendEvent(new ExternalEvent.InternalException(e));
		}

	}

	synchronized void visit(IncomingPacket event) {
		if (!running)
			return;

		try {
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
		} catch (Exception e) {
			eventSource.sendEvent(new ExternalEvent.InternalException(e));
		}
	}

	synchronized void visit(InternalException internalError) {
		if (running)
			eventSource.sendEvent(new ExternalEvent.InternalException(internalError.getException()));
	}

	synchronized void visit(CloseSignal event) {
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
			if (keepAlive != null)
				keepAlive.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			if (mutlticastReceiver != null)
				mutlticastReceiver.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		mutlticastReceiver = null;
		keepAlive = null;
		receiver = null;
		sender = null;

		for (Node node : nodes.values()) {
			try {
				node.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		nodes.clear();
		eventSource.close();
	}

	private void createNode(QSYPacket packet) throws IOException {
		Node node = new Node(packet);
		nodes.put(node.getPhysicalId(), node);
		keepAlive.newNode(node.getPhysicalId());
		sender.newNode(node.getPhysicalId(), node.getNodeSocketChannel());
		receiver.newNode(node.getPhysicalId(), node.getNodeSocketChannel());
		eventSource.sendEvent(new ExternalEvent.ConnectedNode(node.getPhysicalId(), node.getNodeAddress()));
	}

	private void removeNode(Node node) throws IOException {
		receiver.removeNode(node.getPhysicalId(), node.getNodeSocketChannel());
		sender.removeNode(node.getPhysicalId());
		keepAlive.removeNode(node.getPhysicalId());
		nodes.remove(node.getPhysicalId());
		node.close();
		eventSource.sendEvent(new ExternalEvent.DisconnectedNode(node.getPhysicalId(), node.getNodeAddress()));
	}

	@Override
	public void addListener(EventListener<ExternalEvent> eventListener) {
		eventSource.addListener(eventListener);
	}

	@Override
	public void removeListener(EventListener<ExternalEvent> eventListener) {
		eventSource.removeListener(eventListener);
	}

}
