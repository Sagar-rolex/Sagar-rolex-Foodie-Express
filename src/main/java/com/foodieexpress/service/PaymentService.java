package com.foodieexpress.service;

import com.foodieexpress.model.*;
import com.foodieexpress.repository.*;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    public record VerificationResult(boolean verified, String status, String message) {}

    @Value("${razorpay.key.id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret:}")
    private String razorpayKeySecret;

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private BillRepository billRepository;

    private RazorpayClient razorpayClient;

    public String getKeyId() {
        return razorpayKeyId;
    }

    public boolean isConfigured() {
        return razorpayKeyId != null && !razorpayKeyId.isBlank()
                && razorpayKeySecret != null && !razorpayKeySecret.isBlank();
    }

    @Transactional
    public PaymentRecord createRazorpayOrder(Customer customer, com.foodieexpress.model.Order localOrder) {
        try {
            if (!isConfigured()) {
                throw new RuntimeException("Razorpay is not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET to enable checkout.");
            }
            Bill bill = billRepository.findByOrder(localOrder)
                    .orElseThrow(() -> new RuntimeException("Bill not found for order"));
            // Check if payment record already exists for this order
            Optional<PaymentRecord> existing = paymentRecordRepository.findByOrder(localOrder);
            if (existing.isPresent()) {
                PaymentRecord record = existing.get();
                if (record.getStatus() == PaymentRecord.PaymentStatus.SUCCESS) {
                    throw new RuntimeException("This order has already been paid");
                }
                if (record.getStatus() != PaymentRecord.PaymentStatus.FAILED) return record;
                // A provider-confirmed failed attempt can be retried with a fresh Razorpay order.
                razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
                JSONObject retryRequest = new JSONObject();
                retryRequest.put("amount", bill.getGrandTotal().movePointRight(2).longValueExact());
                retryRequest.put("currency", "INR");
                retryRequest.put("receipt", "order_rcptid_" + localOrder.getId() + "_retry_" + System.currentTimeMillis());
                com.razorpay.Order retryOrder = razorpayClient.orders.create(retryRequest);
                record.setRazorpayOrderId(retryOrder.get("id"));
                record.setRazorpayPaymentId(null); record.setRazorpaySignature(null);
                record.setStatus(PaymentRecord.PaymentStatus.CREATED);
                return paymentRecordRepository.save(record);
            }

            // Use the server-created bill; the browser never supplies payment amounts.
            BigDecimal amount = bill.getGrandTotal();
            long amountInPaise = amount.movePointRight(2).longValueExact();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "order_rcptid_" + localOrder.getId());

            razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String rzpOrderId = razorpayOrder.get("id");

            PaymentRecord record = PaymentRecord.builder()
                    .customer(customer)
                    .order(localOrder)
                    .razorpayOrderId(rzpOrderId)
                    .amount(amount)
                    .currency("INR")
                    .status(PaymentRecord.PaymentStatus.CREATED)
                    .build();

            return paymentRecordRepository.save(record);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Razorpay payment order: " + e.getMessage(), e);
        }
    }

    @Transactional
    public VerificationResult verifyPaymentSignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature, Customer customer) {
        PaymentRecord record = paymentRecordRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("Payment record not found for Razorpay order ID: " + razorpayOrderId));

        if (!record.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("Unauthorized payment verification attempt");
        }

        if (record.getStatus() == PaymentRecord.PaymentStatus.SUCCESS) {
            return razorpayPaymentId.equals(record.getRazorpayPaymentId())
                    ? new VerificationResult(true, "SUCCESS", "Payment was already verified")
                    : new VerificationResult(false, "PAYMENT_CONFLICT", "This order was already verified with a different payment");
        }
        if (!isConfigured()) {
            throw new RuntimeException("Razorpay is not configured. Payment cannot be verified.");
        }

        boolean signatureValid;
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", razorpayOrderId);
            options.put("razorpay_payment_id", razorpayPaymentId);
            options.put("razorpay_signature", razorpaySignature);
            signatureValid = Utils.verifyPaymentSignature(options, razorpayKeySecret);
        } catch (Exception e) {
            record.setStatus(PaymentRecord.PaymentStatus.PENDING);
            paymentRecordRepository.save(record);
            log.warn("Could not verify Razorpay signature for local payment record {} ({})", record.getId(), e.getClass().getSimpleName());
            return new VerificationResult(false, "SIGNATURE_CHECK_ERROR",
                    "The payment signature could not be checked. Confirm the backend is configured with the matching Razorpay test keys, then retry.");
        }

        if (!signatureValid) {
            record.setStatus(PaymentRecord.PaymentStatus.PENDING);
            paymentRecordRepository.save(record);
            return new VerificationResult(false, "SIGNATURE_MISMATCH",
                    "Razorpay could not verify this payment signature. Confirm the backend uses the same test key pair as Checkout.");
        }

        final com.razorpay.Payment razorpayPayment;
        final long expectedAmount;
        try {
            razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            razorpayPayment = razorpayClient.payments.fetch(razorpayPaymentId);
            Bill bill = billRepository.findByOrder(record.getOrder())
                    .orElseThrow(() -> new IllegalStateException("Bill not found for payment record " + record.getId()));
            expectedAmount = bill.getGrandTotal().movePointRight(2).longValueExact();
        } catch (Exception e) {
            record.setStatus(PaymentRecord.PaymentStatus.PENDING);
            paymentRecordRepository.save(record);
            log.warn("Could not confirm Razorpay payment state for local payment record {} ({})", record.getId(), e.getClass().getSimpleName());
            return new VerificationResult(false, "STATUS_CHECK_ERROR",
                    "Razorpay could not confirm the payment status. Check the payment in the Razorpay Dashboard and retry verification.");
        }

        String paymentStatus = String.valueOf(razorpayPayment.get("status"));
        if (!"captured".equals(paymentStatus)) {
            record.setStatus(PaymentRecord.PaymentStatus.PENDING);
            paymentRecordRepository.save(record);
            String message = "authorized".equals(paymentStatus)
                    ? "Razorpay authorized this payment but has not captured it yet. Enable automatic capture or capture it in Razorpay, then retry."
                    : "Razorpay has not marked this payment as captured yet. Check its status in the Razorpay Dashboard, then retry.";
            return new VerificationResult(false, "PAYMENT_NOT_CAPTURED", message);
        }

        boolean paymentMatches = razorpayOrderId.equals(String.valueOf(razorpayPayment.get("order_id")))
                && "INR".equals(String.valueOf(razorpayPayment.get("currency")))
                && expectedAmount == Long.parseLong(String.valueOf(razorpayPayment.get("amount")));
        if (!paymentMatches) {
            record.setStatus(PaymentRecord.PaymentStatus.PENDING);
            paymentRecordRepository.save(record);
            log.warn("Razorpay payment details did not match local payment record {}", record.getId());
            return new VerificationResult(false, "PAYMENT_DETAILS_MISMATCH",
                    "Razorpay payment details do not match this order. The order remains unpaid; contact support with the order number.");
        }

        record.setRazorpayPaymentId(razorpayPaymentId);
        record.setRazorpaySignature(razorpaySignature);
        record.setStatus(PaymentRecord.PaymentStatus.SUCCESS);
        paymentRecordRepository.save(record);

        // Update associated order status to CONFIRMED
        com.foodieexpress.model.Order order = record.getOrder();
        order.setStatus(com.foodieexpress.model.Order.OrderStatus.CONFIRMED);
        orderRepository.save(order);
        couponService.complete(order);

        // Update associated bill to PAID
        Bill bill = billRepository.findByOrder(order).orElse(null);
        if (bill != null) {
            bill.setPaymentStatus(Bill.PaymentStatus.PAID);
            billRepository.save(bill);
        }

        orderService.clearOrderedItemsFromCart(customer, order);

        return new VerificationResult(true, "SUCCESS", "Payment verified successfully");
    }

    @Autowired
    private OrderService orderService;

    @Autowired private CouponService couponService;

    @Transactional
    public boolean recordFailedPayment(Customer customer, String paymentId) {
        if (!isConfigured()) throw new RuntimeException("Razorpay is not configured");
        try {
            razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            com.razorpay.Payment payment = razorpayClient.payments.fetch(paymentId);
            String rzpOrderId = String.valueOf(payment.get("order_id"));
            PaymentRecord record = paymentRecordRepository.findByRazorpayOrderId(rzpOrderId)
                    .orElseThrow(() -> new RuntimeException("Payment attempt not found"));
            if (!record.getCustomer().getId().equals(customer.getId())) throw new RuntimeException("Unauthorized payment failure update");
            if (!"failed".equals(String.valueOf(payment.get("status")))) return false;
            record.setStatus(PaymentRecord.PaymentStatus.FAILED);
            record.setRazorpayPaymentId(paymentId);
            paymentRecordRepository.save(record);
            couponService.release(record.getOrder());
            return true;
        } catch (RuntimeException e) { throw e; }
        catch (Exception e) { throw new RuntimeException("Could not confirm failed payment with Razorpay", e); }
    }

}
