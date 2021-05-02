package com.lorevantonio.recipetamrin.request.response;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Resource<T> {

    public enum Status {SUCCESS, LOADING, ERROR}

    public T data;

    public String msg;

    public Status status;

    public Resource(T data, String msg, Status status) {
        this.data = data;
        this.msg = msg;
        this.status = status;
    }

    public static <T> Resource<T> success(@NonNull T data) {
        return new Resource<>(data, null, Status.SUCCESS);
    }

    public static <T> Resource<T> loading(@Nullable T data) {
        return new Resource<>(data, null, Status.LOADING);
    }

    public static <T> Resource<T> error(@Nullable T data, @NonNull String msg) {
        return new Resource<>(data, msg, Status.ERROR);
    }

}
