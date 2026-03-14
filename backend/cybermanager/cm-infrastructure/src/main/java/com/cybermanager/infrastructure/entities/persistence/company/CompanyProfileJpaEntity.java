package com.cybermanager.infrastructure.entities.persistence.company;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cm_company_profile")
public class CompanyProfileJpaEntity {
    @Id
    public UUID id;
    public String legalName;
    public String siret;
    public String phone;
    public String email;
    public String addressLine1;
    public String addressLine2;
    public String postalCode;
    public String city;
    public String country;
    public LocalDateTime updatedAt;
}
