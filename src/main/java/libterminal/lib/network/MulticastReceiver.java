package main.java.libterminal.lib.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;

import main.java.libterminal.lib.protocol.QSYPacket;
import main.java.libterminal.patterns.observer.Event.InternalEvent;
import main.java.libterminal.patterns.observer.EventListener;
import main.java.libterminal.patterns.observer.EventSourceI;

/**
 * Maneja los paquetes que se reciven por multicast. No es Thread-Safe.
 */
public final class MulticastReceiver implements EventSourceI<InternalEvent>, AutoCloseable {

	private final Thread multicastReceiverTask;

	private final EventSource<InternalEvent> eventSource;
	private final MulticastSocket socket;
	private final DatagramPacket packet;

	private volatile Boolean acceptPackets;
	private volatile boolean closed;

	public MulticastReceiver(InetAddress interfaceAddress, InetAddress multicastAddress, int port) throws SocketException, IOException {
		this.socket = new MulticastSocket(port);
		this.socket.joinGroup(new InetSocketAddress(multicastAddress, port), NetworkInterface.getByInetAddress(interfaceAddress));

		this.eventSource = new EventSource<>();
		this.packet = new DatagramPacket(new byte[QSYPacket.PACKET_SIZE], QSYPacket.PACKET_SIZE);
		this.closed = false;
		this.acceptPackets = false;

		this.multicastReceiverTask = new Thread(new MulticastReceiverTask(), "MulticastReceiver");
		this.multicastReceiverTask.start();
	}

	public void acceptPackets(boolean acceptPackets) {
		synchronized (this.acceptPackets) {
			this.acceptPackets = acceptPackets;
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
					synchronized (acceptPackets) {
						if (acceptPackets) {
							InetAddress sender = packet.getAddress();
							eventSource.sendEvent(new InternalEvent.IncomingPacket(new QSYPacket(sender, packet.getData())));
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
