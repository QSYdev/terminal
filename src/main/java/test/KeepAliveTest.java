package main.java.test;

import java.util.Random;

import main.java.libterminal.lib.keepalive.KeepAlive;
import main.java.libterminal.lib.protocol.QSYPacket;
import main.java.libterminal.patterns.observer.Event.InternalException;
import main.java.libterminal.patterns.observer.Event.KeepAliveError;
import main.java.libterminal.patterns.observer.EventListener;
import main.java.libterminal.patterns.visitor.event.InternalEventVisitor;

public final class KeepAliveTest extends EventListener<InternalException> implements Runnable, InternalEventVisitor {

	private static final KeepAlive keepAlive = new KeepAlive();

	private boolean running = true;

	public KeepAliveTest() {
		keepAlive.addListener(this);
	}

	@Override
	public void run() {
		while (running) {
			try {
				final InternalException event = getEvent();
				event.accept(this);
			} catch (final InterruptedException e) {
				running = false;
			}
		}
	}

	@Override
	public void visit(final KeepAliveError event) {
		System.out.println("Se ha desconectado el nodo " + event.getPhysicalId());
		keepAlive.removeNode(event.getPhysicalId());
	}

	private static final class KeepAliveTask implements Runnable {

		private final int physicalId;
		private boolean running;
		private final Random random;

		public KeepAliveTask(final int physicalId) {
			this.physicalId = physicalId;
			this.running = true;
			this.random = new Random();
			keepAlive.newNode(physicalId);
		}

		@Override
		public void run() {
			while (running) {
				try {
					Thread.sleep(QSYPacket.KEEP_ALIVE_MS + random.nextInt(100) - 100);
					keepAlive.keepAlive(physicalId);
				} catch (final InterruptedException e) {
					running = false;
					keepAlive.removeNode(physicalId);
				}
			}
		}
	}

	public static void main(String[] args) throws InterruptedException {
		final Thread t = new Thread(new KeepAliveTest(), "Listener");
		t.start();

		final byte MAX_THREADS = 10;
		final Thread threads[] = new Thread[MAX_THREADS];

		for (byte i = 0; i < MAX_THREADS; i++) {
			threads[i] = new Thread(new KeepAliveTask(i), "Task " + i);
			threads[i].start();
		}

		final long sleepTime = 20000 + new Random().nextInt(5000);
		System.out.println("Tiempo de espera antes de terminar = " + sleepTime);
		Thread.sleep(sleepTime);

		for (byte i = 0; i < MAX_THREADS; i++) {
			threads[i].interrupt();
			threads[i].join();
		}

		t.interrupt();
		t.join();

		keepAlive.close();

	}

}
