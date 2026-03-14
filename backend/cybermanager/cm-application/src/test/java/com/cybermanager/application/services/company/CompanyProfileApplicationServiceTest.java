package com.cybermanager.application.services.company;

import com.cybermanager.application.commands.company.UpsertCompanyProfileCommand;
import com.cybermanager.application.queries.company.GetCompanyProfileQuery;
import com.cybermanager.domain.model.company.CompanyProfile;
import com.cybermanager.domain.port.company.CompanyProfileRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompanyProfileApplicationServiceTest {
    @Test
    void shouldReturnNullWhenCompanyDoesNotExist() {
        CompanyProfileRepository repository = mock(CompanyProfileRepository.class);
        CompanyProfileApplicationService service = new CompanyProfileApplicationService(repository);

        when(repository.findCurrent()).thenReturn(Optional.empty());

        assertNull(service.execute(new GetCompanyProfileQuery()));
    }

    @Test
    void shouldCreateCompanyProfile() {
        CompanyProfileRepository repository = mock(CompanyProfileRepository.class);
        CompanyProfileApplicationService service = new CompanyProfileApplicationService(repository);

        when(repository.findCurrent()).thenReturn(Optional.empty());
        when(repository.save(any(CompanyProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.execute(new UpsertCompanyProfileCommand(
                "admin@cybermanager.local",
                "CyberManager SARL",
                "12345678901234",
                "0102030405",
                "contact@cybermanager.local",
                "1 rue du Net",
                null,
                "75001",
                "Paris",
                "France"
        ));

        assertEquals("CyberManager SARL", result.legalName());
        assertEquals("12345678901234", result.siret());
    }

    @Test
    void shouldUpdateCompanyProfile() {
        CompanyProfileRepository repository = mock(CompanyProfileRepository.class);
        CompanyProfileApplicationService service = new CompanyProfileApplicationService(repository);
        CompanyProfile current = CompanyProfile.create("Old", null, null, null, null, null, null, null, null, LocalDateTime.now().minusDays(1));

        when(repository.findCurrent()).thenReturn(Optional.of(current));
        when(repository.save(any(CompanyProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.execute(new UpsertCompanyProfileCommand(
                "admin@cybermanager.local",
                "CyberManager SARL",
                "12345678901234",
                "0102030405",
                "contact@cybermanager.local",
                "1 rue du Net",
                null,
                "75001",
                "Paris",
                "France"
        ));

        assertEquals(current.id(), result.id());
        assertEquals("CyberManager SARL", result.legalName());
    }
}
