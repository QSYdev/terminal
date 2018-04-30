package main.java.libterminal.lib.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;

import main.java.libterminal.lib.protocol.QSYPacket;
import main.java.libterminal.patterns.command.Command;
import main.java.libterminal.patterns.observer.Event.InternalEvent;
import main.java.libterminal.patterns.observer.EventListener;
import main.java.libterminal.patterns.observer.EventSourceI;

/**
 * Envia asincronicamente paquetes a los diferentes nodos registrados. No es
 * Thread-Safe.
 */
public final class Sender implements EventSourceI<InternalEvent>, AutoCloseable {

	private final EventSource<InternalEvent> eventSource;
	private final LinkedBlockingQueue<Command> pendingTasks;

	private final TreeMap<Integer, SocketChannel> nodes;
	private final ByteBuffer byteBuffer;

	private final Thread senderTask;

	private volatile boolean closed;

	public Sender() {
		this.eventSource = new EventSource<>();
		this.pendingTasks = new LinkedBlockingQueue<>();

		this.closed = false;

		this.nodes = new TreeMap<>();
		this.byteBuffer = ByteBuffer.allocate(QSYPacket.PACKET_SIZE);

		this.senderTask = new Thread(new SenderTask(), "Sender");
		this.senderTask.start();
	}

	public void newNode(int physicalId, SocketChannel socket) {
		synchronized (nodes) {
			if (!nodes.containsKey(physicalId) || !nodes.get(physicalId).equals(socket))
				nodes.put(physicalId, socket);
		}
	}

	public void command(QSYPacket packet) {
		pendingTasks.add(new SenderCommand(packet));
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
			senderTask.interrupt();

			try {
				senderTask.join();
			} finally {
				pendingTasks.clear();
				nodes.clear();
				byteBuffer.clear();
				eventSource.close();
			}
		}
	}

	private final class SenderTask implements Runnable {

		private volatile boolean running = true;

		@Override
		public void run() {
			while (running) {
				try {
					Command task = pendingTasks.take();
					task.execute();
				} catch (InterruptedException e) {
					running = false;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

	private final class SenderCommand extends Command {

		private static final int MAX_TRIES = 256;
		private final QSYPacket packet;

		public SenderCommand(QSYPacket packet) {
			this.packet = packet;
		}

		@Override
		public void execute() throws Exception {
			if (packet.getType() != QSYPacket.PacketType.Command)
				return;

			SocketChannel channel;

			synchronized (nodes) {
				channel = nodes.get(packet.getPhysicalId());
			}

			if (channel != null) {
				byteBuffer.put(packet.getRawData());
				byteBuffer.flip();
				byte bytesTransmitted = 0;
				short tries = 0;
				while (bytesTransmitted < QSYPacket.PACKET_SIZE && tries++ < MAX_TRIES)
					bytesTransmitted += channel.write(byteBuffer);

				if (bytesTransmitted != QSYPacket.PACKET_SIZE)
					throw new IOException("<< SENDER >> El paquete hacia el nodo " + packet.getPhysicalId() + " no se pudo enviar correctamente");

				byteBuffer.clear();
			}
		}

	}

}
