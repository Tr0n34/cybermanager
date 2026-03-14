package com.cybermanager.application.views.customer;

public record CustomerArchiveResultView(String fileName, String mediaType, byte[] content, int archivedCustomers) {
}
