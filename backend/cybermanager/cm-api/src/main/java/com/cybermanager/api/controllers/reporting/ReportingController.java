package com.cybermanager.api.controllers.reporting;

import com.cybermanager.api.dtos.reporting.ReportingDtos.CustomerDayHistoryResponse;
import com.cybermanager.api.dtos.reporting.ReportingDtos.DayCustomerHistoryResponse;
import com.cybermanager.api.dtos.reporting.ReportingDtos.DayHistoryResponse;
import com.cybermanager.api.dtos.reporting.ReportingDtos.SaleActivityResponse;
import com.cybermanager.application.services.reporting.ReportingApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/history/days")
public class ReportingController {
    private final ReportingApplicationService service;

    public ReportingController(ReportingApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<DayHistoryResponse> day(
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam(name = "endDate", required = false) LocalDate endDate
    ) {
        var resolvedEndDate = endDate == null ? startDate : endDate;
        var view = service.dayHistory(startDate, resolvedEndDate);
        return ResponseEntity.ok(new DayHistoryResponse(
                view.startDate(),
                view.endDate(),
                view.customers().stream()
                        .map(item -> new DayCustomerHistoryResponse(
                                item.customerId(),
                                item.name(),
                                item.type(),
                                item.totalMinutes(),
                                item.salesTotal(),
                                item.debtTotal(),
                                item.collectedTotal(),
                                item.sessionState()
                        ))
                        .toList(),
                view.totalCollected(),
                view.totalDebtCreated()
        ));
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<CustomerDayHistoryResponse> customer(
            @PathVariable("id") UUID id,
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam(name = "endDate", required = false) LocalDate endDate
    ) {
        var resolvedEndDate = endDate == null ? startDate : endDate;
        var view = service.customerHistory(startDate, resolvedEndDate, id);
        return ResponseEntity.ok(new CustomerDayHistoryResponse(
                view.customerId(),
                view.name(),
                view.sales().stream().map(item -> new SaleActivityResponse(item.label(), item.quantity(), item.totalPrice())).toList(),
                view.sessions(),
                view.totalCollected(),
                view.totalDebtCreated()
        ));
    }
}

