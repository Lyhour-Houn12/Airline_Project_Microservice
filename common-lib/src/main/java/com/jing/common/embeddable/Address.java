package com.jing.common.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Address {

    @Column(name = "street", length = 255)
    private String street;

    @Column(name = "postal_code", length = 255)
    private String postalCode;

}
