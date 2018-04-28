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
import main.java.libterminal.patterns.observer.EventSource;

/**
 * Maneja los paquetes que se reciven por multicast. No es Thread-Safe.
 */
public final class MulticastReceiver extends EventSource<InternalEvent> implements AutoCloseable {

	private final Thread thread;

	private final MulticastSocket socket;
	private final DatagramPacket packet;

	private volatile boolean acceptPackets;
	private boolean running;

	public MulticastReceiver(InetAddress interfaceAddress, InetAddress multicastAddress, int port) throws SocketException, IOException {
		this.socket = new MulticastSocket(port);
		this.socket.joinGroup(new InetSocketAddress(multicastAddress, port), NetworkInterface.getByInetAddress(interfaceAddress));

		this.packet = new DatagramPacket(new byte[QSYPacket.PACKET_SIZE], QSYPacket.PACKET_SIZE);
		this.running = true;
		this.acceptPackets = false;

		this.thread = new Thread(new MulticastReceiverTask(), "Multicast Receiver");
		this.thread.start();
	}

	public void acceptPackets(boolean acceptPackets) {
		if (running) {
			synchronized (this) {
				this.acceptPackets = acceptPackets;
			}
		}
	}

	@Override
	public void close() throws InterruptedException {
		if (running) {
			running = false;
			socket.close();
			thread.join();
		}
	}

	private final class MulticastReceiverTask implements Runnable {

		private boolean running = true;

		@Override
		public void run() {
			while (running) {
				try {
					socket.receive(packet);
					synchronized (this) {
						if (acceptPackets) {
							InetAddress sender = packet.getAddress();
							sendEvent(new InternalEvent.IncomingPacket(new QSYPacket(sender, packet.getData())));
						}
					}
				} catch (SocketException e) {
					running = false;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

}
