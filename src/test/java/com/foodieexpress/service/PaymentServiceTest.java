package com.foodieexpress.service;

import com.foodieexpress.model.Customer;
import com.foodieexpress.model.Bill;
import com.foodieexpress.model.Order;
import com.foodieexpress.model.PaymentRecord;
import com.foodieexpress.repository.BillRepository;
import com.foodieexpress.repository.OrderRepository;
import com.foodieexpress.repository.PaymentRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

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

    @Spy @InjectMocks private PaymentService paymentService;

    @BeforeEach
    void configureTestKeys() {
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "rzp_test_unit");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "unit_test_secret");
    }

    @Test
    void invalidSignatureStaysPendingAndExplainsThatTheTestKeyPairMustMatch() {
        Customer customer = Customer.builder().id(7L).build();
        PaymentRecord record = paymentRecord(customer, PaymentRecord.PaymentStatus.CREATED);
        when(paymentRecordRepository.findByRazorpayOrderIdForUpdate("order_test_1")).thenReturn(Optional.of(record));

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
        when(paymentRecordRepository.findByRazorpayOrderIdForUpdate("order_test_1")).thenReturn(Optional.of(record));

        PaymentService.VerificationResult result = paymentService.verifyPaymentSignature(
                "order_test_1", "pay_test_1", "signature", customer);

        assertThat(result.verified()).isTrue();
        assertThat(result.status()).isEqualTo("SUCCESS");
        verify(paymentRecordRepository, never()).save(any(PaymentRecord.class));
        verifyNoInteractions(billRepository, orderRepository, orderService, couponService);
    }

    @Test
    void readsRazorpayGenericEntityFieldsAsStringsWithoutCharArrayCast() {
        com.razorpay.Payment sdkPayment = mock(com.razorpay.Payment.class);
        when(sdkPayment.get("status")).thenReturn("captured");
        when(sdkPayment.get("order_id")).thenReturn("order_test_1");
        when(sdkPayment.get("currency")).thenReturn("INR");
        when(sdkPayment.get("amount")).thenReturn(10000);

        PaymentService.PaymentDetails details = PaymentService.readPaymentDetails(sdkPayment);

        assertThat(details.status()).isEqualTo("captured");
        assertThat(details.orderId()).isEqualTo("order_test_1");
        assertThat(details.currency()).isEqualTo("INR");
        assertThat(details.amount()).isEqualTo(10000L);
    }

    @Test
    void capturedPaymentConfirmsOrderAndBillAndClearsCartOnlyAfterVerification() throws Exception {
        Customer customer = Customer.builder().id(7L).build();
        Order order = Order.builder().id(31L).customer(customer).status(Order.OrderStatus.PLACED).build();
        PaymentRecord record = paymentRecord(customer, PaymentRecord.PaymentStatus.PENDING);
        record.setOrder(order);
        Bill bill = Bill.builder().order(order).grandTotal(new BigDecimal("100.00"))
                .paymentStatus(Bill.PaymentStatus.PENDING).build();
        when(paymentRecordRepository.findByRazorpayOrderIdForUpdate("order_test_1")).thenReturn(Optional.of(record));
        when(billRepository.findByOrder(order)).thenReturn(Optional.of(bill));
        when(paymentService.isConfigured()).thenReturn(true);
        doReturn(new PaymentService.PaymentDetails("captured", "order_test_1", "INR", 10000))
                .when(paymentService).fetchPaymentDetails("pay_test_1");

        PaymentService.VerificationResult result = paymentService.verifyPaymentSignature(
                "order_test_1", "pay_test_1", signature("order_test_1", "pay_test_1"), customer);

        assertThat(result.verified()).isTrue();
        assertThat(record.getStatus()).isEqualTo(PaymentRecord.PaymentStatus.SUCCESS);
        assertThat(record.getRazorpayPaymentId()).isEqualTo("pay_test_1");
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
        assertThat(bill.getPaymentStatus()).isEqualTo(Bill.PaymentStatus.PAID);
        verify(orderService).clearOrderedItemsFromCart(customer, order);
        verify(couponService).complete(order);
    }

    @Test
    void amountMismatchNeverMarksPaymentSuccessfulOrClearsCart() throws Exception {
        Customer customer = Customer.builder().id(7L).build();
        Order order = Order.builder().id(31L).customer(customer).status(Order.OrderStatus.PLACED).build();
        PaymentRecord record = paymentRecord(customer, PaymentRecord.PaymentStatus.PENDING);
        record.setOrder(order);
        when(paymentRecordRepository.findByRazorpayOrderIdForUpdate("order_test_1")).thenReturn(Optional.of(record));
        when(billRepository.findByOrder(order)).thenReturn(Optional.of(Bill.builder().order(order)
                .grandTotal(new BigDecimal("100.00")).paymentStatus(Bill.PaymentStatus.PENDING).build()));
        when(paymentService.isConfigured()).thenReturn(true);
        doReturn(new PaymentService.PaymentDetails("captured", "order_test_1", "INR", 1))
                .when(paymentService).fetchPaymentDetails("pay_test_1");

        PaymentService.VerificationResult result = paymentService.verifyPaymentSignature(
                "order_test_1", "pay_test_1", signature("order_test_1", "pay_test_1"), customer);

        assertThat(result.verified()).isFalse();
        assertThat(result.status()).isEqualTo("PAYMENT_DETAILS_MISMATCH");
        assertThat(record.getStatus()).isEqualTo(PaymentRecord.PaymentStatus.PENDING);
        verifyNoInteractions(orderService, couponService);
    }

    @Test
    void providerConfirmedFailureMarksPaymentAndBillFailedAndIsIdempotent() throws Exception {
        Customer customer = Customer.builder().id(7L).build();
        Order order = Order.builder().id(31L).customer(customer).status(Order.OrderStatus.PLACED).build();
        PaymentRecord record = paymentRecord(customer, PaymentRecord.PaymentStatus.PENDING);
        record.setOrder(order);
        Bill bill = Bill.builder().order(order).grandTotal(new BigDecimal("100.00"))
                .paymentStatus(Bill.PaymentStatus.PENDING).build();
        when(paymentService.isConfigured()).thenReturn(true);
        doReturn(new PaymentService.PaymentDetails("failed", "order_test_1", "INR", 10000))
                .when(paymentService).fetchPaymentDetails("pay_test_1");
        when(paymentRecordRepository.findByRazorpayOrderIdForUpdate("order_test_1")).thenReturn(Optional.of(record));
        when(billRepository.findByOrder(order)).thenReturn(Optional.of(bill));

        assertThat(paymentService.recordFailedPayment(customer, "pay_test_1")).isTrue();
        assertThat(paymentService.recordFailedPayment(customer, "pay_test_1")).isTrue();

        assertThat(record.getStatus()).isEqualTo(PaymentRecord.PaymentStatus.FAILED);
        assertThat(bill.getPaymentStatus()).isEqualTo(Bill.PaymentStatus.FAILED);
        verify(couponService, times(1)).release(order);
    }

    @Test
    void retryAfterConfirmedFailureCreatesFreshProviderOrderAndResetsPendingState() throws Exception {
        Customer customer = Customer.builder().id(7L).build();
        Order order = Order.builder().id(31L).customer(customer).status(Order.OrderStatus.PLACED).build();
        PaymentRecord record = paymentRecord(customer, PaymentRecord.PaymentStatus.FAILED);
        record.setOrder(order);
        Bill bill = Bill.builder().order(order).totalAmount(new BigDecimal("100.00"))
                .grandTotal(new BigDecimal("110.00")).paymentStatus(Bill.PaymentStatus.FAILED).build();
        when(paymentService.isConfigured()).thenReturn(true);
        when(paymentRecordRepository.findByOrder(order)).thenReturn(Optional.of(record));
        when(paymentRecordRepository.save(record)).thenReturn(record);
        when(billRepository.findByOrder(order)).thenReturn(Optional.of(bill));
        doReturn("order_test_retry").when(paymentService).createProviderOrder(eq(11000L), anyString());

        PaymentRecord result = paymentService.createRazorpayOrder(customer, order);

        assertThat(result.getRazorpayOrderId()).isEqualTo("order_test_retry");
        assertThat(result.getRazorpayPaymentId()).isNull();
        assertThat(result.getStatus()).isEqualTo(PaymentRecord.PaymentStatus.PENDING);
        assertThat(bill.getPaymentStatus()).isEqualTo(Bill.PaymentStatus.PENDING);
    }

    private String signature(String orderId, String paymentId) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("unit_test_secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8)));
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
