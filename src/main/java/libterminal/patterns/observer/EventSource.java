package main.java.libterminal.patterns.observer;

import java.util.LinkedList;
import java.util.List;

public class EventSource<T extends Event> {

	private final List<EventListener<T>> listeners;

	public EventSource() {
		this.listeners = new LinkedList<>();
	}

	public final void addListener(final EventListener<T> eventListener) {
		if (eventListener != null) {
			synchronized (listeners) {
				listeners.add(eventListener);
			}
		}
	}

	public final void removeListener(final EventListener<T> eventListener) {
		if (eventListener != null) {
			synchronized (listeners) {
				listeners.remove(eventListener);
			}
		}
	}

	public final void sendEvent(final T event) {
		synchronized (listeners) {
			for (final EventListener<T> eventListener : listeners) {
				eventListener.receiveEvent(event);
			}
		}
	}

}
