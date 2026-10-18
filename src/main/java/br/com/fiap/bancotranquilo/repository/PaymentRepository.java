
package br.com.fiap.bancotranquilo.repository;

import br.com.fiap.bancotranquilo.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    @Modifying
    @Query(
            value = """
                    INSERT INTO bank_payments (
                        order_id,
                        amount,
                        transaction_id,
                        status,
                        message,
                        created_at
                    )
                    VALUES (
                        :orderId,
                        :amount,
                        :transactionId,
                        :status,
                        :message,
                        CURRENT_TIMESTAMP
                    )
                    ON CONFLICT (order_id) DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertIfAbsent(
            @Param("orderId") Long orderId,
            @Param("amount") BigDecimal amount,
            @Param("transactionId") String transactionId,
            @Param("status") String status,
            @Param("message") String message
    );
}
