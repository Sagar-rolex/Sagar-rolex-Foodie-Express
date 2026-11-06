package com.foodieexpress.service;

import com.foodieexpress.model.Customer;
import com.foodieexpress.model.User;
import com.foodieexpress.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    public Customer createProfile(User user, String fullName, String phone, String address) {
        Customer customer = new Customer();
        customer.setUser(user);
        customer.setFullName(fullName);
        customer.setPhone(phone);
        customer.setAddress(address);
        return customerRepository.save(customer);
    }

    public Optional<Customer> getByUser(User user) {
        return customerRepository.findByUser(user);
    }

    public Customer getOrCreateProfile(User user) {
        return customerRepository.findByUser(user)
                .orElseGet(() -> createProfile(user, user.getUsername(), null, null));
    }

    public Customer updateProfile(Customer customer, String fullName, String phone, String address) {
        if (fullName != null) customer.setFullName(fullName);
        if (phone != null) customer.setPhone(phone);
        if (address != null) customer.setAddress(address);
        return customerRepository.save(customer);
    }
}