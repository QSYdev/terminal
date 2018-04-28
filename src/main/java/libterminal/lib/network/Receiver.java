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
import main.java.libterminal.patterns.command.Command;
import main.java.libterminal.patterns.command.TerminalRunnable;
import main.java.libterminal.patterns.observer.Event.InternalEvent;
import main.java.libterminal.patterns.observer.EventSourceI.EventSource;

/**
 * Maneja los paquetes que se reciben por algun socket. No es Thread-Safe.
 */
public final class Receiver extends EventSource<InternalEvent> implements AutoCloseable {

	private final Selector selector;
	private final LinkedBlockingQueue<Command> pendingTasks;
	private final TreeMap<Integer, ByteBuffer> buffers;
	private final byte[] data;

	private final Thread receiverTask;

	private volatile boolean closed;

	public Receiver() throws IOException {
		this.selector = Selector.open();
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
				}
			}
		}
	}

	private final class ReceiverTask extends TerminalRunnable {

		private boolean running = true;

		@Override
		protected void runTerminalTask() throws Exception {
			while (running) {
				Command task;
				while ((task = pendingTasks.poll()) != null)
					task.execute();

				try {
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
									sendEvent(new InternalEvent.IncomingPacket(new QSYPacket(channel.socket().getInetAddress(), data)));
									byteBuffer.clear();
								}
							}
						}
					}
					selector.selectedKeys().clear();
				} catch (ClosedSelectorException e) {
					running = false;
				}
			}
		}

		@Override
		protected void handleError(Exception e) {
			sendEvent(new InternalEvent.InternalException(e));
		}

	}

	private final class NewNodeTask extends Command {

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

	private final class RemoveNodeTask extends Command {

		private final int physicalId;
		private final SocketChannel socket;

		public RemoveNodeTask(int physicalId, SocketChannel socket) {
			this.physicalId = physicalId;
			this.socket = socket;
		}

		@Override
		public void execute() {
			if (buffers.containsKey(physicalId)) {
				SelectionKey key = socket.keyFor(selector);
				if (key != null)
					key.cancel();
				buffers.remove(physicalId).clear();
			}
		}

	}

}