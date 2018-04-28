package main.java.libterminal.patterns.observer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class EventListener<T extends Event> {

	private final BlockingQueue<T> eventQueue;

	public EventListener() {
		this.eventQueue = new LinkedBlockingQueue<>();
	}

	public final void receiveEvent(T event) {
		eventQueue.add(event);
	}

	public final T getEvent() throws InterruptedException {
		return eventQueue.take();
	}

}
