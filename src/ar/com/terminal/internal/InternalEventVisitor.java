package ar.com.terminal.internal;

import ar.com.terminal.internal.InternalEvent.CloseSignal;
import ar.com.terminal.internal.InternalEvent.IncomingPacket;
import ar.com.terminal.internal.InternalEvent.InternalException;
import ar.com.terminal.internal.InternalEvent.KeepAliveError;

interface InternalEventVisitor {

	public default void visit(IncomingPacket event) throws Exception {
	}

	public default void visit(KeepAliveError event) throws Exception {
	}

	public default void visit(InternalException event) throws Exception {
	}

	public default void visit(CloseSignal event) throws Exception {
	}

}
