package com.cybermanager.application.services.customer;

import com.cybermanager.application.commands.customer.ArchiveCustomersCommand;
import com.cybermanager.application.commands.customer.ConvertCustomerToSubscriberCommand;
import com.cybermanager.application.commands.customer.CreateDebtFromSaleCommand;
import com.cybermanager.application.commands.customer.CreateCustomerCommand;
import com.cybermanager.application.commands.customer.ReattachDebtToSessionCommand;
import com.cybermanager.application.commands.customer.SettleDebtCommand;
import com.cybermanager.application.commands.customer.UpdateCustomerCommand;
import com.cybermanager.application.services.shared.BusinessErrorType;
import com.cybermanager.application.services.shared.BusinessException;
import com.cybermanager.application.queries.customer.GetCustomerDetailsQuery;
import com.cybermanager.application.queries.customer.ExportCustomerDebtReportQuery;
import com.cybermanager.application.queries.customer.ListGeneratedArchiveFilesQuery;
import com.cybermanager.application.queries.customer.SearchCustomerArchiveCandidatesQuery;
import com.cybermanager.application.queries.customer.SearchOpenDebtsQuery;
import com.cybermanager.application.queries.customer.SearchCustomersQuery;
import com.cybermanager.application.usecases.customer.ArchiveCustomersUseCase;
import com.cybermanager.application.usecases.customer.ConvertCustomerToSubscriberUseCase;
import com.cybermanager.application.usecases.customer.CreateDebtFromSaleUseCase;
import com.cybermanager.application.usecases.customer.CreateCustomerUseCase;
import com.cybermanager.application.usecases.customer.GetCustomerDetailsUseCase;
import com.cybermanager.application.usecases.customer.ExportCustomerDebtReportUseCase;
import com.cybermanager.application.usecases.customer.ListGeneratedArchiveFilesUseCase;
import com.cybermanager.application.usecases.customer.ReattachDebtToSessionUseCase;
import com.cybermanager.application.usecases.customer.SearchCustomerArchiveCandidatesUseCase;
import com.cybermanager.application.usecases.customer.SearchOpenDebtsUseCase;
import com.cybermanager.application.usecases.customer.SearchCustomersUseCase;
import com.cybermanager.application.usecases.customer.SettleDebtUseCase;
import com.cybermanager.application.usecases.customer.UpdateCustomerUseCase;
import com.cybermanager.application.views.customer.CustomerArchiveCandidateView;
import com.cybermanager.application.views.customer.CustomerArchiveResultView;
import com.cybermanager.application.views.customer.CustomerDebtReportView;
import com.cybermanager.application.views.customer.ConversionView;
import com.cybermanager.application.views.customer.CustomerDebtSummaryView;
import com.cybermanager.application.views.customer.CustomerDebtView;
import com.cybermanager.application.views.customer.CustomerDetailsView;
import com.cybermanager.application.views.customer.CustomerSaleView;
import com.cybermanager.application.views.customer.CustomerView;
import com.cybermanager.application.views.customer.GeneratedArchiveFileView;
import com.cybermanager.domain.model.customer.Customer;
import com.cybermanager.domain.model.customer.CustomerId;
import com.cybermanager.domain.model.customer.CustomerType;
import com.cybermanager.domain.model.customer.DebtId;
import com.cybermanager.domain.model.customer.DebtRecord;
import com.cybermanager.domain.model.customer.DebtStatus;
import com.cybermanager.domain.model.customer.GeneratedArchiveFile;
import com.cybermanager.domain.model.sales.Sale;
import com.cybermanager.domain.model.sales.SaleLine;
import com.cybermanager.domain.model.sales.SaleType;
import com.cybermanager.domain.model.session.SessionId;
import com.cybermanager.domain.model.subscription.SubscriptionOfferId;
import com.cybermanager.application.services.shared.ActorSupport;
import com.cybermanager.domain.port.customer.CustomerRepository;
import com.cybermanager.domain.port.customer.DebtRepository;
import com.cybermanager.domain.port.customer.GeneratedArchiveFileRepository;
import com.cybermanager.domain.port.sales.SaleRepository;
import com.cybermanager.domain.port.session.CafeSessionRepository;
import com.cybermanager.domain.port.subscription.SubscriptionOfferRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class CustomerApplicationService implements
        CreateCustomerUseCase,
        UpdateCustomerUseCase,
        SearchCustomersUseCase,
        GetCustomerDetailsUseCase,
        ConvertCustomerToSubscriberUseCase,
        CreateDebtFromSaleUseCase,
        ReattachDebtToSessionUseCase,
        SearchOpenDebtsUseCase,
        SettleDebtUseCase,
        SearchCustomerArchiveCandidatesUseCase,
        ArchiveCustomersUseCase,
        ExportCustomerDebtReportUseCase,
        ListGeneratedArchiveFilesUseCase {
    private static final DateTimeFormatter CSV_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String ARCHIVE_MEDIA_TYPE_CSV = "text/csv";
    private static final String ARCHIVE_MEDIA_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String DEBT_REPORT_MEDIA_TYPE_PDF = "application/pdf";

    private final CustomerRepository customerRepository;
    private final SubscriptionOfferRepository subscriptionOfferRepository;
    private final SaleRepository saleRepository;
    private final DebtRepository debtRepository;
    private final GeneratedArchiveFileRepository generatedArchiveFileRepository;
    private final CafeSessionRepository sessionRepository;

    public CustomerApplicationService(
            CustomerRepository customerRepository,
            SubscriptionOfferRepository subscriptionOfferRepository,
            SaleRepository saleRepository,
            DebtRepository debtRepository,
            GeneratedArchiveFileRepository generatedArchiveFileRepository,
            CafeSessionRepository sessionRepository
    ) {
        this.customerRepository = customerRepository;
        this.subscriptionOfferRepository = subscriptionOfferRepository;
        this.saleRepository = saleRepository;
        this.debtRepository = debtRepository;
        this.generatedArchiveFileRepository = generatedArchiveFileRepository;
        this.sessionRepository = sessionRepository;
    }

    @Override
    public CustomerView execute(CreateCustomerCommand command) {
        Customer created;
        if ("SUBSCRIBER".equalsIgnoreCase(command.type())) {
            if (command.subscriptionOfferId() == null) {
                throw new BusinessException(BusinessErrorType.VALIDATION, "SUBSCRIPTION_OFFER_REQUIRED", "Subscription offer is required");
            }
            var offer = subscriptionOfferRepository.findById(new SubscriptionOfferId(command.subscriptionOfferId()))
                    .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "SUBSCRIPTION_OFFER_NOT_FOUND", "Subscription offer not found"));
            created = customerRepository.save(Customer.createWalkIn(command.name()).convertToSubscriber(offer.includedMinutes()));
            saleRepository.save(Sale.create(
                    created.id(),
                    SaleType.SUBSCRIPTION,
                    LocalDateTime.now(),
                    List.of(new SaleLine(offer.name(), 1, offer.price(), offer.price())),
                    offer.price()
            ));
            return toView(created);
        }
        created = customerRepository.save(Customer.createWalkIn(command.name()));
        return toView(created);
    }

    @Override
    public CustomerView execute(UpdateCustomerCommand command) {
        var customer = customerRepository.findById(new CustomerId(command.customerId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found"));
        return toView(customerRepository.save(customer.updateName(command.name())));
    }

    @Transactional(readOnly = true)
    @Override
    public List<CustomerView> execute(SearchCustomersQuery query) {
        CustomerType type = query.type() == null || query.type().isBlank() ? null : CustomerType.valueOf(query.type().toUpperCase());
        return customerRepository.search(query.term(), type).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public CustomerDetailsView execute(GetCustomerDetailsQuery query) {
        var customer = customerRepository.findById(new CustomerId(query.customerId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found"));
        var sales = saleRepository.findByCustomerId(customer.id()).stream()
                .collect(Collectors.toMap(Sale::id, Function.identity(), (left, right) -> left, LinkedHashMap::new))
                .values()
                .stream()
                .toList();
        String currentSubscription = sales.stream()
                .filter(sale -> sale.type() == SaleType.SUBSCRIPTION)
                .findFirst()
                .map(sale -> sale.lines().isEmpty() ? "Abonnement actif" : sale.lines().getFirst().label())
                .orElse(null);

        return new CustomerDetailsView(
                customer.id().value(),
                customer.name(),
                customer.type().name(),
                customer.status().name(),
                customer.remainingMinutes(),
                currentSubscription,
                sales.stream().map(this::toSaleView).toList(),
                debtRepository.findByCustomerId(customer.id()).stream().map(this::toDebtView).toList()
        );
    }

    @Transactional(readOnly = true)
    @Override
    public List<CustomerDebtSummaryView> execute(SearchOpenDebtsQuery query) {
        Map<CustomerId, List<DebtRecord>> debtsByCustomer = debtRepository.findOpenDebts().stream()
                .collect(Collectors.groupingBy(DebtRecord::customerId));
        return debtsByCustomer.entrySet().stream()
                .map(entry -> {
                    Customer customer = customerRepository.findById(entry.getKey())
                            .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found"));
                    var total = entry.getValue().stream()
                            .map(DebtRecord::amount)
                            .reduce(com.cybermanager.domain.model.shared.Money.of("0"), com.cybermanager.domain.model.shared.Money::add);
                    return new CustomerDebtSummaryView(
                            customer.id().value(),
                            customer.name(),
                            customer.type().name(),
                            total.amount(),
                            entry.getValue().stream().map(this::toDebtView).toList()
                    );
                })
                .sorted((left, right) -> right.totalOpenDebt().compareTo(left.totalOpenDebt()))
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<CustomerArchiveCandidateView> execute(SearchCustomerArchiveCandidatesQuery query) {
        return archiveCandidates(query.startDate(), query.endDate(), query.type()).stream()
                .map(candidate -> new CustomerArchiveCandidateView(
                        candidate.customer().id().value(),
                        candidate.customer().name(),
                        candidate.customer().type().name(),
                        candidate.customer().status().name(),
                        candidate.customer().remainingMinutes(),
                        candidate.latestActivityAt(),
                        candidate.sessions().size(),
                        candidate.sales().size(),
                        candidate.debts().size(),
                        candidate.salesTotal(),
                        candidate.debtTotal()
                ))
                .toList();
    }

    @Override
    public void execute(SettleDebtCommand command) {
        var debt = debtRepository.findById(new DebtId(command.debtId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "DEBT_NOT_FOUND", "Debt not found"));
        debtRepository.save(debt.settle(LocalDateTime.now(), command.comment()));
    }

    @Override
    public void execute(CreateDebtFromSaleCommand command) {
        var sale = saleRepository.findById(new com.cybermanager.domain.model.sales.SaleId(command.saleId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "SALE_NOT_FOUND", "Sale not found"));
        var existingOpenDebt = debtRepository.findByCustomerId(sale.customerId()).stream()
                .anyMatch(debt -> debt.status() == DebtStatus.OPEN
                        && debt.label().equals(debtLabelForSale(sale))
                        && debt.amount().amount().compareTo(sale.totalAmount().amount()) == 0);
        if (existingOpenDebt) {
            throw new BusinessException(BusinessErrorType.CONFLICT, "SALE_ALREADY_IN_DEBT", "Sale is already in debt");
        }
        debtRepository.save(DebtRecord.create(sale.customerId(), debtLabelForSale(sale), sale.totalAmount(), LocalDateTime.now()));
    }

    @Override
    public void execute(ReattachDebtToSessionCommand command) {
        var debt = debtRepository.findById(new DebtId(command.debtId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "DEBT_NOT_FOUND", "Debt not found"));
        if (debt.status() != DebtStatus.OPEN) {
            throw new BusinessException(BusinessErrorType.CONFLICT, "DEBT_ALREADY_SETTLED", "Debt is already settled");
        }
        var session = sessionRepository.findById(new SessionId(command.sessionId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "SESSION_NOT_FOUND", "Session not found"));
        if (!session.customerId().equals(debt.customerId())) {
            throw new BusinessException(BusinessErrorType.CONFLICT, "SESSION_CUSTOMER_MISMATCH", "Debt does not belong to the same customer");
        }
        if (session.endedAt() != null && session.paid()) {
            throw new BusinessException(BusinessErrorType.CONFLICT, "SESSION_ALREADY_PAID", "Cannot reattach debt to a paid session");
        }

        debtRepository.save(debt.settle(LocalDateTime.now(), "Reintegree a la session " + session.id().value()));
        var existingSale = saleRepository.findByCustomerId(debt.customerId()).stream()
                .filter(sale -> debtLabelForSale(sale).equals(debt.label()))
                .filter(sale -> sale.totalAmount().amount().compareTo(debt.amount().amount()) == 0)
                .findFirst();

        if (existingSale.isPresent()) {
            var sale = existingSale.get();
            saleRepository.save(new Sale(
                    sale.id(),
                    sale.customerId(),
                    session.id().value(),
                    sale.type(),
                    sale.soldAt(),
                    sale.lines(),
                    sale.totalAmount()
            ));
            return;
        }

        var soldAt = LocalDateTime.now();
        saleRepository.save(Sale.create(
                debt.customerId(),
                session.id().value(),
                saleTypeForDebtLabel(debt.label()),
                soldAt,
                saleLinesForDebt(debt),
                debt.amount()
        ));
    }

    @Override
    public ConversionView execute(ConvertCustomerToSubscriberCommand command) {
        var customer = customerRepository.findById(new CustomerId(command.customerId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found"));
        var offer = subscriptionOfferRepository.findById(new SubscriptionOfferId(command.subscriptionOfferId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "SUBSCRIPTION_OFFER_NOT_FOUND", "Subscription offer not found"));
        int deductedMinutes = command.deductCurrentSession() ? 30 : 0;
        var converted = customerRepository.save(customer.convertToSubscriber(Math.max(0, offer.includedMinutes() - deductedMinutes)));
        var sale = saleRepository.save(Sale.create(
                converted.id(),
                command.sessionId(),
                SaleType.SUBSCRIPTION,
                LocalDateTime.now(),
                List.of(new SaleLine(offer.name(), 1, offer.price(), offer.price())),
                offer.price()
        ));
        return new ConversionView(toView(converted), sale.id().value(), deductedMinutes);
    }

    @Override
    public CustomerArchiveResultView execute(ArchiveCustomersCommand command) {
        var candidates = archiveCandidates(command.startDate(), command.endDate(), command.type());
        ArchiveDocument archiveDocument = buildArchiveDocument(command.startDate(), command.endDate(), command.type(), command.format(), candidates);
        var customerIds = candidates.stream().map(candidate -> candidate.customer().id()).toList();
        if (!customerIds.isEmpty()) {
            debtRepository.deleteByCustomerIds(customerIds);
            saleRepository.deleteByCustomerIds(customerIds);
            sessionRepository.deleteByCustomerIds(customerIds);
            customerRepository.deleteByIds(customerIds);
        }
        var result = new CustomerArchiveResultView(
                archiveDocument.fileName(),
                archiveDocument.mediaType(),
                archiveDocument.content(),
                customerIds.size()
        );
        generatedArchiveFileRepository.save(GeneratedArchiveFile.create(result.fileName(), archiveDocument.mediaType(), command.actorEmail(), LocalDateTime.now()));
        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public CustomerDebtReportView execute(ExportCustomerDebtReportQuery query) {
        var reportEntries = debtReportEntries();
        var result = new CustomerDebtReportView(
                "customer-open-debts-report.pdf",
                DEBT_REPORT_MEDIA_TYPE_PDF,
                buildDebtReportPdf(reportEntries)
        );
        generatedArchiveFileRepository.save(GeneratedArchiveFile.create(result.fileName(), "PDF_DEBTS", query.actorEmail(), LocalDateTime.now()));
        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public List<GeneratedArchiveFileView> execute(ListGeneratedArchiveFilesQuery query) {
        return generatedArchiveFileRepository.findRecent().stream()
                .map(file -> new GeneratedArchiveFileView(file.id(), file.fileName(), file.fileType(), file.generatedBy(), file.generatedAt()))
                .toList();
    }

    private CustomerView toView(Customer customer) {
        var totalDebt = debtRepository.findByCustomerId(customer.id()).stream()
                .filter(debt -> debt.status() == DebtStatus.OPEN)
                .map(DebtRecord::amount)
                .reduce(com.cybermanager.domain.model.shared.Money.of("0"), com.cybermanager.domain.model.shared.Money::add);
        return new CustomerView(customer.id().value(), customer.name(), customer.type().name(), customer.status().name(), customer.remainingMinutes(), totalDebt.amount());
    }

    private CustomerSaleView toSaleView(Sale sale) {
        String debtLabel = debtLabelForSale(sale);
        String label = sale.lines().isEmpty() ? sale.type().name() : sale.lines().stream().map(SaleLine::label).reduce((first, second) -> first + ", " + second).orElse(sale.type().name());
        boolean openDebt = debtRepository.findByCustomerId(sale.customerId()).stream()
                .anyMatch(debt -> debt.status() == DebtStatus.OPEN
                        && debt.label().equals(debtLabel)
                        && debt.amount().amount().compareTo(sale.totalAmount().amount()) == 0);
        return new CustomerSaleView(sale.id().value(), sale.sessionId(), sale.type().name(), label, debtLabel, sale.soldAt(), sale.totalAmount().amount(), openDebt);
    }

    private String debtLabelForSale(Sale sale) {
        return switch (sale.type()) {
            case PRODUCTS -> sale.lines().stream()
                    .map(line -> line.quantity() + " x " + line.label())
                    .reduce((left, right) -> left + ", " + right)
                    .map(label -> label + " du " + com.cybermanager.application.services.shared.DateTimeLabelFormatter.format(sale.soldAt()))
                    .orElse("Vente produits du " + com.cybermanager.application.services.shared.DateTimeLabelFormatter.format(sale.soldAt()));
            case SUBSCRIPTION -> "Vente abonnement du " + com.cybermanager.application.services.shared.DateTimeLabelFormatter.format(sale.soldAt());
            case CONNECTION_TIME -> "Vente temps du " + com.cybermanager.application.services.shared.DateTimeLabelFormatter.format(sale.soldAt());
        };
    }

    private SaleType saleTypeForDebtLabel(String debtLabel) {
        if (debtLabel.startsWith("Vente abonnement du ")) {
            return SaleType.SUBSCRIPTION;
        }
        if (debtLabel.startsWith("Vente temps du ") || debtLabel.startsWith("Session du ")) {
            return SaleType.CONNECTION_TIME;
        }
        return SaleType.PRODUCTS;
    }

    private List<SaleLine> saleLinesForDebt(DebtRecord debt) {
        if (debt.label().startsWith("Vente abonnement du ")) {
            return List.of(new SaleLine("Abonnement", 1, debt.amount(), debt.amount()));
        }
        if (debt.label().startsWith("Vente temps du ") || debt.label().startsWith("Session du ")) {
            return List.of(new SaleLine("Temps de connexion", 1, debt.amount(), debt.amount()));
        }

        String label = debt.label();
        int dateIndex = label.lastIndexOf(" du ");
        String productLabel = dateIndex > 0 ? label.substring(0, dateIndex) : label;
        return List.of(new SaleLine(productLabel, 1, debt.amount(), debt.amount()));
    }

    private CustomerDebtView toDebtView(DebtRecord debtRecord) {
        return new CustomerDebtView(
                debtRecord.id().value(),
                debtRecord.label(),
                debtRecord.comment(),
                debtRecord.amount().amount(),
                debtRecord.status().name(),
                debtRecord.createdAt(),
                debtRecord.settledAt()
        );
    }

    private List<ArchiveCustomerCandidate> archiveCandidates(LocalDate startDate, LocalDate endDate, String type) {
        validateArchivePeriod(startDate, endDate);
        CustomerType customerType = requestedArchiveCustomerType(type);
        List<Customer> customers = customerRepository.search("", customerType);
        if (customers.isEmpty()) {
            return List.of();
        }

        var customerIds = customers.stream().map(Customer::id).toList();
        Map<CustomerId, List<Sale>> salesByCustomer = saleRepository.findByCustomerIds(customerIds).stream()
                .collect(Collectors.groupingBy(Sale::customerId));
        Map<CustomerId, List<DebtRecord>> debtsByCustomer = debtRepository.findByCustomerIds(customerIds).stream()
                .collect(Collectors.groupingBy(DebtRecord::customerId));
        Map<CustomerId, List<com.cybermanager.domain.model.session.CafeSession>> sessionsByCustomer = sessionRepository.findByCustomerIds(customerIds).stream()
                .collect(Collectors.groupingBy(com.cybermanager.domain.model.session.CafeSession::customerId));

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();

        return customers.stream()
                .map(customer -> buildArchiveCandidate(customer, salesByCustomer.getOrDefault(customer.id(), List.of()), debtsByCustomer.getOrDefault(customer.id(), List.of()), sessionsByCustomer.getOrDefault(customer.id(), List.of()), start, endExclusive))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ArchiveCustomerCandidate::latestActivityAt).thenComparing(candidate -> candidate.customer().name()))
                .toList();
    }

    private ArchiveCustomerCandidate buildArchiveCandidate(
            Customer customer,
            List<Sale> sales,
            List<DebtRecord> debts,
            List<com.cybermanager.domain.model.session.CafeSession> sessions,
            LocalDateTime start,
            LocalDateTime endExclusive
    ) {
        boolean hasActiveSession = sessions.stream().anyMatch(session -> session.endedAt() == null && !session.paid());
        if (hasActiveSession) {
            return null;
        }
        if (customer.type() != CustomerType.WALK_IN) {
            return null;
        }
        boolean hasOpenDebt = debts.stream().anyMatch(debt -> debt.status() == DebtStatus.OPEN);
        if (hasOpenDebt || customer.remainingMinutes() > 0) {
            return null;
        }

        LocalDateTime latestActivityAt = latestActivityAt(sales, debts, sessions);
        if (latestActivityAt == null || latestActivityAt.isBefore(start) || !latestActivityAt.isBefore(endExclusive)) {
            return null;
        }

        BigDecimal salesTotal = sales.stream()
                .map(sale -> sale.totalAmount().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal debtTotal = debts.stream()
                .map(debt -> debt.amount().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ArchiveCustomerCandidate(customer, latestActivityAt, sessions, sales, debts, salesTotal, debtTotal);
    }

    private List<DebtReportEntry> debtReportEntries() {
        Map<CustomerId, List<DebtRecord>> openDebtsByCustomer = debtRepository.findOpenDebts().stream()
                .collect(Collectors.groupingBy(DebtRecord::customerId));
        if (openDebtsByCustomer.isEmpty()) {
            return List.of();
        }

        return openDebtsByCustomer.entrySet().stream()
                .map(entry -> customerRepository.findById(entry.getKey())
                        .map(customer -> new DebtReportEntry(
                                customer,
                                entry.getValue().stream().sorted(Comparator.comparing(DebtRecord::createdAt)).toList(),
                                entry.getValue().stream().map(debt -> debt.amount().amount()).reduce(BigDecimal.ZERO, BigDecimal::add)
                        ))
                        .orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(entry -> entry.customer().name()))
                .toList();
    }

    private LocalDateTime latestActivityAt(
            List<Sale> sales,
            List<DebtRecord> debts,
            List<com.cybermanager.domain.model.session.CafeSession> sessions
    ) {
        return java.util.stream.Stream.of(
                        sales.stream().map(Sale::soldAt),
                        debts.stream().map(DebtRecord::createdAt),
                        sessions.stream().map(session -> session.endedAt() != null ? session.endedAt() : session.startedAt())
                )
                .flatMap(Function.identity())
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private byte[] buildArchiveCsv(LocalDate startDate, LocalDate endDate, List<ArchiveCustomerCandidate> candidates) {
        StringBuilder csv = new StringBuilder();
        csv.append("customerId,name,type,status,remainingMinutes,periodStart,periodEnd,latestActivityAt,sessionCount,saleCount,debtCount,salesTotal,debtTotal,sessions,sales,debts")
                .append(System.lineSeparator());
        for (ArchiveCustomerCandidate candidate : candidates) {
            csv.append(csv(candidate.customer().id().value()))
                    .append(',')
                    .append(csv(candidate.customer().name()))
                    .append(',')
                    .append(csv(candidate.customer().type().name()))
                    .append(',')
                    .append(csv(candidate.customer().status().name()))
                    .append(',')
                    .append(candidate.customer().remainingMinutes())
                    .append(',')
                    .append(csv(startDate))
                    .append(',')
                    .append(csv(endDate))
                    .append(',')
                    .append(csv(candidate.latestActivityAt()))
                    .append(',')
                    .append(candidate.sessions().size())
                    .append(',')
                    .append(candidate.sales().size())
                    .append(',')
                    .append(candidate.debts().size())
                    .append(',')
                    .append(candidate.salesTotal())
                    .append(',')
                    .append(candidate.debtTotal())
                    .append(',')
                    .append(csv(candidate.sessions().stream()
                            .sorted(Comparator.comparing(com.cybermanager.domain.model.session.CafeSession::startedAt))
                            .map(session -> session.workstation() + " du " + csvValue(session.startedAt()) + " au " + csvValue(session.endedAt()))
                            .collect(Collectors.joining(" | "))))
                    .append(',')
                    .append(csv(candidate.sales().stream()
                            .sorted(Comparator.comparing(Sale::soldAt))
                            .map(sale -> sale.type().name() + " " + csvValue(sale.soldAt()) + " " + sale.totalAmount().amount())
                            .collect(Collectors.joining(" | "))))
                    .append(',')
                    .append(csv(candidate.debts().stream()
                            .sorted(Comparator.comparing(DebtRecord::createdAt))
                            .map(debt -> debt.status().name() + " " + csvValue(debt.createdAt()) + " " + debt.label() + " " + debt.amount().amount())
                            .collect(Collectors.joining(" | "))))
                    .append(System.lineSeparator());
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private ArchiveDocument buildArchiveDocument(LocalDate startDate, LocalDate endDate, String type, String format, List<ArchiveCustomerCandidate> candidates) {
        String normalizedFormat = format == null || format.isBlank() ? "csv" : format.toLowerCase();
        if ("xlsx".equals(normalizedFormat)) {
            return new ArchiveDocument(
                    archiveFileName(startDate, endDate, type, "xlsx"),
                    ARCHIVE_MEDIA_TYPE_XLSX,
                    buildArchiveXlsx(startDate, endDate, candidates)
            );
        }
        return new ArchiveDocument(
                archiveFileName(startDate, endDate, type, "csv"),
                ARCHIVE_MEDIA_TYPE_CSV,
                buildArchiveCsv(startDate, endDate, candidates)
        );
    }

    private byte[] buildArchiveXlsx(LocalDate startDate, LocalDate endDate, List<ArchiveCustomerCandidate> candidates) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Archivage");
            String[] headers = {
                    "customerId", "name", "type", "status", "remainingMinutes", "periodStart", "periodEnd", "latestActivityAt",
                    "sessionCount", "saleCount", "debtCount", "salesTotal", "debtTotal", "sessions", "sales", "debts"
            };
            Row headerRow = sheet.createRow(0);
            for (int index = 0; index < headers.length; index++) {
                headerRow.createCell(index).setCellValue(headers[index]);
            }
            int rowIndex = 1;
            for (ArchiveCustomerCandidate candidate : candidates) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(candidate.customer().id().value().toString());
                row.createCell(1).setCellValue(candidate.customer().name());
                row.createCell(2).setCellValue(candidate.customer().type().name());
                row.createCell(3).setCellValue(candidate.customer().status().name());
                row.createCell(4).setCellValue(candidate.customer().remainingMinutes());
                row.createCell(5).setCellValue(startDate.toString());
                row.createCell(6).setCellValue(endDate.toString());
                row.createCell(7).setCellValue(csvValue(candidate.latestActivityAt()));
                row.createCell(8).setCellValue(candidate.sessions().size());
                row.createCell(9).setCellValue(candidate.sales().size());
                row.createCell(10).setCellValue(candidate.debts().size());
                row.createCell(11).setCellValue(candidate.salesTotal().doubleValue());
                row.createCell(12).setCellValue(candidate.debtTotal().doubleValue());
                row.createCell(13).setCellValue(candidate.sessions().stream()
                        .sorted(Comparator.comparing(com.cybermanager.domain.model.session.CafeSession::startedAt))
                        .map(session -> session.workstation() + " du " + csvValue(session.startedAt()) + " au " + csvValue(session.endedAt()))
                        .collect(Collectors.joining(" | ")));
                row.createCell(14).setCellValue(candidate.sales().stream()
                        .sorted(Comparator.comparing(Sale::soldAt))
                        .map(sale -> sale.type().name() + " " + csvValue(sale.soldAt()) + " " + sale.totalAmount().amount())
                        .collect(Collectors.joining(" | ")));
                row.createCell(15).setCellValue(candidate.debts().stream()
                        .sorted(Comparator.comparing(DebtRecord::createdAt))
                        .map(debt -> debt.status().name() + " " + csvValue(debt.createdAt()) + " " + debt.label() + " " + debt.amount().amount())
                        .collect(Collectors.joining(" | ")));
            }
            for (int index = 0; index < headers.length; index++) {
                sheet.autoSizeColumn(index);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new BusinessException(BusinessErrorType.CONFLICT, "ARCHIVE_EXPORT_FAILED", "Archive XLSX export failed");
        }
    }

    private byte[] buildDebtReportPdf(List<DebtReportEntry> entries) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream stream = new PDPageContentStream(document, page);
            float y = 800;
            stream.beginText();
            stream.setFont(boldFont, 14);
            stream.newLineAtOffset(50, y);
            stream.showText("Rapport complet des dettes ouvertes");
            stream.endText();
            y -= 28;

            for (DebtReportEntry entry : entries) {
                if (y < 120) {
                    stream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    stream = new PDPageContentStream(document, page);
                    y = 800;
                }
                stream.beginText();
                stream.setFont(boldFont, 11);
                stream.newLineAtOffset(50, y);
                stream.showText(entry.customer().name() + " - " + entry.customer().type().name() + " - Total " + entry.total().toPlainString() + " EUR");
                stream.endText();
                y -= 18;
                for (DebtRecord debt : entry.debts()) {
                    if (y < 80) {
                        stream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        stream = new PDPageContentStream(document, page);
                        y = 800;
                    }
                    stream.beginText();
                    stream.setFont(font, 9);
                    stream.newLineAtOffset(60, y);
                    stream.showText(csvValue(debt.createdAt()) + " | " + debt.label() + " | " + debt.amount().amount().toPlainString() + " EUR");
                    stream.endText();
                    y -= 14;
                }
                y -= 10;
            }
            stream.close();
            document.save(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new BusinessException(BusinessErrorType.CONFLICT, "DEBT_REPORT_EXPORT_FAILED", "Debt report export failed");
        }
    }

    private String archiveFileName(LocalDate startDate, LocalDate endDate, String type, String extension) {
        return "customers-archive-" + normalizedArchiveType(type) + "-" + startDate + "-to-" + endDate + "." + extension;
    }

    private void validateArchivePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessException(BusinessErrorType.VALIDATION, "ARCHIVE_PERIOD_REQUIRED", "Archive period is required");
        }
        if (endDate.isBefore(startDate)) {
            throw new BusinessException(BusinessErrorType.VALIDATION, "ARCHIVE_PERIOD_INVALID", "Archive end date must be after start date");
        }
    }

    private String csv(Object value) {
        return "\"" + csvValue(value).replace("\"", "\"\"") + "\"";
    }

    private String csvValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.format(CSV_DATE_TIME);
        }
        return String.valueOf(value);
    }

    private CustomerType requestedArchiveCustomerType(String type) {
        if (type == null || type.isBlank()) {
            return CustomerType.WALK_IN;
        }
        CustomerType requestedType = CustomerType.valueOf(type.toUpperCase());
        if (requestedType == CustomerType.SUBSCRIBER) {
            return CustomerType.SUBSCRIBER;
        }
        return CustomerType.WALK_IN;
    }

    private String normalizedArchiveType(String type) {
        return type == null || type.isBlank() ? "walk_in" : type.toLowerCase();
    }

    private record ArchiveCustomerCandidate(
            Customer customer,
            LocalDateTime latestActivityAt,
            List<com.cybermanager.domain.model.session.CafeSession> sessions,
            List<Sale> sales,
            List<DebtRecord> debts,
            BigDecimal salesTotal,
            BigDecimal debtTotal
    ) {
    }

    private record ArchiveDocument(String fileName, String mediaType, byte[] content) {
    }

    private record DebtReportEntry(Customer customer, List<DebtRecord> debts, BigDecimal total) {
    }
}
