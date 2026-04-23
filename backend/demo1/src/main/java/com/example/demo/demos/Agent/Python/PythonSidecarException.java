package com.example.demo.demos.Agent.Python;

public class PythonSidecarException extends RuntimeException {

    public PythonSidecarException(String message) {
        super(message);
    }

    public PythonSidecarException(String message, Throwable cause) {
        super(message, cause);
    }
}
