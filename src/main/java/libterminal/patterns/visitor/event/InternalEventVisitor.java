package main.java.libterminal.patterns.visitor.event;

import main.java.libterminal.patterns.observer.Event.IncomingPacket;
import main.java.libterminal.patterns.observer.Event.KeepAliveError;

public interface InternalEventVisitor {

	public default void visit(final IncomingPacket event) {
	}

	public default void visit(final KeepAliveError event) {
	}

}
