package main.java.libterminal.patterns.visitor.event;

import main.java.libterminal.patterns.observer.Event.ConnectedNode;
import main.java.libterminal.patterns.observer.Event.DisconnectedNode;
import main.java.libterminal.patterns.observer.Event.Touche;

public interface ExternalEventVisitor {

	public default void visit(final ConnectedNode event) {
	}

	public default void visit(final DisconnectedNode event) {
	}

	public default void visit(final Touche event) {
	}

}
