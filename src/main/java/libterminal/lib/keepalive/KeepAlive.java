package main.java.libterminal.lib.keepalive;

import java.util.TreeMap;

import main.java.libterminal.patterns.observer.Event.InternalEvent;
import main.java.libterminal.patterns.observer.EventSource;

public final class KeepAlive extends EventSource<InternalEvent> implements AutoCloseable {

	private static final int ERROR_RATE = 50;
	// private static final int MAX_ALLOWED_TIME = (int) ((1 + ERROR_RATE / 100f) *
	// QSYPacket.KEEP_ALIVE_MS);
	private static final int MAX_ALLOWED_TIME = 300;
	private static final byte MAX_TRIES = 1;

	private static final int DEAD_NODES_PURGER_PERIOD = (int) (MAX_ALLOWED_TIME * 1.5f);

	private final TreeMap<Integer, KeepAliveInfo> nodes;
	private volatile boolean running;
	private final Thread thread;

	public KeepAlive() {
		this.nodes = new TreeMap<>();
		this.running = true;
		this.thread = new Thread(new DeadNodesPurgerTask(), "Deads Nodes Purger");
		this.thread.start();
	}

	public void newNode(final int physicalId) {
		final long lastKeepAliveReceived = System.currentTimeMillis();
		synchronized (this) {
			if (running) {
				if (!nodes.containsKey(physicalId)) {
					nodes.put(physicalId, new KeepAliveInfo(physicalId, lastKeepAliveReceived));
				}
			}
		}
	}

	public void keepAlive(final int physicalId) {
		final long currentTime = System.currentTimeMillis();
		updateKeepAlive(physicalId, currentTime);
	}

	private void updateKeepAlive(final int physicalId, final long currentTime) {
		synchronized (this) {
			if (running) {
				final KeepAliveInfo info = nodes.get(physicalId);
				if (info != null)
					info.lastKeepAliveReceived = currentTime;
			}
		}
	}

	public void touche(final int physicalId) {
		keepAlive(physicalId);
	}

	public void removeNode(final int physicalId) {
		synchronized (this) {
			if (running)
				nodes.remove(physicalId);
		}
	}

	@Override
	public void close() {
		synchronized (this) {
			if (running) {
				running = false;
				thread.interrupt();
			}
		}
		// Despues de este punto, ningun metodo va a producir efectos en las variables
		// internas de la clase.
		try {
			thread.join();
		} catch (final InterruptedException e) {
			// Estamos en graves problemas si pasa, porque es en el thread principal.
			e.printStackTrace();
		}
		nodes.clear();
	}

	private final static class KeepAliveInfo {

		private int physicalId;
		private byte tries;
		private long lastKeepAliveReceived;

		public KeepAliveInfo(final int physicalId, final long lastKeepAliveReceived) {
			this.physicalId = physicalId;
			this.tries = 0;
			this.lastKeepAliveReceived = lastKeepAliveReceived;
		}

	}

	private final class DeadNodesPurgerTask implements Runnable {

		private boolean running;

		public DeadNodesPurgerTask() {
			this.running = true;
		}

		@Override
		public void run() {
			while (running) {
				try {
					Thread.sleep(DEAD_NODES_PURGER_PERIOD);
					final long currentTime = System.currentTimeMillis();
					synchronized (KeepAlive.this) {
						for (final KeepAliveInfo info : nodes.values()) {
							checkKeepAlive(info, currentTime);
						}
					}
				} catch (final InterruptedException e) {
					running = false;
				}
			}
		}
	}

	private void checkKeepAlive(final KeepAliveInfo info, final long currentTime) {
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
