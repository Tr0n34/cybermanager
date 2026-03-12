package com.cybermanager.api.controllers.reporting;

import com.cybermanager.api.dtos.reporting.ReportingDtos.CustomerDayHistoryResponse;
import com.cybermanager.api.dtos.reporting.ReportingDtos.DayCustomerHistoryResponse;
import com.cybermanager.api.dtos.reporting.ReportingDtos.DayHistoryResponse;
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

    @GetMapping("/{date}")
    public ResponseEntity<DayHistoryResponse> day(@PathVariable("date") LocalDate date) {
        var view = service.dayHistory(date);
        return ResponseEntity.ok(new DayHistoryResponse(view.date(), view.customers().stream().map(item -> new DayCustomerHistoryResponse(item.customerId(), item.name(), item.type(), item.totalMinutes(), item.salesTotal())).toList()));
    }

    @GetMapping("/{date}/customers")
    public ResponseEntity<DayHistoryResponse> dayCustomers(@PathVariable("date") LocalDate date) {
        var view = service.dayHistory(date);
        return ResponseEntity.ok(new DayHistoryResponse(view.date(), view.customers().stream().map(item -> new DayCustomerHistoryResponse(item.customerId(), item.name(), item.type(), item.totalMinutes(), item.salesTotal())).toList()));
    }

    @GetMapping("/{date}/customers/{id}")
    public ResponseEntity<CustomerDayHistoryResponse> customer(@PathVariable("date") LocalDate date, @PathVariable("id") UUID id) {
        var view = service.customerHistory(date, id);
        return ResponseEntity.ok(new CustomerDayHistoryResponse(view.customerId(), view.name(), view.sales(), view.sessions()));
    }
}

