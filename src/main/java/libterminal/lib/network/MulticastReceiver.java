package main.java.libterminal.lib.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;

import main.java.libterminal.lib.protocol.QSYPacket;
import main.java.libterminal.patterns.command.TerminalRunnable;
import main.java.libterminal.patterns.observer.Event.InternalEvent;
import main.java.libterminal.patterns.observer.EventSourceI.EventSource;

/**
 * Maneja los paquetes que se reciven por multicast. No es Thread-Safe.
 */
public final class MulticastReceiver extends EventSource<InternalEvent> implements AutoCloseable {

	private final Thread multicastReceiverTask;

	private final MulticastSocket socket;
	private final DatagramPacket packet;

	private volatile Boolean acceptPackets;
	private volatile boolean closed;

	public MulticastReceiver(InetAddress interfaceAddress, InetAddress multicastAddress, int port) throws SocketException, IOException {
		this.socket = new MulticastSocket(port);
		this.socket.joinGroup(new InetSocketAddress(multicastAddress, port), NetworkInterface.getByInetAddress(interfaceAddress));

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
	public void close() throws InterruptedException {
		if (!closed) {
			closed = true;
			socket.close();
			multicastReceiverTask.join();
		}
	}

	private final class MulticastReceiverTask extends TerminalRunnable {

		private boolean running = true;

		@Override
		protected void runTerminalTask() throws Exception {
			while (running) {
				try {
					socket.receive(packet);
					synchronized (acceptPackets) {
						if (acceptPackets) {
							InetAddress sender = packet.getAddress();
							sendEvent(new InternalEvent.IncomingPacket(new QSYPacket(sender, packet.getData())));
						}
					}
				} catch (SocketException e) {
					running = false;
				}
			}
		}

		@Override
		protected void handleError(Exception e) {
			sendEvent(new InternalEvent.InternalException(e));
		}

	}

}
