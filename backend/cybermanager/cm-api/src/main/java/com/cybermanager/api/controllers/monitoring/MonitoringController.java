package com.cybermanager.api.controllers.monitoring;

import com.cybermanager.api.dtos.monitoring.MonitoringDtos.DayCustomerActivityResponse;
import com.cybermanager.api.dtos.monitoring.MonitoringDtos.DayCustomerResponse;
import com.cybermanager.api.dtos.monitoring.MonitoringDtos.SaleActivityResponse;
import com.cybermanager.api.dtos.monitoring.MonitoringDtos.SessionActivityResponse;
import com.cybermanager.application.services.monitoring.MonitoringApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/day-monitoring/customers")
public class MonitoringController {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final MonitoringApplicationService service;

    public MonitoringController(MonitoringApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<DayCustomerResponse>> customers() {
        return ResponseEntity.ok(service.customers().stream()
                .map(view -> new DayCustomerResponse(
                        view.customerId(),
                        view.name(),
                        view.type(),
                        view.remainingMinutes(),
                        view.consumedMinutes(),
                        view.purchasesTotal(),
                        view.debtTotal(),
                        view.collectedTotal(),
                        view.sessionState()
                ))
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DayCustomerActivityResponse> customer(@PathVariable("id") UUID id) {
        var view = service.customerActivity(id);
        return ResponseEntity.ok(new DayCustomerActivityResponse(
                view.customerId(),
                view.name(),
                view.sales().stream()
                        .map(item -> new SaleActivityResponse(
                                item.label(),
                                item.quantity(),
                                item.totalPrice(),
                                item.debt(),
                                format(item.soldAt())
                        ))
                        .toList(),
                view.sessions().stream()
                        .map(item -> new SessionActivityResponse(
                                item.sessionLabel(),
                                format(item.startedAt()),
                                format(item.endedAt())
                        ))
                        .toList(),
                view.totalCollected(),
                view.totalDebtCreated()
        ));
    }

    private String format(java.time.LocalDateTime value) {
        return value == null ? "-" : value.format(DATE_TIME_FORMATTER);
    }
}

