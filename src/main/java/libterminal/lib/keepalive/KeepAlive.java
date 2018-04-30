package main.java.libterminal.lib.keepalive;

import java.util.TreeMap;

import main.java.libterminal.lib.protocol.QSYPacket;
import main.java.libterminal.patterns.observer.Event.InternalEvent;
import main.java.libterminal.patterns.observer.EventListener;
import main.java.libterminal.patterns.observer.EventSourceI;

/**
 * La clase se encarga de llevar un control respecto de la desconexion de los
 * nodos. No es Thread-Safe.
 */
public final class KeepAlive implements EventSourceI<InternalEvent>, AutoCloseable {

	private static final int ERROR_RATE = 50;
	private static final int MAX_ALLOWED_TIME = (int) ((1 + ERROR_RATE / 100f) * QSYPacket.KEEP_ALIVE_MS);
	private static final byte MAX_TRIES = 5;
	private static final int DEAD_NODES_PURGER_PERIOD = (int) (MAX_ALLOWED_TIME * 1.5f);

	private final EventSource<InternalEvent> eventSource;
	private final TreeMap<Integer, KeepAliveInfo> nodes;
	private final Thread deadNodesPurgerTask;

	private volatile boolean closed;

	public KeepAlive() {
		this.eventSource = new EventSource<>();
		this.nodes = new TreeMap<>();
		this.closed = false;
		this.deadNodesPurgerTask = new Thread(new DeadNodesPurgerTask(), "DeadsNodesPurger");
		this.deadNodesPurgerTask.start();
	}

	public void newNode(int physicalId) {
		long lastKeepAliveReceived = System.currentTimeMillis();
		synchronized (nodes) {
			if (!nodes.containsKey(physicalId)) {
				nodes.put(physicalId, new KeepAliveInfo(physicalId, lastKeepAliveReceived));
			}
		}
	}

	public void keepAlive(int physicalId) {
		long currentTime = System.currentTimeMillis();
		synchronized (nodes) {
			KeepAliveInfo info = nodes.get(physicalId);
			if (info != null)
				info.lastKeepAliveReceived = currentTime;
		}
	}

	public void touche(int physicalId) {
		keepAlive(physicalId);
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
			deadNodesPurgerTask.interrupt();
			try {
				deadNodesPurgerTask.join();
			} finally {
				nodes.clear();
				eventSource.close();
			}
		}
	}

	private static final class KeepAliveInfo {

		private final int physicalId;
		private volatile byte tries;
		private volatile long lastKeepAliveReceived;

		public KeepAliveInfo(int physicalId, long lastKeepAliveReceived) {
			this.physicalId = physicalId;
			this.tries = 0;
			this.lastKeepAliveReceived = lastKeepAliveReceived;
		}

	}

	private final class DeadNodesPurgerTask implements Runnable {

		private volatile boolean running = true;

		@Override
		public void run() {
			while (running) {
				try {
					Thread.sleep(DEAD_NODES_PURGER_PERIOD);
					long currentTime = System.currentTimeMillis();
					synchronized (nodes) {
						for (KeepAliveInfo info : nodes.values()) {
							// System.out.println(currentTime - info.lastKeepAliveReceived);
							if (currentTime - info.lastKeepAliveReceived > MAX_ALLOWED_TIME) {
								++info.tries;
								if (info.tries >= MAX_TRIES)
									eventSource.sendEvent(new InternalEvent.KeepAliveError(info.physicalId));
							} else {
								info.tries = 0;
							}
						}
					}
				} catch (InterruptedException e) {
					running = false;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

}
