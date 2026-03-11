package com.cybermanager.application.views.customer;

import java.util.UUID;

public record ConversionView(CustomerView customer, UUID saleId, int deductedMinutes) {
}
