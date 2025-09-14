package com.example.client.dto;

public class ApiResponse<T> {
    private String message;
    private int status;
    private T payload;

    // Constructors
    public ApiResponse() {}

    public ApiResponse(String message, int status, T payload) {
        this.message = message;
        this.status = status;
        this.payload = payload;
    }

    // Static factory methods
    public static <T> ApiResponse<T> success(T payload) {
        return new ApiResponse<>("Success", 200, payload);
    }

    public static <T> ApiResponse<T> success(String message, T payload) {
        return new ApiResponse<>(message, 200, payload);
    }

    public static <T> ApiResponse<T> error(String message, int status) {
        return new ApiResponse<>(message, status, null);
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "ApiResponse{" +
                "message='" + message + '\'' +
                ", status=" + status +
                ", payload=" + payload +
                '}';
    }
}