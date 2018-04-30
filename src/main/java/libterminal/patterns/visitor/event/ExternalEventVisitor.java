package main.java.libterminal.patterns.visitor.event;

import main.java.libterminal.patterns.observer.Event.ExternalEvent.ConnectedNode;
import main.java.libterminal.patterns.observer.Event.ExternalEvent.DisconnectedNode;
import main.java.libterminal.patterns.observer.Event.ExternalEvent.InternalException;
import main.java.libterminal.patterns.observer.Event.ExternalEvent.Touche;

public interface ExternalEventVisitor {

	public default void visit(ConnectedNode event) {
	}

	public default void visit(DisconnectedNode event) {
	}

	public default void visit(Touche event) {
	}

	public default void visit(InternalException event) {
	}

}
