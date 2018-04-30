package main.java.libterminal.patterns.observer;

import java.util.LinkedList;
import java.util.List;

/**
 * La clase que permite enviar eventos a todos sus listenes.
 */
public interface EventSourceI<T extends Event> {

	void addListener(EventListener<T> eventListener);

	void removeListener(EventListener<T> eventListener);

	/**
	 * La clase concreta de EventSourceI. Es Thread-Safe.
	 */
	public static final class EventSource<T extends Event> implements AutoCloseable {

		private final List<Runnable> pendingActions;
		private final List<EventListener<T>> listeners;

		public EventSource() {
			this.pendingActions = new LinkedList<>();
			this.listeners = new LinkedList<>();
		}

		public synchronized final void addListener(EventListener<T> eventListener) {
			pendingActions.add(() -> {
				listeners.add(eventListener);
			});
		}

		public synchronized final void removeListener(EventListener<T> eventListener) {
			pendingActions.add(() -> {
				if (eventListener != null)
					listeners.remove(eventListener);
			});
		}

		public synchronized final void sendEvent(T event) {
			for (Runnable runnable : pendingActions)
				runnable.run();
			pendingActions.clear();
			for (EventListener<T> eventListener : listeners)
				eventListener.receiveEvent(event);
		}

		@Override
		public synchronized void close() {
			pendingActions.clear();
			listeners.clear();
		}
	}
}
