package com.cybermanager.api.controllers.session;

import com.cybermanager.api.dtos.session.SessionDtos.CurrentSessionsResponse;
import com.cybermanager.api.dtos.session.SessionDtos.SessionResponse;
import com.cybermanager.api.dtos.session.SessionDtos.StartSessionRequest;
import com.cybermanager.api.dtos.session.SessionDtos.StopSessionRequest;
import com.cybermanager.application.commands.session.StartSessionCommand;
import com.cybermanager.application.commands.session.PauseSessionCommand;
import com.cybermanager.application.commands.session.ResumeSessionCommand;
import com.cybermanager.application.commands.session.StopSessionCommand;
import com.cybermanager.application.queries.session.SearchSessionsOfDayQuery;
import com.cybermanager.application.services.session.SessionApplicationService;
import com.cybermanager.application.views.session.SessionView;
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
    public ResponseEntity<SessionResponse> stop(@PathVariable("id") UUID id, @RequestBody(required = false) StopSessionRequest request) {
        return ResponseEntity.ok(toResponse(service.execute(new StopSessionCommand(id, request != null && request.paid()))));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<SessionResponse> pause(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(toResponse(service.execute(new PauseSessionCommand(id))));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<SessionResponse> resume(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(toResponse(service.execute(new ResumeSessionCommand(id))));
    }

    private SessionResponse toResponse(SessionView view) {
        return new SessionResponse(
                view.sessionId(),
                view.customerId(),
                view.customerName(),
                view.customerType(),
                view.remainingMinutes(),
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
