package com.doomsday.game.common;

public class ApiException extends RuntimeException {
    private final String code;
    private final Object data;

    public ApiException(String code, String message) {
        this(code, message, null);
    }

    public ApiException(String code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    public String getCode() {
        return code;
    }

    public Object getData() {
        return data;
    }
}
