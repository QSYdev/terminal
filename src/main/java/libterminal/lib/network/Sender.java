package main.java.libterminal.lib.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.SocketChannel;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;

import main.java.libterminal.lib.protocol.QSYPacket;
import main.java.libterminal.lib.protocol.QSYPacket.PacketType;

public final class Sender implements AutoCloseable {

	private final LinkedBlockingQueue<Runnable> pendingTasks;

	private final TreeMap<Integer, SocketChannel> nodes;
	private final ByteBuffer byteBuffer;

	private final Thread thread;

	private volatile boolean running;

	public Sender() {
		this.pendingTasks = new LinkedBlockingQueue<>();

		this.running = true;

		this.nodes = new TreeMap<>();
		this.byteBuffer = ByteBuffer.allocate(QSYPacket.PACKET_SIZE);

		this.thread = new Thread(new SenderTask(), "Sender");
		this.thread.start();
	}

	public void newNode(final int physicalId, final SocketChannel socket) {
		synchronized (this) {
			if (running) {
				if (!nodes.containsKey(physicalId) || !nodes.get(physicalId).equals(socket))
					nodes.put(physicalId, socket);
			}
		}
	}

	public void command(final QSYPacket packet) {
		synchronized (this) {
			if (running) {
				if (packet.getType() == PacketType.Command) {
					pendingTasks.add(new CommandTask(packet));
				}
			}
		}
	}

	public void removeNode(final int physicalId) {
		synchronized (this) {
			if (running) {
				nodes.remove(physicalId);
			}
		}
	}

	@Override
	public void close() {
		synchronized (this) {
			if (running) {
				running = false;
				thread.interrupt();
			}
			try {
				thread.join();
			} catch (final InterruptedException e) {
				// Si se mete aca, estamos en graves problemas, porque el thread principal fue
				// interrumpido.
				e.printStackTrace();
			}
			pendingTasks.clear();
			nodes.clear();
			byteBuffer.clear();
		}
	}

	private final class SenderTask implements Runnable {

		private boolean running;

		public SenderTask() {
			this.running = true;
		}

		@Override
		public void run() {
			while (running) {
				try {
					final Runnable task = pendingTasks.take();
					task.run();
				} catch (final InterruptedException e) {
					running = false;
				}
			}
		}

	}

	private final class CommandTask implements Runnable {

		private final QSYPacket packet;

		public CommandTask(final QSYPacket packet) {
			this.packet = packet;
		}

		@Override
		public void run() {
			final SocketChannel channel;

			synchronized (Sender.this) {
				channel = nodes.get(packet.getPhysicalId());
			}

			if (channel != null) {
				byteBuffer.put(packet.getRawData());
				byteBuffer.flip();
				try {
					byte bytesTransmitted = 0;
					short tries = 0;
					while (bytesTransmitted < QSYPacket.PACKET_SIZE && tries++ < 256)
						bytesTransmitted += channel.write(byteBuffer);

					if (bytesTransmitted != QSYPacket.PACKET_SIZE)
						throw new IOException("<< SENDER >> El paquete hacia el nodo " + packet.getPhysicalId() + " no se pudo enviar correctamente");

				} catch (final ClosedByInterruptException e) {
					thread.interrupt();
				} catch (final Exception e) {
					// No se pudo enviar el paquete.
					e.printStackTrace();
				}
				byteBuffer.clear();
			}
		}

	}

}
