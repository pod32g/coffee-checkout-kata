package com.example.checkout;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/** Keeps customers in memory. A real app would use a database. */
@Repository
public class CustomerRepository {

    private final Map<String, Customer> customers = new ConcurrentHashMap<>();

    public CustomerRepository() {
        save(new Customer("c-regular", "Ana", "REGULAR", 120, 3));
        save(new Customer("c-gold", "Ben", "GOLD", 840, 27));
        save(new Customer("c-platinum", "Cleo", "PLATINUM", 5210, 112));
        save(new Customer("c-new", "Dana", "REGULAR", 0, 0));
    }

    public Customer findById(String id) {
        return customers.get(id);
    }

    public void save(Customer customer) {
        customers.put(customer.getId(), customer);
    }
}
