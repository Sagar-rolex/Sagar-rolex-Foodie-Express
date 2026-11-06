package com.foodieexpress.service;

import com.foodieexpress.model.Customer;
import com.foodieexpress.model.PaymentRecord;
import com.foodieexpress.repository.BillRepository;
import com.foodieexpress.repository.OrderRepository;
import com.foodieexpress.repository.PaymentRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock private PaymentRecordRepository paymentRecordRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private BillRepository billRepository;
    @Mock private OrderService orderService;
    @Mock private CouponService couponService;

    @InjectMocks private PaymentService paymentService;

    @BeforeEach
    void configureTestKeys() {
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "rzp_test_unit");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "unit_test_secret");
    }

    @Test
    void invalidSignatureStaysPendingAndExplainsThatTheTestKeyPairMustMatch() {
        Customer customer = Customer.builder().id(7L).build();
        PaymentRecord record = paymentRecord(customer, PaymentRecord.PaymentStatus.CREATED);
        when(paymentRecordRepository.findByRazorpayOrderId("order_test_1")).thenReturn(Optional.of(record));

        PaymentService.VerificationResult result = paymentService.verifyPaymentSignature(
                "order_test_1", "pay_test_1", "invalid_signature", customer);

        assertThat(result.verified()).isFalse();
        assertThat(result.status()).isEqualTo("SIGNATURE_MISMATCH");
        assertThat(result.message()).contains("same test key pair");
        assertThat(record.getStatus()).isEqualTo(PaymentRecord.PaymentStatus.PENDING);
        verify(paymentRecordRepository).save(record);
        verifyNoInteractions(billRepository, orderRepository, orderService, couponService);
    }

    @Test
    void repeatingTheSameVerifiedPaymentIsIdempotent() {
        Customer customer = Customer.builder().id(7L).build();
        PaymentRecord record = paymentRecord(customer, PaymentRecord.PaymentStatus.SUCCESS);
        record.setRazorpayPaymentId("pay_test_1");
        when(paymentRecordRepository.findByRazorpayOrderId("order_test_1")).thenReturn(Optional.of(record));

        PaymentService.VerificationResult result = paymentService.verifyPaymentSignature(
                "order_test_1", "pay_test_1", "signature", customer);

        assertThat(result.verified()).isTrue();
        assertThat(result.status()).isEqualTo("SUCCESS");
        verify(paymentRecordRepository, never()).save(any(PaymentRecord.class));
        verifyNoInteractions(billRepository, orderRepository, orderService, couponService);
    }

    private PaymentRecord paymentRecord(Customer customer, PaymentRecord.PaymentStatus status) {
        return PaymentRecord.builder()
                .id(15L)
                .customer(customer)
                .razorpayOrderId("order_test_1")
                .amount(new BigDecimal("100.00"))
                .currency("INR")
                .status(status)
                .build();
    }
}
