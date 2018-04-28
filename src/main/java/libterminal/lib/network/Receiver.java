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

	private boolean running;

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

	public void newNode(int physicalId, SocketChannel socket) {
		if (running) {
			pendingTasks.add(new NewNodeTask(physicalId, socket));
			selector.wakeup();
		}
	}

	public void removeNode(int physicalId, SocketChannel socket) {
		if (running) {
			pendingTasks.add(new RemoveNodeTask(physicalId, socket));
			selector.wakeup();
		}
	}

	@Override
	public void close() throws IOException, InterruptedException {
		if (running) {
			running = false;
			try {
				selector.close();
			} finally {
				try {
					thread.join();
				} finally {
					pendingTasks.clear();
					buffers.clear();
				}
			}
		}
	}

	private final class ReceiverTask implements Runnable {

		private boolean running = true;

		@Override
		public void run() {
			while (running) {
				Runnable task;
				while ((task = pendingTasks.poll()) != null)
					task.run();

				try {
					selector.select();
					for (SelectionKey key : selector.selectedKeys()) {
						if (key.isReadable()) {
							SocketChannel channel = (SocketChannel) key.channel();
							int physicalId = (int) key.attachment();
							if (buffers.containsKey(physicalId)) {
								ByteBuffer byteBuffer = buffers.get(physicalId);

								try {
									channel.read(byteBuffer);
									if (byteBuffer.remaining() == 0) {
										byteBuffer.flip();
										byteBuffer.get(data);
										sendEvent(new InternalEvent.IncomingPacket(new QSYPacket(channel.socket().getInetAddress(), data)));
										byteBuffer.clear();
									}
								} catch (IOException e) {
									byteBuffer.clear();
									e.printStackTrace();
								}
							}
						}
					}
					selector.selectedKeys().clear();
				} catch (ClosedSelectorException e) {
					running = false;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

	private final class NewNodeTask implements Runnable {

		private final int physicalId;
		private final SocketChannel socket;

		public NewNodeTask(int physicalId, SocketChannel socket) {
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
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private final class RemoveNodeTask implements Runnable {

		private final int physicalId;
		private final SocketChannel socket;

		public RemoveNodeTask(int physicalId, SocketChannel socket) {
			this.physicalId = physicalId;
			this.socket = socket;
		}

		@Override
		public void run() {
			try {
				if (buffers.containsKey(physicalId)) {
					SelectionKey key = socket.keyFor(selector);
					if (key != null) {
						key.cancel();
					}
					buffers.remove(physicalId).clear();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

}