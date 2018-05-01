package ar.com.terminal;

/**
 * Las clases que encapsulen un solo metodo a ejecutar en otro momento, deben
 * extender de esta clase.
 */
abstract class Command {

	public abstract void execute() throws Exception;
}