package com.cybermanager.api.controllers.session;

import com.cybermanager.api.dtos.session.SessionDtos.CurrentSessionsResponse;
import com.cybermanager.api.dtos.session.SessionDtos.PaySessionRequest;
import com.cybermanager.api.dtos.session.SessionDtos.RestartSessionsDayResponse;
import com.cybermanager.api.dtos.session.SessionDtos.SessionResponse;
import com.cybermanager.api.dtos.session.SessionDtos.StartSessionRequest;
import com.cybermanager.application.commands.session.PaySessionCommand;
import com.cybermanager.application.commands.session.PaySessionAndCreateInvoiceCommand;
import com.cybermanager.application.commands.session.RestartSessionsDayCommand;
import com.cybermanager.application.commands.session.StartSessionCommand;
import com.cybermanager.application.commands.session.PauseSessionCommand;
import com.cybermanager.application.commands.session.ResumeSessionCommand;
import com.cybermanager.application.commands.session.StopSessionCommand;
import com.cybermanager.application.queries.session.SearchSessionsOfDayQuery;
import com.cybermanager.application.services.session.SessionApplicationService;
import com.cybermanager.application.views.session.SessionView;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {
    private final SessionApplicationService service;

    public SessionController(SessionApplicationService service) {
        this.service = service;
    }

    @GetMapping("/day")
    public ResponseEntity<List<SessionResponse>> day(@RequestParam(name = "date", required = false) String date) {
        LocalDate parsed = date == null || date.isBlank() ? LocalDate.now() : LocalDate.parse(date);
        return ResponseEntity.ok(service.execute(new SearchSessionsOfDayQuery(parsed)).stream().map(this::toResponse).toList());
    }

    @GetMapping("/current")
    public ResponseEntity<CurrentSessionsResponse> current() {
        return ResponseEntity.ok(new CurrentSessionsResponse(service.execute().sessions().stream().map(this::toResponse).toList()));
    }

    @PostMapping("/start")
    public ResponseEntity<SessionResponse> start(@RequestBody StartSessionRequest request) {
        return ResponseEntity.ok(toResponse(service.execute(new StartSessionCommand(request.customerId(), request.customerName()))));
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<SessionResponse> stop(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(toResponse(service.execute(new StopSessionCommand(id))));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<SessionResponse> pay(@PathVariable("id") UUID id, @RequestBody(required = false) PaySessionRequest request) {
        return ResponseEntity.ok(toResponse(service.execute(new PaySessionCommand(
                id,
                request == null ? null : request.amountPaid(),
                request == null || request.subscriptionOfferIds() == null ? List.of() : request.subscriptionOfferIds(),
                request != null && request.createSubscriptionDebt()
        ))));
    }

    @PostMapping("/{id}/pay-with-invoice")
    public ResponseEntity<byte[]> payWithInvoice(@PathVariable("id") UUID id, @RequestBody(required = false) PaySessionRequest request) {
        var result = service.execute(new PaySessionAndCreateInvoiceCommand(
                id,
                request == null ? null : request.amountPaid(),
                request == null || request.subscriptionOfferIds() == null ? List.of() : request.subscriptionOfferIds(),
                request != null && request.createSubscriptionDebt()
        ));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.fileName() + "\"")
                .header("X-Invoice-Id", result.invoiceId().toString())
                .header("X-Invoice-Number", result.invoiceNumber())
                .contentType(MediaType.parseMediaType(result.mediaType()))
                .body(result.content());
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<SessionResponse> pause(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(toResponse(service.execute(new PauseSessionCommand(id))));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<SessionResponse> resume(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(toResponse(service.execute(new ResumeSessionCommand(id))));
    }

    @PostMapping("/restart-day")
    public ResponseEntity<RestartSessionsDayResponse> restartDay() {
        return ResponseEntity.ok(new RestartSessionsDayResponse(service.execute(new RestartSessionsDayCommand())));
    }

    private SessionResponse toResponse(SessionView view) {
        return new SessionResponse(
                view.sessionId(),
                view.customerId(),
                view.customerName(),
                view.customerType(),
                view.remainingMinutes(),
                view.displayRemainingMinutes(),
                view.workstation(),
                view.startedAt(),
                view.endedAt(),
                view.paused(),
                view.paid(),
                view.consumedSeconds(),
                view.consumedMinutes(),
                view.calculatedPrice(),
                view.purchasesAmount(),
                view.openDebtAmount(),
                view.totalAmountDue(),
                view.totalPaidAmount()
        );
    }
}
