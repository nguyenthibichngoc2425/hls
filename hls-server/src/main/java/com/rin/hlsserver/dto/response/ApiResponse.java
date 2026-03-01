package com.rin.hlsserver.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    String message;
    T result;

    @Builder.Default
    int code = 200;


    public static <T> ApiResponse<T> success(T result){
        return ApiResponse.<T>builder()
                .code(200)
                .message("Success")
                .result(result)
                .build();
    }
}
