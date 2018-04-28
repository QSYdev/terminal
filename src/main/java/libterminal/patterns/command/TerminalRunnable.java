package main.java.libterminal.patterns.command;

/**
 * Los threads que este proyecto utilice, deben extender de esta clase.
 */
public abstract class TerminalRunnable implements Runnable {

	protected abstract void runTerminalTask() throws Exception;

	protected abstract void handleError(Exception e);

	@Override
	public final void run() {
		try {
			runTerminalTask();
		} catch (Exception e) {
			handleError(e);
		}
	}

}
