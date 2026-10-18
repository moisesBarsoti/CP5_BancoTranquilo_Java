
package br.com.fiap.bancotranquilo;

import br.com.fiap.bancotranquilo.dto.PaymentRequest;
import br.com.fiap.bancotranquilo.dto.PaymentResponse;
import br.com.fiap.bancotranquilo.entity.Payment;
import br.com.fiap.bancotranquilo.exception.PaymentConflictException;
import br.com.fiap.bancotranquilo.repository.PaymentRepository;
import br.com.fiap.bancotranquilo.service.PaymentService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    private PaymentRepository paymentRepository;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        paymentService = new PaymentService(paymentRepository);
    }

    @Test
    void shouldApprovePaymentBelowLimit() {
        mockStoredPayment(10L, "3200.00", "BT-123", "APPROVED");

        PaymentResponse response = paymentService.processPayment(
                request(10L, "3200.00")
        );

        assertEquals("APPROVED", response.status());
        assertEquals("BT-123", response.transactionId());

        verify(paymentRepository).insertIfAbsent(
                eq(10L),
                eq(new BigDecimal("3200.00")),
                anyString(),
                eq("APPROVED"),
                eq("Payment approved successfully")
        );
    }

    @Test
    void shouldApprovePaymentAtLimit() {
        mockStoredPayment(11L, "5000.00", "BT-456", "APPROVED");

        PaymentResponse response = paymentService.processPayment(
                request(11L, "5000.00")
        );

        assertEquals("APPROVED", response.status());
    }

    @Test
    void shouldDeclinePaymentAboveLimit() {
        mockStoredPayment(12L, "6400.00", "BT-789", "DECLINED");

        PaymentResponse response = paymentService.processPayment(
                request(12L, "6400.00")
        );

        assertEquals("DECLINED", response.status());
        assertEquals("BT-789", response.transactionId());

        verify(paymentRepository).insertIfAbsent(
                eq(12L),
                eq(new BigDecimal("6400.00")),
                anyString(),
                eq("DECLINED"),
                eq("Payment declined by simulated bank")
        );
    }

    @Test
    void shouldReuseExistingTransaction() {
        PaymentRequest request = request(13L, "3200.00");

        mockStoredPayment(
                13L,
                "3200.00",
                "BT-ORIGINAL",
                "APPROVED"
        );

        PaymentResponse first = paymentService.processPayment(request);
        PaymentResponse second = paymentService.processPayment(request);

        assertEquals(first, second);
        assertEquals("BT-ORIGINAL", second.transactionId());

        verify(paymentRepository, times(2)).insertIfAbsent(
                eq(13L),
                any(BigDecimal.class),
                anyString(),
                eq("APPROVED"),
                eq("Payment approved successfully")
        );
    }

    @Test
    void shouldRejectDifferentAmountForSameOrder() {
        mockStoredPayment(
                14L,
                "3200.00",
                "BT-ORIGINAL",
                "APPROVED"
        );

        assertThrows(
                PaymentConflictException.class,
                () -> paymentService.processPayment(
                        request(14L, "4000.00")
                )
        );
    }

    @Test
    void shouldAcceptEquivalentMonetaryScale() {
        mockStoredPayment(
                15L,
                "3200.00",
                "BT-ORIGINAL",
                "APPROVED"
        );

        PaymentResponse response = paymentService.processPayment(
                request(15L, "3200.0")
        );

        assertEquals("BT-ORIGINAL", response.transactionId());
    }

    private PaymentRequest request(Long orderId, String amount) {
        return new PaymentRequest(
                orderId,
                new BigDecimal(amount)
        );
    }

    private void mockStoredPayment(
            Long orderId,
            String amount,
            String transactionId,
            String status
    ) {
        String message = status.equals("APPROVED")
                ? "Payment approved successfully"
                : "Payment declined by simulated bank";

        Payment payment = new Payment(
                orderId,
                new BigDecimal(amount),
                transactionId,
                status,
                message
        );

        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(payment));
    }
}
