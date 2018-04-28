package main.java.libterminal.patterns.observer;

import java.util.LinkedList;
import java.util.List;

/**
 * La clase que permite enviar eventos a todos sus listenes. No es Thread-safe.
 */
public class EventSource<T extends Event> {

	private final List<EventListener<T>> listeners;

	public EventSource() {
		this.listeners = new LinkedList<>();
	}

	public final void addListener(final EventListener<T> eventListener) {
		listeners.add(eventListener);
	}

	public final void removeListener(final EventListener<T> eventListener) {
		listeners.remove(eventListener);
	}

	public final void sendEvent(final T event) {
		for (final EventListener<T> eventListener : listeners) {
			eventListener.receiveEvent(event);
		}
	}

}
