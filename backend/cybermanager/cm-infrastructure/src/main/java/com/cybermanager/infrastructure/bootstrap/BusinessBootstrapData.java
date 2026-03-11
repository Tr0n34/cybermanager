package com.cybermanager.infrastructure.bootstrap;

import com.cybermanager.infrastructure.entities.persistence.catalog.ProductJpaEntity;
import com.cybermanager.infrastructure.entities.persistence.customer.CustomerJpaEntity;
import com.cybermanager.infrastructure.entities.persistence.sales.ConnectionPricingTierJpaEntity;
import com.cybermanager.infrastructure.entities.persistence.subscription.SubscriptionOfferJpaEntity;
import com.cybermanager.infrastructure.repositories.persistence.catalog.ProductJpaRepository;
import com.cybermanager.infrastructure.repositories.persistence.customer.CustomerJpaRepository;
import com.cybermanager.infrastructure.repositories.persistence.sales.ConnectionPricingJpaRepository;
import com.cybermanager.infrastructure.repositories.persistence.subscription.SubscriptionOfferJpaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.UUID;

@Configuration
public class BusinessBootstrapData {
    @Bean
    CommandLineRunner bootstrapBusinessData(
            ProductJpaRepository productRepository,
            SubscriptionOfferJpaRepository offerRepository,
            CustomerJpaRepository customerRepository,
            ConnectionPricingJpaRepository pricingRepository
    ) {
        return args -> {
            if (productRepository.count() == 0) {
                var coffee = new ProductJpaEntity();
                coffee.id = UUID.fromString("22222222-2222-2222-2222-222222222222");
                coffee.name = "Cafe";
                coffee.category = "BOISSON";
                coffee.price = new BigDecimal("1.50");
                coffee.status = "ACTIVE";
                productRepository.save(coffee);

                var print = new ProductJpaEntity();
                print.id = UUID.fromString("33333333-3333-3333-3333-333333333333");
                print.name = "Impression";
                print.category = "SERVICE";
                print.price = new BigDecimal("0.20");
                print.status = "ACTIVE";
                productRepository.save(print);
            }

            if (offerRepository.count() == 0) {
                var offer = new SubscriptionOfferJpaEntity();
                offer.id = UUID.fromString("44444444-4444-4444-4444-444444444444");
                offer.name = "Forfait 5h";
                offer.price = new BigDecimal("12.00");
                offer.includedMinutes = 300;
                offer.status = "ACTIVE";
                offerRepository.save(offer);
            }

            if (customerRepository.findById(UUID.fromString("66666666-6666-6666-6666-666666666666")).isEmpty()) {
                var customer = new CustomerJpaEntity();
                customer.id = UUID.fromString("66666666-6666-6666-6666-666666666666");
                customer.name = "Client Abonne";
                customer.type = "SUBSCRIBER";
                customer.status = "ACTIVE";
                customer.remainingMinutes = 180;
                customerRepository.save(customer);
            }

            if (pricingRepository.count() == 0) {
                var thirtyMinutes = new ConnectionPricingTierJpaEntity();
                thirtyMinutes.durationMinutes = 30;
                thirtyMinutes.price = new BigDecimal("1.50");
                pricingRepository.save(thirtyMinutes);

                var oneHour = new ConnectionPricingTierJpaEntity();
                oneHour.durationMinutes = 60;
                oneHour.price = new BigDecimal("2.50");
                pricingRepository.save(oneHour);

                var twoHours = new ConnectionPricingTierJpaEntity();
                twoHours.durationMinutes = 120;
                twoHours.price = new BigDecimal("4.50");
                pricingRepository.save(twoHours);
            }
        };
    }
}
