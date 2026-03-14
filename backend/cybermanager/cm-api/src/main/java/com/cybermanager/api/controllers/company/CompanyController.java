package com.cybermanager.api.controllers.company;

import com.cybermanager.api.shared.ApiSupport;
import com.cybermanager.application.commands.company.UpsertCompanyProfileCommand;
import com.cybermanager.application.queries.company.GetCompanyProfileQuery;
import com.cybermanager.application.usecases.company.GetCompanyProfileUseCase;
import com.cybermanager.application.usecases.company.UpsertCompanyProfileUseCase;
import com.cybermanager.application.views.company.CompanyProfileView;
import com.cybermanager.infrastructure.security.users.JwtAccessTokenReader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.cybermanager.api.dtos.company.CompanyDtos.CompanyProfileRequest;
import static com.cybermanager.api.dtos.company.CompanyDtos.CompanyProfileResponse;

@RestController
@RequestMapping("/api/company")
public class CompanyController {
    private final GetCompanyProfileUseCase getCompanyProfileUseCase;
    private final UpsertCompanyProfileUseCase upsertCompanyProfileUseCase;
    private final JwtAccessTokenReader tokenReader;

    public CompanyController(
            GetCompanyProfileUseCase getCompanyProfileUseCase,
            UpsertCompanyProfileUseCase upsertCompanyProfileUseCase,
            JwtAccessTokenReader tokenReader
    ) {
        this.getCompanyProfileUseCase = getCompanyProfileUseCase;
        this.upsertCompanyProfileUseCase = upsertCompanyProfileUseCase;
        this.tokenReader = tokenReader;
    }

    @GetMapping
    public ResponseEntity<CompanyProfileResponse> current() {
        CompanyProfileView view = getCompanyProfileUseCase.execute(new GetCompanyProfileQuery());
        return ResponseEntity.ok(view == null ? null : toResponse(view));
    }

    @PutMapping
    public ResponseEntity<CompanyProfileResponse> upsert(@RequestHeader("Authorization") String authorization, @RequestBody CompanyProfileRequest request) {
        var actor = ApiSupport.actor(authorization, tokenReader);
        var view = upsertCompanyProfileUseCase.execute(new UpsertCompanyProfileCommand(
                actor.email(),
                request.legalName(),
                request.siret(),
                request.phone(),
                request.email(),
                request.addressLine1(),
                request.addressLine2(),
                request.postalCode(),
                request.city(),
                request.country()
        ));
        return ResponseEntity.ok(toResponse(view));
    }

    private CompanyProfileResponse toResponse(CompanyProfileView view) {
        return new CompanyProfileResponse(
                view.id(),
                view.legalName(),
                view.siret(),
                view.phone(),
                view.email(),
                view.addressLine1(),
                view.addressLine2(),
                view.postalCode(),
                view.city(),
                view.country(),
                view.updatedAt()
        );
    }
}
