package com.cybermanager.domain.port.session;

import com.cybermanager.domain.model.session.CafeSession;
import com.cybermanager.domain.model.session.SessionId;
import com.cybermanager.domain.model.customer.CustomerId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CafeSessionRepository {
    CafeSession save(CafeSession session);
    Optional<CafeSession> findById(SessionId sessionId);
    List<CafeSession> findByDay(LocalDate date);
    List<CafeSession> findAll();
    List<CafeSession> findActive();
    List<CafeSession> findByCustomerIds(List<CustomerId> customerIds);
    void deleteByCustomerIds(List<CustomerId> customerIds);
}

