package main.java.libterminal.lib.keepalive;

import java.util.TreeMap;

import main.java.libterminal.lib.protocol.QSYPacket;
import main.java.libterminal.patterns.observer.Event.InternalEvent;
import main.java.libterminal.patterns.observer.EventSource;

/**
 * La clase se encarga de llevar un control respecto de la desconexion de los
 * nodos. No es Thread-Safe.
 */
public final class KeepAlive extends EventSource<InternalEvent> implements AutoCloseable {

	private static final int ERROR_RATE = 50;
	private static final int MAX_ALLOWED_TIME = (int) ((1 + ERROR_RATE / 100f) * QSYPacket.KEEP_ALIVE_MS);
	private static final byte MAX_TRIES = 1;

	private static final int DEAD_NODES_PURGER_PERIOD = (int) (MAX_ALLOWED_TIME * 1.5f);
	private final TreeMap<Integer, KeepAliveInfo> nodes;
	private final Thread deadNodesPurgerTask;

	private boolean running;

	public KeepAlive() {
		this.nodes = new TreeMap<>();
		this.running = true;
		this.deadNodesPurgerTask = new Thread(new DeadNodesPurgerTask(), "Deads Nodes Purger");
		this.deadNodesPurgerTask.start();
	}

	public void newNode(int physicalId) {
		long lastKeepAliveReceived = System.currentTimeMillis();
		if (running) {
			synchronized (nodes) {
				if (!nodes.containsKey(physicalId)) {
					nodes.put(physicalId, new KeepAliveInfo(physicalId, lastKeepAliveReceived));
				}
			}
		}
	}

	public void keepAlive(int physicalId) {
		long currentTime = System.currentTimeMillis();
		updateKeepAlive(physicalId, currentTime);
	}

	private void updateKeepAlive(int physicalId, long currentTime) {
		if (running) {
			synchronized (nodes) {
				KeepAliveInfo info = nodes.get(physicalId);
				if (info != null)
					info.lastKeepAliveReceived = currentTime;
			}
		}
	}

	public void touche(int physicalId) {
		keepAlive(physicalId);
	}

	public void removeNode(int physicalId) {
		if (running) {
			synchronized (nodes) {
				nodes.remove(physicalId);
			}
		}
	}

	@Override
	public void close() throws InterruptedException {
		if (running) {
			running = false;
			deadNodesPurgerTask.interrupt();
			try {
				deadNodesPurgerTask.join();
			} finally {
				nodes.clear();
			}
		}
	}

	private static final class KeepAliveInfo {

		private int physicalId;
		private byte tries;
		private long lastKeepAliveReceived;

		public KeepAliveInfo(int physicalId, long lastKeepAliveReceived) {
			this.physicalId = physicalId;
			this.tries = 0;
			this.lastKeepAliveReceived = lastKeepAliveReceived;
		}

	}

	private final class DeadNodesPurgerTask implements Runnable {

		private boolean running = true;

		@Override
		public void run() {
			while (running) {
				try {
					Thread.sleep(DEAD_NODES_PURGER_PERIOD);
					long currentTime = System.currentTimeMillis();
					synchronized (nodes) {
						for (KeepAliveInfo info : nodes.values())
							checkKeepAlive(info, currentTime);
					}
				} catch (InterruptedException e) {
					running = false;
				}
			}
		}
	}

	private void checkKeepAlive(KeepAliveInfo info, long currentTime) {
		// System.out.println(currentTime - info.lastKeepAliveReceived);
		if (currentTime - info.lastKeepAliveReceived > MAX_ALLOWED_TIME) {
			++info.tries;
			if (info.tries >= MAX_TRIES)
				sendEvent(new InternalEvent.KeepAliveError(info.physicalId));
		} else {
			info.tries = 0;
		}
	}

}
