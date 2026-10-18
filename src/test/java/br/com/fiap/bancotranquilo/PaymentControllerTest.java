
package br.com.fiap.bancotranquilo;

import br.com.fiap.bancotranquilo.controller.PaymentController;
import br.com.fiap.bancotranquilo.dto.PaymentRequest;
import br.com.fiap.bancotranquilo.dto.PaymentResponse;
import br.com.fiap.bancotranquilo.exception.PaymentConflictException;
import br.com.fiap.bancotranquilo.service.PaymentService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentControllerTest {

    private PaymentController paymentController;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = mock(PaymentService.class);
        paymentController = new PaymentController(paymentService);
    }

    @Test
    void shouldReturnApprovedPayment() {

        PaymentRequest request = new PaymentRequest(
                1L,
                new BigDecimal("3200.00")
        );

        PaymentResponse expected = new PaymentResponse(
                "BT-123",
                1L,
                "APPROVED",
                "Payment approved successfully"
        );

        when(paymentService.processPayment(request))
                .thenReturn(expected);

        ResponseEntity<PaymentResponse> response =
                paymentController.processPayment(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());

        verify(paymentService).processPayment(request);
    }

    @Test
    void shouldReturnDeclinedPayment() {

        PaymentRequest request = new PaymentRequest(
                2L,
                new BigDecimal("6400.00")
        );

        PaymentResponse expected = new PaymentResponse(
                "BT-456",
                2L,
                "DECLINED",
                "Payment declined by simulated bank"
        );

        when(paymentService.processPayment(request))
                .thenReturn(expected);

        ResponseEntity<PaymentResponse> response =
                paymentController.processPayment(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }

    @Test
    void shouldReturnSameTransactionForRepeatedRequest() {

        PaymentRequest request = new PaymentRequest(
                3L,
                new BigDecimal("1000.00")
        );

        PaymentResponse expected = new PaymentResponse(
                "BT-SAME",
                3L,
                "APPROVED",
                "Payment approved successfully"
        );

        when(paymentService.processPayment(request))
                .thenReturn(expected);

        PaymentResponse first =
                paymentController.processPayment(request).getBody();

        PaymentResponse second =
                paymentController.processPayment(request).getBody();

        assertNotNull(first);
        assertEquals(first, second);
        assertEquals(first.transactionId(), second.transactionId());

        verify(paymentService, times(2))
                .processPayment(request);
    }

    @Test
    void shouldPropagatePaymentConflict() {

        PaymentRequest request = new PaymentRequest(
                4L,
                new BigDecimal("2000.00")
        );

        when(paymentService.processPayment(request))
                .thenThrow(new PaymentConflictException(
                        "Order already has a payment with a different amount"
                ));

        assertThrows(
                PaymentConflictException.class,
                () -> paymentController.processPayment(request)
        );
    }
}
