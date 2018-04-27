package main.java.test.miniTests;

import java.util.concurrent.LinkedBlockingQueue;

public class E extends A implements Runnable {

	private final LinkedBlockingQueue<Integer> list = new LinkedBlockingQueue<>();

	public void run() {
		synchronized (this) {
			try {
				list.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("unused")
	private final class F {

		public void bloquearse() {

		}
	}

	/*
	 * Conclusion, this hay uno solo que se comparte entre todas las subclases. Y si
	 * tenes una clase interna, ese this no es el mismo de la clase que la
	 * ccontiene.
	 */
	public static void main(String[] args) throws InterruptedException {
		final E e = new E();
		final Thread t = new Thread(e);
		t.start();
		Thread.sleep(1000);
		e.imprimir();
	}

}
