package ar.com.terminal;

import ar.com.terminal.Event.InternalEvent;

abstract class Executor extends EventSourceI<InternalEvent> implements AutoCloseable {

	private final EventSource<InternalEvent> eventSource;

	public Executor() {
		this.eventSource = new EventSource<>();
	}

	public abstract void touche(int physicalId);

	public abstract boolean contains(int physicalId);

	@Override
	public void addListener(EventListener<InternalEvent> eventListener) {
		eventSource.addListener(eventListener);
	}

	@Override
	public void removeListener(EventListener<InternalEvent> eventListener) {
		eventSource.removeListener(eventListener);
	}

	@Override
	public abstract void close();
}
