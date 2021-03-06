package terminal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;

import terminal.Event.InternalEvent;
import terminal.QSYPacket.PacketType;

final class Receiver extends EventSourceI<InternalEvent> implements AutoCloseable {

	private final EventSource<InternalEvent> eventSource;
	private final Selector selector;
	private final LinkedBlockingQueue<ReceiverCommand> pendingTasks;
	private final TreeMap<Integer, ByteBuffer> buffers;
	private final byte[] data;

	private final Thread receiverTask;

	private volatile boolean closed;

	public Receiver() throws IOException {
		this.selector = Selector.open();
		this.eventSource = new EventSource<>();
		this.pendingTasks = new LinkedBlockingQueue<>();
		this.buffers = new TreeMap<>();
		this.data = new byte[QSYPacket.PACKET_SIZE];

		this.closed = false;

		this.receiverTask = new Thread(new ReceiverTask(), "Receiver");
		this.receiverTask.start();
	}

	public void newNode(int physicalId, SocketChannel socket) {
		pendingTasks.add(new NewNodeTask(physicalId, socket));
		selector.wakeup();
	}

	public void removeNode(int physicalId, SocketChannel socket) {
		pendingTasks.add(new RemoveNodeTask(physicalId, socket));
		selector.wakeup();
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
	public void close() throws IOException, InterruptedException {
		if (!closed) {
			closed = true;
			try {
				selector.close();
			} finally {
				try {
					receiverTask.join();
				} finally {
					pendingTasks.clear();
					buffers.clear();
					eventSource.close();
				}
			}
		}
	}

	private final class ReceiverTask implements Runnable {

		private volatile boolean running = true;

		@Override
		public void run() {
			while (running) {
				try {
					ReceiverCommand task;
					while ((task = pendingTasks.poll()) != null)
						task.execute();

					selector.select();
					for (SelectionKey key : selector.selectedKeys()) {
						if (key.isReadable()) {
							SocketChannel channel = (SocketChannel) key.channel();
							int physicalId = (int) key.attachment();
							if (buffers.containsKey(physicalId)) {
								ByteBuffer byteBuffer = buffers.get(physicalId);

								channel.read(byteBuffer);
								if (byteBuffer.remaining() == 0) {
									byteBuffer.flip();
									byteBuffer.get(data);
									byteBuffer.clear();
									QSYPacket packet = new QSYPacket(channel.socket().getInetAddress(), data);
									if (packet.getType() == PacketType.Keepalive || packet.getType() == PacketType.Touche)
										eventSource.sendEvent(new InternalEvent.IncomingPacket(new QSYPacket(channel.socket().getInetAddress(), data)));
								}
							}
						}
					}
					selector.selectedKeys().clear();
				} catch (ClosedSelectorException e) {
					running = false;
				} catch (IllegalArgumentException e) {
					// No se pudo construir el paquete recibido.
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

	private static abstract class ReceiverCommand {

		public abstract void execute() throws Exception;

	}

	private final class NewNodeTask extends ReceiverCommand {

		private final int physicalId;
		private final SocketChannel socket;

		public NewNodeTask(int physicalId, SocketChannel socket) {
			this.physicalId = physicalId;
			this.socket = socket;
		}

		@Override
		public void execute() throws Exception {
			if (!buffers.containsKey(physicalId)) {
				if (socket.register(selector, SelectionKey.OP_READ, physicalId) != null)
					buffers.put(physicalId, ByteBuffer.allocate(QSYPacket.PACKET_SIZE));
			}
		}

	}

	private final class RemoveNodeTask extends ReceiverCommand {

		private final int physicalId;
		private final SocketChannel socket;

		public RemoveNodeTask(int physicalId, SocketChannel socket) {
			this.physicalId = physicalId;
			this.socket = socket;
		}

		@Override
		public void execute() throws Exception {
			if (buffers.containsKey(physicalId)) {
				SelectionKey key = socket.keyFor(selector);
				if (key != null)
					key.cancel();
				buffers.remove(physicalId).clear();
			}
		}

	}

}