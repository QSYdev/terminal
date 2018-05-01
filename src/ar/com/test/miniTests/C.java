package ar.com.test.miniTests;

public class C {

	static Thread thread1;

	/*
	 * Conclusion, si usas thread.interrupt() y el thread esta bloqueado, arroja una
	 * excepcion que indica que fue interrumpido. Caso contarrior, se debe chequear
	 * manualmente si el thread fue interrumpido.
	 */
	public static void main(String[] args) throws InterruptedException {
		thread1 = new Thread(new Task());
		thread1.start();
		Thread.sleep(1000);
		thread1.interrupt();
	}

	private static class Task implements Runnable {

		@Override
		public void run() {
			while (!Thread.interrupted()) {
				for (long i = 0; i < 10000000000L; i++) {

				}
				System.out.println("Se ejecuto una vuelta");
			}
		}
	}

}
