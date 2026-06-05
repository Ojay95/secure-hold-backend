package com.prymo.accountservice.domain.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkedAccount {
    private Long id;
    private String username;
    private String bankName;
    private String accountName;
    private String accountNumber;
    private String status;
}
