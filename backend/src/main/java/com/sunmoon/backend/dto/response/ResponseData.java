package com.sunmoon.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResponseData<T> implements Serializable {
    int status;
    String lang;
    String messageCode;
    T data;
    String error;
    String message;
    Date timestamp;
    String path;
}
