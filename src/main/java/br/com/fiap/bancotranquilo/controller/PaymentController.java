
package br.com.fiap.bancotranquilo.controller;

import br.com.fiap.bancotranquilo.dto.PaymentRequest;
import br.com.fiap.bancotranquilo.dto.PaymentResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final BigDecimal SIMULATED_LIMIT =
            new BigDecimal("5000.00");

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(
            @Valid @RequestBody PaymentRequest request
    ) {

        String transactionId = "BT-" + UUID.randomUUID();

        if (request.amount().compareTo(SIMULATED_LIMIT) > 0) {

            return ResponseEntity.ok(
                    new PaymentResponse(
                            transactionId,
                            request.orderId(),
                            "DECLINED",
                            "Payment declined by simulated bank"
                    )
            );
        }

        return ResponseEntity.ok(
                new PaymentResponse(
                        transactionId,
                        request.orderId(),
                        "APPROVED",
                        "Payment approved successfully"
                )
        );
    }
}
