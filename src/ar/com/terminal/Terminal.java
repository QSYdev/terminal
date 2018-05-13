package ar.com.terminal;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.TreeMap;

import ar.com.terminal.Event.ExternalEvent;
import ar.com.terminal.Event.InternalEvent.CloseSignal;
import ar.com.terminal.Event.InternalEvent.IncomingPacket;
import ar.com.terminal.Event.InternalEvent.KeepAliveError;
import ar.com.terminal.QSYPacket.CommandArgs;
import ar.com.terminal.QSYPacket.ToucheArgs;

/**
 * La clase Terminal es la interfaz de comunicacion externa con las demas
 * aplicaciones que decidan utilizar este sistema. Es una clase ThreadSafe por
 * lo que puede ejecutarse asincronicamente desde multiples threads al mismo
 * tiempo.
 */
public final class Terminal extends EventSourceI<ExternalEvent> implements AutoCloseable {

	private final InetAddress interfaceAddress;
	private final EventSource<ExternalEvent> eventSource;
	private final TreeMap<Integer, Node> nodes;

	private volatile MulticastReceiver mutlticastReceiver;
	private volatile KeepAlive keepAlive;
	private volatile Sender sender;
	private volatile Receiver receiver;
	private volatile MainController mainController;
	private volatile Executor executor;

	private volatile boolean running;
	private volatile boolean closed;

	public Terminal(String interfaceAddress) throws UnknownHostException {
		this.interfaceAddress = (Inet4Address) Inet4Address.getByName(interfaceAddress);
		this.eventSource = new EventSource<>();
		this.nodes = new TreeMap<>();
		this.closed = false;
		this.running = false;
	}

	/**
	 * Inicia el sistema, en caso de que exista una excepcion interna se garantiza
	 * que se volvera al estado por defecto. Este metodo es necesario antes de
	 * ejecutar cualquier otro dentro de esta clase.
	 */
	public synchronized void start() throws Exception {
		if (closed || running)
			return;

		try {
			executor = null;
			mutlticastReceiver = new MulticastReceiver(interfaceAddress, (InetAddress) InetAddress.getByName(QSYPacket.MULTICAST_ADDRESS), QSYPacket.MULTICAST_PORT);
			receiver = new Receiver();
			sender = new Sender();
			keepAlive = new KeepAlive();
			mainController = new MainController(this);
			keepAlive.addListener(mainController);
			sender.addListener(mainController);
			receiver.addListener(mainController);
			mutlticastReceiver.addListener(mainController);
			running = true;
		} catch (Exception e) {
			visit(new CloseSignal());
			throw e;
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

	/**
	 * Devuelve la cantidad de nodos conectados al sistema.
	 */
	public synchronized int getConnectedNodes() {
		return (running) ? nodes.size() : 0;
	}

	/**
	 * Inicia la busqueda de nuevos nodos conectados a la red.
	 */
	public synchronized void searchNodes() {
		if (running)
			mutlticastReceiver.acceptPackets(true);
	}

	/**
	 * Finaliza la busqueda de nodos conectados a la red.
	 */
	public synchronized void finalizeNodesSearching() {
		if (running)
			mutlticastReceiver.acceptPackets(false);
	}

	/**
	 * Inicia una nueva rutina custom con los parametros proporcionados. En caso de
	 * que una rutina ya estuviera ejecutandose, se finaliza y se inicia la nueva.
	 */
	public synchronized void startCustomRoutine() {
		if (!running)
			return;

		if (executor != null)
			executor.close();

		// TODO crear la nueva rutina.
		executor.addListener(mainController);
	}

	/**
	 * Finaliza la rutina que se esta ejecutando actualmente. En caso de que no
	 * hubiera ninguna, simplemente se ignora este metodo.
	 */
	public synchronized void stopRoutine() {
		if (running && executor != null) {
			executor.close();
			executor = null;
		}
	}

	/**
	 * Envia un comando con los parametros especificados.
	 */
	public synchronized void sendCommand(CommandArgs params) {
		sendCommand(params, false);
	}

	synchronized void sendCommand(CommandArgs params, boolean isExecutor) {
		if (!running)
			return;

		if (isExecutor || executor == null || !executor.contains(params.getPhysicialId()))
			sender.command(QSYPacket.createCommandPacket(params));
	}

	synchronized void visit(KeepAliveError event) throws Exception {
		if (!running)
			return;

		Node node = nodes.get(event.getPhysicalId());
		if (node != null)
			removeNode(node);

	}

	synchronized void visit(IncomingPacket event) throws Exception {
		if (!running)
			return;

		QSYPacket packet = event.getPacket();

		switch (packet.getType()) {
		case Hello:
			int physicalId = packet.getPhysicalId();
			if (!nodes.containsKey(physicalId))
				createNode(packet);
			break;
		case Touche:
			keepAlive.touche(packet.getPhysicalId());
			executor.touche(packet.getPhysicalId(), packet.getNumberOfStep(), packet.getColor(), packet.getDelay());
			eventSource.sendEvent(new ExternalEvent.Touche(new ToucheArgs(packet.getPhysicalId(), packet.getDelay(), packet.getColor())));
			break;
		case Keepalive:
			keepAlive.keepAlive(packet.getPhysicalId());
			break;
		default:
			break;
		}
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

		try {
			if (executor != null)
				executor.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		executor = null;
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

	private void createNode(QSYPacket packet) throws Exception {
		try {
			Node node = new Node(packet);
			nodes.put(node.getPhysicalId(), node);
			keepAlive.newNode(node.getPhysicalId());
			sender.newNode(node.getPhysicalId(), node.getNodeSocketChannel());
			receiver.newNode(node.getPhysicalId(), node.getNodeSocketChannel());
			eventSource.sendEvent(new ExternalEvent.ConnectedNode(node.getPhysicalId(), node.getNodeAddress()));
		} catch (Exception e) {
			mutlticastReceiver.removeNode(packet.getPhysicalId());
			throw e;
		}
	}

	private void removeNode(Node node) throws Exception {
		if (executor != null && executor.contains(node.getPhysicalId())) {
			executor.close();
			executor = null;
		}
		receiver.removeNode(node.getPhysicalId(), node.getNodeSocketChannel());
		keepAlive.removeNode(node.getPhysicalId());
		sender.removeNode(node.getPhysicalId());
		mutlticastReceiver.removeNode(node.getPhysicalId());
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
