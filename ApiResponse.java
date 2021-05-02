package com.lorevantonio.recipetamrin.request.response;

import android.util.Log;

import java.io.IOException;

import retrofit2.Response;

public class ApiResponse<T> {

    public ApiResponse<T> create(Throwable t) {
        return new ApiErrorResponse<>(t.toString());
    }

    public ApiResponse<T> create(Response<T> response) {
        if (response.isSuccessful()) {
            T body = response.body();
            if (response.code() == 204 || body == null)
                return new ApiEmptyResponse<>();
            return new ApiSuccessResponse<>(body);
        }
        try {
            return new ApiErrorResponse<>(response.errorBody().string());
        } catch (IOException e) {
            e.printStackTrace();
            return new ApiErrorResponse<>(response.message());
        }
    }

    public static class ApiSuccessResponse<T> extends ApiResponse<T> {

        private T body;

        public ApiSuccessResponse(T body) {
            this.body = body;
        }

        public T getBody() {
            return body;
        }
    }

    public static class ApiErrorResponse<T> extends ApiResponse<T> {
        private String msg;

        public ApiErrorResponse(String msg) {
            this.msg = msg;
        }

        public String getMsg() {
            return msg;
        }
    }

    public static class ApiEmptyResponse<T> extends ApiResponse<T> {

    }

}
