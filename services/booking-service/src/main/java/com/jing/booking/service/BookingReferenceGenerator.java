package com.jing.booking.service;

import com.jing.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class BookingReferenceGenerator {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 6;
    private static final int MAX_ATTEMPTS = 10;

    private final SecureRandom random = new SecureRandom();
    private final BookingRepository bookingRepository;

    public String generator(){
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            StringBuilder sb = new StringBuilder(LENGTH);

            for (int i = 0; i < LENGTH; i++){
                sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }

            String candidate = sb.toString();
            if(!bookingRepository.existsByBookingReference(candidate)){
                return candidate;
            }
        }

        throw new IllegalStateException("Could not generate a unique booking reference");
    }
}
