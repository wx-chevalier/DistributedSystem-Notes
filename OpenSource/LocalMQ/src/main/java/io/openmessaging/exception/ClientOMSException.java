package io.openmessaging.exception;

import io.openmessaging.exception.OMSRuntimeException;

public class ClientOMSException extends OMSRuntimeException {

    public String message;

    public ClientOMSException(String message) {
        this.message = message;
    }

    public ClientOMSException(String message, Throwable throwable) {
        this.initCause(throwable);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }


}
