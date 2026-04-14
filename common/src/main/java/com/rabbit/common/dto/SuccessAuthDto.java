package com.rabbit.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuccessAuthDto {
    private String token;
    private UserDto user;

    // public SuccessAuthDto(String token, UserDto user) {
    //     this.token = token;
    //     this.user = user;
    // }
}
