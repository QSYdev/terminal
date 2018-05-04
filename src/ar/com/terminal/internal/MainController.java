package ar.com.terminal.internal;

import ar.com.terminal.internal.Event.InternalEvent;
import ar.com.terminal.internal.Event.InternalEvent.CloseSignal;
import ar.com.terminal.internal.Event.InternalEvent.IncomingPacket;
import ar.com.terminal.internal.Event.InternalEvent.InternalEventVisitor;
import ar.com.terminal.internal.Event.InternalEvent.KeepAliveError;

final class MainController extends EventListener<InternalEvent> implements AutoCloseable {

	private final Terminal terminal;
	private final Thread mainControllerTask;

	private volatile boolean closed;

	public MainController(Terminal terminal) {
		this.terminal = terminal;
		this.closed = false;
		this.mainControllerTask = new Thread(new MainControllerTask(), "MainController");
		this.mainControllerTask.start();
	}

	@Override
	public void close() {
		if (!closed) {
			closed = true;
			receiveEvent(new InternalEvent.CloseSignal());
			try {
				mainControllerTask.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private final class MainControllerTask implements Runnable, InternalEventVisitor {

		private volatile boolean running = true;

		@Override
		public void run() {
			while (running) {
				try {
					InternalEvent event = getEvent();
					event.accept(this);
				} catch (InterruptedException e) {
					running = false;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void visit(IncomingPacket event) throws Exception {
			terminal.visit(event);
		}

		@Override
		public void visit(KeepAliveError event) throws Exception {
			terminal.visit(event);
		}

		@Override
		public void visit(CloseSignal event) {
			terminal.visit(event);
			running = false;
		}
	}
}
