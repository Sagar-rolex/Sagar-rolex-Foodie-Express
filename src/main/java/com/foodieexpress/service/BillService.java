package com.foodieexpress.service;

import com.foodieexpress.model.Bill;
import com.foodieexpress.model.Customer;
import com.foodieexpress.model.Order;
import com.foodieexpress.repository.BillRepository;
import com.foodieexpress.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BillService {

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private OrderRepository orderRepository;

    public Optional<Bill> getBillByOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return billRepository.findByOrder(order);
    }

    public Bill getBillById(Long billId) {
        return billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));
    }

    public List<Bill> getCustomerBills(Customer customer) {
        List<Order> orders = orderRepository.findByCustomer(customer);
        return orders.stream()
                .map(order -> billRepository.findByOrder(order).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public Bill markAsFailed(Long billId) {
        Bill bill = getBillById(billId);
        bill.setPaymentStatus(Bill.PaymentStatus.FAILED);
        return billRepository.save(bill);
    }
}
