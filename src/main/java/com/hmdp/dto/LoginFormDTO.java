package com.hmdp.dto;

import io.netty.handler.codec.dns.DnsResponse;
import lombok.Builder;
import lombok.Data;
@Builder
@Data
public class LoginFormDTO {
    private String phone;
    private String code;
    private String password;


}
