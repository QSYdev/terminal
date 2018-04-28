package main.java.libterminal.patterns.command;

/**
 * Las clases que encapsulen un solo metodo a ejecutar en otro momento, deben
 * extender de esta clase.
 */
public abstract class Command {

	public abstract void execute() throws Exception;
}