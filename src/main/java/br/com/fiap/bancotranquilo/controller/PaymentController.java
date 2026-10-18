
package br.com.fiap.bancotranquilo.controller;

import br.com.fiap.bancotranquilo.dto.PaymentRequest;
import br.com.fiap.bancotranquilo.dto.PaymentResponse;
import br.com.fiap.bancotranquilo.service.PaymentService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(
            @Valid @RequestBody PaymentRequest request
    ) {
        return ResponseEntity.ok(
                paymentService.processPayment(request)
        );
    }
}
