package ar.com.terminal;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Las clases que quieran recibir eventos asincronicamente deben extender de
 * esta clase. La misma provee funcionalidades para depositar eventos en una
 * cola bloqueante y ser atendidos desde el otro lado.
 */
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
