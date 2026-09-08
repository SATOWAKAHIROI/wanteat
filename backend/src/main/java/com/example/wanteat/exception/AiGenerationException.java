package com.example.wanteat.exception;

/** AI による生成に失敗したことを表す。502 として返す。 */
public class AiGenerationException extends RuntimeException {

    public AiGenerationException(String message) {
        super(message);
    }

    public AiGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
