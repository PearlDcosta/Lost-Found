package com.lostfound.exception;

/**
 * Thrown when the RPC-over-UDP server-time request fails: the server
 * is unreachable, the request timed out, or the response couldn't be
 * parsed. Callers (see ItemServiceImpl) are expected to catch this
 * and fall back gracefully rather than let a network hiccup on the
 * RPC service break the whole application.
 */
public class RPCException extends Exception {

    public RPCException(String message) {
        super(message);
    }

    public RPCException(String message, Throwable cause) {
        super(message, cause);
    }
}
