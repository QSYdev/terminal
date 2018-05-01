package ar.com.terminal;

import ar.com.terminal.Event.ExternalEvent.ConnectedNode;
import ar.com.terminal.Event.ExternalEvent.DisconnectedNode;
import ar.com.terminal.Event.ExternalEvent.InternalException;
import ar.com.terminal.Event.ExternalEvent.Touche;

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
