package main.java.libterminal.patterns.observer;

import java.util.LinkedList;
import java.util.List;

/**
 * La clase que permite enviar eventos a todos sus listenes. No es Thread-safe.
 */
public interface EventSourceI<T extends Event> {

	void addListener(EventListener<T> eventListener);

	void removeListener(EventListener<T> eventListener);

	public static class EventSource<T extends Event> {

		private final List<Runnable> pendingActions;
		private final List<EventListener<T>> listeners;

		public EventSource() {
			this.pendingActions = new LinkedList<>();
			this.listeners = new LinkedList<>();
		}

		public final void addListener(EventListener<T> eventListener) {
			pendingActions.add(() -> {
				listeners.add(eventListener);
			});
		}

		public final void removeListener(EventListener<T> eventListener) {
			pendingActions.add(() -> {
				if (eventListener != null)
					listeners.remove(eventListener);
			});
		}

		public final void sendEvent(T event) {
			for (Runnable runnable : pendingActions)
				runnable.run();
			for (EventListener<T> eventListener : listeners)
				eventListener.receiveEvent(event);
		}
	}
}
