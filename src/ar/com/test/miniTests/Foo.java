package ar.com.test.miniTests;

public class Foo {

	private final Thread thread;

	public Foo() {
		thread = new Thread(new Bar(), "F");
		thread.start();
	}

	public void run() {
		synchronized (this) {
			thread.interrupt();
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("Foo run method");
		}
	}

	private final class Bar implements Runnable {

		@Override
		public void run() {
			synchronized (Foo.this) {
				System.out.println("Bar run method");
			}
		}

	}

	/*
	 * Conclusion, el yield no libera el lock.
	 */
	public static void main(String[] args) throws InterruptedException {
		final Foo foo = new Foo();
		foo.run();
	}

}
