package ar.com.terminal;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.TreeSet;

import ar.com.terminal.Event.InternalEvent;
import ar.com.terminal.QSYPacket.PacketType;

final class MulticastReceiver extends EventSourceI<InternalEvent> implements AutoCloseable {

	private final Thread multicastReceiverTask;

	private final EventSource<InternalEvent> eventSource;
	private final MulticastSocket socket;
	private final DatagramPacket packet;

	private final TreeSet<Integer> nodes;
	private volatile boolean acceptPackets;

	private volatile boolean closed;

	public MulticastReceiver(InetAddress interfaceAddress, InetAddress multicastAddress, int port) throws SocketException, IOException {
		this.socket = new MulticastSocket(port);
		this.socket.joinGroup(new InetSocketAddress(multicastAddress, port), NetworkInterface.getByInetAddress(interfaceAddress));

		this.eventSource = new EventSource<>();
		this.packet = new DatagramPacket(new byte[QSYPacket.PACKET_SIZE], QSYPacket.PACKET_SIZE);
		this.closed = false;

		this.nodes = new TreeSet<>();
		this.acceptPackets = false;

		this.multicastReceiverTask = new Thread(new MulticastReceiverTask(), "MulticastReceiver");
		this.multicastReceiverTask.start();
	}

	public void acceptPackets(boolean acceptPackets) {
		synchronized (nodes) {
			this.acceptPackets = acceptPackets;
		}
	}

	public void removeNode(int physicalId) {
		synchronized (nodes) {
			nodes.remove(physicalId);
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
	public void close() throws InterruptedException {
		if (!closed) {
			closed = true;
			socket.close();
			try {
				multicastReceiverTask.join();
			} finally {
				eventSource.close();
			}
		}
	}

	private final class MulticastReceiverTask implements Runnable {

		private volatile boolean running = true;

		@Override
		public void run() {
			while (running) {
				try {
					socket.receive(packet);
					synchronized (nodes) {
						if (acceptPackets) {
							InetAddress sender = packet.getAddress();
							QSYPacket qsyPacket = new QSYPacket(sender, packet.getData());
							if (qsyPacket.getType() == PacketType.Hello && !nodes.contains(qsyPacket.getPhysicalId())) {
								nodes.add(qsyPacket.getPhysicalId());
								eventSource.sendEvent(new InternalEvent.IncomingPacket(qsyPacket));
							}
						}
					}
				} catch (SocketException e) {
					running = false;
				} catch (IllegalArgumentException e) {
					// No se pudo construir el paquete.
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

}
