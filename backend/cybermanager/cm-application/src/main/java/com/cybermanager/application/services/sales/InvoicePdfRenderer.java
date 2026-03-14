package com.cybermanager.application.services.sales;

import com.cybermanager.domain.model.company.CompanyProfile;
import com.cybermanager.domain.model.sales.Invoice;
import com.cybermanager.domain.model.sales.InvoiceLine;
import com.cybermanager.domain.port.company.CompanyProfileRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class InvoicePdfRenderer {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CompanyProfileRepository companyProfileRepository;

    public InvoicePdfRenderer(CompanyProfileRepository companyProfileRepository) {
        this.companyProfileRepository = companyProfileRepository;
    }

    public byte[] render(Invoice invoice) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = 790;
                y = writeHeader(content, invoice, y);
                y = writeCompany(content, y, companyProfileRepository.findCurrent());
                y = writeCustomer(content, invoice, y - 10);
                y = writeLines(content, invoice, y - 16);
                writeTotal(content, invoice, 90);
            }

            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to render invoice PDF", exception);
        }
    }

    private float writeHeader(PDPageContentStream content, Invoice invoice, float y) throws IOException {
        write(content, Standard14Fonts.FontName.HELVETICA_BOLD, 18, 40, y, "Facture " + invoice.invoiceNumber());
        write(content, Standard14Fonts.FontName.HELVETICA, 10, 40, y - 18, "Date d emission : " + invoice.issuedAt().format(DATE_TIME));
        write(content, Standard14Fonts.FontName.HELVETICA, 10, 40, y - 32, "Statut : " + invoice.status().name());
        return y - 58;
    }

    private float writeCompany(PDPageContentStream content, float y, Optional<CompanyProfile> company) throws IOException {
        write(content, Standard14Fonts.FontName.HELVETICA_BOLD, 12, 40, y, "Entreprise");
        if (company.isEmpty()) {
            write(content, Standard14Fonts.FontName.HELVETICA, 10, 40, y - 14, "Entreprise non configuree");
            return y - 36;
        }

        CompanyProfile current = company.get();
        float lineY = y - 14;
        lineY = writeOptional(content, lineY, current.legalName());
        lineY = writeOptional(content, lineY, current.addressLine1());
        lineY = writeOptional(content, lineY, current.addressLine2());
        lineY = writeOptional(content, lineY, current.postalCode() == null && current.city() == null
                ? null
                : ((current.postalCode() == null ? "" : current.postalCode() + " ") + (current.city() == null ? "" : current.city())).trim());
        lineY = writeOptional(content, lineY, current.country());
        lineY = writeOptional(content, lineY, current.siret() == null ? null : "SIRET : " + current.siret());
        lineY = writeOptional(content, lineY, current.phone() == null ? null : "Tel : " + current.phone());
        lineY = writeOptional(content, lineY, current.email() == null ? null : "Email : " + current.email());
        return lineY - 8;
    }

    private float writeCustomer(PDPageContentStream content, Invoice invoice, float y) throws IOException {
        write(content, Standard14Fonts.FontName.HELVETICA_BOLD, 12, 40, y, "Client");
        write(content, Standard14Fonts.FontName.HELVETICA, 10, 40, y - 14, invoice.customerName());
        return y - 36;
    }

    private float writeLines(PDPageContentStream content, Invoice invoice, float y) throws IOException {
        write(content, Standard14Fonts.FontName.HELVETICA_BOLD, 11, 40, y, "Libelle");
        write(content, Standard14Fonts.FontName.HELVETICA_BOLD, 11, 300, y, "Qt");
        write(content, Standard14Fonts.FontName.HELVETICA_BOLD, 11, 360, y, "PU");
        write(content, Standard14Fonts.FontName.HELVETICA_BOLD, 11, 450, y, "Total");
        float currentY = y - 16;
        for (InvoiceLine line : invoice.lines()) {
            write(content, Standard14Fonts.FontName.HELVETICA, 10, 40, currentY, line.label());
            write(content, Standard14Fonts.FontName.HELVETICA, 10, 300, currentY, String.valueOf(line.quantity()));
            write(content, Standard14Fonts.FontName.HELVETICA, 10, 360, currentY, line.unitPrice().amount().toPlainString() + " EUR");
            write(content, Standard14Fonts.FontName.HELVETICA, 10, 450, currentY, line.totalPrice().amount().toPlainString() + " EUR");
            currentY -= 15;
        }
        return currentY;
    }

    private void writeTotal(PDPageContentStream content, Invoice invoice, float y) throws IOException {
        write(content, Standard14Fonts.FontName.HELVETICA_BOLD, 12, 360, y, "Total");
        write(content, Standard14Fonts.FontName.HELVETICA_BOLD, 12, 450, y, invoice.totalAmount().amount().toPlainString() + " EUR");
    }

    private float writeOptional(PDPageContentStream content, float y, String value) throws IOException {
        if (value == null || value.isBlank()) {
            return y;
        }
        write(content, Standard14Fonts.FontName.HELVETICA, 10, 40, y, value);
        return y - 13;
    }

    private void write(PDPageContentStream content, Standard14Fonts.FontName fontName, float size, float x, float y, String value) throws IOException {
        content.beginText();
        content.setFont(new PDType1Font(fontName), size);
        content.newLineAtOffset(x, y);
        content.showText(value);
        content.endText();
    }
}
