
package br.com.fiap.bancotranquilo;

import br.com.fiap.bancotranquilo.controller.PaymentController;
import br.com.fiap.bancotranquilo.dto.PaymentRequest;
import br.com.fiap.bancotranquilo.dto.PaymentResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentControllerTest {

    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        paymentController = new PaymentController();
    }

    @Test
    void shouldApprovePaymentBelowLimit() {

        PaymentRequest request = createRequest(
                1L,
                "3200.00"
        );

        ResponseEntity<PaymentResponse> response =
                paymentController.processPayment(request);

        assertEquals(200, response.getStatusCode().value());

        assertNotNull(response.getBody());

        assertEquals(
                "APPROVED",
                response.getBody().status()
        );

        assertEquals(
                1L,
                response.getBody().orderId()
        );
    }

    @Test
    void shouldApprovePaymentAtLimit() {

        PaymentRequest request = createRequest(
                2L,
                "5000.00"
        );

        ResponseEntity<PaymentResponse> response =
                paymentController.processPayment(request);

        assertNotNull(response.getBody());

        assertEquals(
                "APPROVED",
                response.getBody().status()
        );
    }

    @Test
    void shouldDeclinePaymentAboveLimit() {

        PaymentRequest request = createRequest(
                3L,
                "6400.00"
        );

        ResponseEntity<PaymentResponse> response =
                paymentController.processPayment(request);

        assertEquals(200, response.getStatusCode().value());

        assertNotNull(response.getBody());

        assertEquals(
                "DECLINED",
                response.getBody().status()
        );

        assertEquals(
                3L,
                response.getBody().orderId()
        );

        assertEquals(
                "Payment declined by simulated bank",
                response.getBody().message()
        );
    }

    @Test
    void shouldGenerateTransactionId() {

        PaymentRequest request = createRequest(
                4L,
                "1000.00"
        );

        ResponseEntity<PaymentResponse> response =
                paymentController.processPayment(request);

        assertNotNull(response.getBody());

        String transactionId =
                response.getBody().transactionId();

        assertNotNull(transactionId);

        assertTrue(transactionId.startsWith("BT-"));
    }

    @Test
    void shouldGenerateDifferentTransactionIds() {

        PaymentRequest request = createRequest(
                5L,
                "3200.00"
        );

        ResponseEntity<PaymentResponse> first =
                paymentController.processPayment(request);

        ResponseEntity<PaymentResponse> second =
                paymentController.processPayment(request);

        assertNotNull(first.getBody());
        assertNotNull(second.getBody());

        assertNotEquals(
                first.getBody().transactionId(),
                second.getBody().transactionId()
        );
    }

    private PaymentRequest createRequest(
            Long orderId,
            String amount
    ) {

        PaymentRequest request = mock(PaymentRequest.class);

        when(request.orderId()).thenReturn(orderId);

        when(request.amount()).thenReturn(
                new BigDecimal(amount)
        );

        return request;
    }
}
