package com.shantanu.model;

import jakarta.persistence.Entity;
import lombok.Data;

@Data
public class ContactInformation {
    private String email;
    private String mobile;
    private String twitter;
    private String instagram;
}
