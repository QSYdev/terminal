package main.java.libterminal.lib.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;

import main.java.libterminal.lib.protocol.QSYPacket;
import main.java.libterminal.patterns.observer.Event.InternalEvent;
import main.java.libterminal.patterns.observer.EventSource;

public final class Receiver extends EventSource<InternalEvent> implements AutoCloseable {

	private final Selector selector;
	private final LinkedBlockingQueue<Runnable> pendingTasks;
	private final TreeMap<Integer, ByteBuffer> buffers;
	private final byte[] data;

	private volatile boolean running;

	private final Thread thread;

	public Receiver() throws IOException {
		this.selector = Selector.open();
		this.pendingTasks = new LinkedBlockingQueue<>();
		this.buffers = new TreeMap<>();
		this.data = new byte[QSYPacket.PACKET_SIZE];

		this.running = true;

		this.thread = new Thread(new ReceiverTask(), "Receiver");
		this.thread.start();
	}

	public void newNode(final int physicalId, final SocketChannel socket) {
		synchronized (this) {
			if (running) {
				pendingTasks.add(new NewNodeTask(physicalId, socket));
			}
		}
		selector.wakeup();
	}

	public void removeNode(final int physicalId, final SocketChannel socket) {
		synchronized (this) {
			if (running) {
				pendingTasks.add(new RemoveNodeTask(physicalId, socket));
			}
		}
		selector.wakeup();
	}

	@Override
	public void close() {
		synchronized (this) {
			if (running) {
				running = false;
				try {
					selector.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
			try {
				thread.join();
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
			pendingTasks.clear();
			buffers.clear();
		}

	}

	private final class ReceiverTask implements Runnable {

		private boolean running;

		public ReceiverTask() {
			this.running = true;
		}

		@Override
		public void run() {
			while (running) {
				Runnable task;
				while ((task = pendingTasks.poll()) != null) {
					task.run();
				}

				try {
					selector.select();
					for (final SelectionKey key : selector.selectedKeys()) {
						if (key.isReadable()) {
							final SocketChannel channel = (SocketChannel) key.channel();
							final int physicalId = (int) key.attachment();
							if (buffers.containsKey(physicalId)) {
								final ByteBuffer byteBuffer = buffers.get(physicalId);

								try {
									channel.read(byteBuffer);
									if (byteBuffer.remaining() == 0) {
										byteBuffer.flip();
										byteBuffer.get(data);
										sendEvent(new InternalEvent.IncomingPacket(new QSYPacket(channel.socket().getInetAddress(), data)));
										byteBuffer.clear();
									}
								} catch (final IOException e) {
									// Hubo un problema con el read.
									byteBuffer.clear();
									e.printStackTrace();
								} catch (final Exception e) {
									e.printStackTrace();
								}

							}
						}
					}
					selector.selectedKeys().clear();
				} catch (final ClosedSelectorException e) {
					running = false;
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

	private final class NewNodeTask implements Runnable {

		private final int physicalId;
		private final SocketChannel socket;

		public NewNodeTask(final int physicalId, final SocketChannel socket) {
			this.physicalId = physicalId;
			this.socket = socket;
		}

		@Override
		public void run() {
			try {
				if (!buffers.containsKey(physicalId)) {
					if (socket.register(selector, SelectionKey.OP_READ, physicalId) != null)
						buffers.put(physicalId, ByteBuffer.allocate(QSYPacket.PACKET_SIZE));
				}
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}

	}

	private final class RemoveNodeTask implements Runnable {

		private final int physicalId;
		private final SocketChannel socket;

		public RemoveNodeTask(final int physicalId, final SocketChannel socket) {
			this.physicalId = physicalId;
			this.socket = socket;
		}

		@Override
		public void run() {
			if (buffers.containsKey(physicalId)) {
				final SelectionKey key = socket.keyFor(selector);
				if (key != null) {
					key.cancel();
				}
				buffers.remove(physicalId).clear();
			}
		}

	}

}