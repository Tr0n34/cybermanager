package com.cybermanager.application.services.sales;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class InvoiceNumberGenerator {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public String next() {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "FAC-" + LocalDateTime.now().format(FORMATTER) + "-" + suffix;
    }
}
