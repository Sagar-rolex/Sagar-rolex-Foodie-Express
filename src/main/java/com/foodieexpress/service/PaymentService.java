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
import java.util.Objects;
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

    public String getKeyId() {
        return razorpayKeyId;
    }

    public boolean isConfigured() {
        return razorpayKeyId != null && !razorpayKeyId.isBlank()
                && razorpayKeySecret != null && !razorpayKeySecret.isBlank();
    }

    record PaymentDetails(String status, String orderId, String currency, long amount) {}

    /**
     * Razorpay Entity#get is generic. Keep its result in an Object-typed
     * parameter before converting so Java cannot select String.valueOf(char[])
     * and insert a String-to-char[] cast at the call site.
     */
    static String requireRazorpayString(Object value, String field) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalStateException("Razorpay returned an invalid " + field);
        }
        return text;
    }

    static long requireRazorpayAmount(Object value) {
        try {
            if (value instanceof Number number) {
                return new BigDecimal(number.toString()).longValueExact();
            }
            if (value instanceof String text && !text.isBlank()) {
                return new BigDecimal(text).longValueExact();
            }
        } catch (NumberFormatException | ArithmeticException e) {
            throw new IllegalStateException("Razorpay returned an invalid payment amount", e);
        }
        throw new IllegalStateException("Razorpay returned an invalid payment amount");
    }

    static PaymentDetails readPaymentDetails(com.razorpay.Payment payment) {
        return new PaymentDetails(
                requireRazorpayString(payment.get("status"), "payment status"),
                requireRazorpayString(payment.get("order_id"), "order ID"),
                requireRazorpayString(payment.get("currency"), "currency"),
                requireRazorpayAmount(payment.get("amount"))
        );
    }

    PaymentDetails fetchPaymentDetails(String paymentId) throws Exception {
        RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        com.razorpay.Payment payment = client.payments.fetch(paymentId);
        return readPaymentDetails(payment);
    }

    String createProviderOrder(long amountInPaise, String receipt) throws Exception {
        RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        org.json.JSONObject request = new org.json.JSONObject();
        request.put("amount", amountInPaise);
        request.put("currency", "INR");
        request.put("receipt", receipt);
        com.razorpay.Order order = client.orders.create(request);
        return requireRazorpayString(order.get("id"), "order ID");
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
                String retryOrderId = createProviderOrder(bill.getGrandTotal().movePointRight(2).longValueExact(),
                        "order_rcptid_" + localOrder.getId() + "_retry_" + System.currentTimeMillis());
                record.setRazorpayOrderId(retryOrderId);
                record.setRazorpayPaymentId(null); record.setRazorpaySignature(null);
                record.setStatus(PaymentRecord.PaymentStatus.PENDING);
                bill.setPaymentStatus(Bill.PaymentStatus.PENDING);
                billRepository.save(bill);
                if (bill.getCouponCode() != null && !bill.getCouponCode().isBlank()) {
                    couponService.reserve(bill.getCouponCode(), customer, localOrder, bill.getTotalAmount());
                }
                return paymentRecordRepository.save(record);
            }

            // Use the server-created bill; the browser never supplies payment amounts.
            BigDecimal amount = bill.getGrandTotal();
            long amountInPaise = amount.movePointRight(2).longValueExact();

            String rzpOrderId = createProviderOrder(amountInPaise, "order_rcptid_" + localOrder.getId());

            PaymentRecord record = PaymentRecord.builder()
                    .customer(customer)
                    .order(localOrder)
                    .razorpayOrderId(rzpOrderId)
                    .amount(amount)
                    .currency("INR")
                    .status(PaymentRecord.PaymentStatus.PENDING)
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
        PaymentRecord record = paymentRecordRepository.findByRazorpayOrderIdForUpdate(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("Payment record not found for Razorpay order ID: " + razorpayOrderId));

        if (customer == null || record.getCustomer() == null
                || !Objects.equals(record.getCustomer().getId(), customer.getId())) {
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
            log.warn("Could not verify Razorpay signature for local payment record {}", record.getId(), e);
            return new VerificationResult(false, "SIGNATURE_CHECK_ERROR",
                    "The payment signature could not be checked. Confirm the backend is configured with the matching Razorpay test keys, then retry.");
        }

        if (!signatureValid) {
            record.setStatus(PaymentRecord.PaymentStatus.PENDING);
            paymentRecordRepository.save(record);
            return new VerificationResult(false, "SIGNATURE_MISMATCH",
                    "Razorpay could not verify this payment signature. Confirm the backend uses the same test key pair as Checkout.");
        }

        final PaymentDetails payment;
        final long expectedAmount;
        try {
            payment = fetchPaymentDetails(razorpayPaymentId);
            Bill bill = billRepository.findByOrder(record.getOrder())
                    .orElseThrow(() -> new IllegalStateException("Bill not found for payment record " + record.getId()));
            expectedAmount = bill.getGrandTotal().movePointRight(2).longValueExact();
        } catch (Exception e) {
            record.setStatus(PaymentRecord.PaymentStatus.PENDING);
            paymentRecordRepository.save(record);
            log.warn("Could not confirm Razorpay payment state for local payment record {}", record.getId(), e);
            return new VerificationResult(false, "STATUS_CHECK_ERROR",
                    "Razorpay could not confirm the payment status. Check the payment in the Razorpay Dashboard and retry verification.");
        }

        if (!"captured".equals(payment.status())) {
            record.setStatus(PaymentRecord.PaymentStatus.PENDING);
            paymentRecordRepository.save(record);
            String message = "authorized".equals(payment.status())
                    ? "Razorpay authorized this payment but has not captured it yet. Enable automatic capture or capture it in Razorpay, then retry."
                    : "Razorpay has not marked this payment as captured yet. Check its status in the Razorpay Dashboard, then retry.";
            return new VerificationResult(false, "PAYMENT_NOT_CAPTURED", message);
        }

        boolean paymentMatches = razorpayOrderId.equals(payment.orderId())
                && "INR".equals(payment.currency())
                && expectedAmount == payment.amount();
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
        orderService.recordPaymentConfirmation(order);
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
            PaymentDetails payment = fetchPaymentDetails(paymentId);
            PaymentRecord record = paymentRecordRepository.findByRazorpayOrderIdForUpdate(payment.orderId())
                    .orElseThrow(() -> new RuntimeException("Payment attempt not found"));
            if (customer == null || record.getCustomer() == null
                    || !Objects.equals(record.getCustomer().getId(), customer.getId())) {
                throw new RuntimeException("Unauthorized payment failure update");
            }
            if (record.getStatus() == PaymentRecord.PaymentStatus.SUCCESS) return false;
            if (record.getStatus() == PaymentRecord.PaymentStatus.FAILED
                    && paymentId.equals(record.getRazorpayPaymentId())) return true;
            if (!"failed".equals(payment.status())) return false;
            record.setStatus(PaymentRecord.PaymentStatus.FAILED);
            record.setRazorpayPaymentId(paymentId);
            paymentRecordRepository.save(record);
            billRepository.findByOrder(record.getOrder()).ifPresent(bill -> {
                bill.setPaymentStatus(Bill.PaymentStatus.FAILED);
                billRepository.save(bill);
            });
            couponService.release(record.getOrder());
            return true;
        } catch (RuntimeException e) { throw e; }
        catch (Exception e) { throw new RuntimeException("Could not confirm failed payment with Razorpay", e); }
    }

}
