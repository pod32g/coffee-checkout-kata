package com.example.checkout;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private CustomerRepository customerRepository;

    @PostMapping("/api/checkout")
    public ResponseEntity<Map<String, Object>> checkout(@RequestBody CheckoutRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();

        if (request.getCustomerId() == null || request.getCustomerId().trim().equals("")) {
            body.put("error", "customerId is required");
            return ResponseEntity.badRequest().body(body);
        }
        if (request.getDeliveryTime() == null || !request.getDeliveryTime().matches("\\d\\d:\\d\\d")) {
            body.put("error", "deliveryTime must look like HH:mm");
            return ResponseEntity.badRequest().body(body);
        }
        if (request.getItems() != null) {
            for (OrderItem item : request.getItems()) {
                if (item.getQuantity() < 1) {
                    body.put("error", "Quantity must be at least 1 for " + item.getName());
                    return ResponseEntity.badRequest().body(body);
                }
            }
        }

        try {
            CheckoutResult result = checkoutService.checkout(request);
            body.put("subtotal", result.getSubtotal());
            body.put("discount", result.getDiscount());
            body.put("deliveryFee", result.getDeliveryFee());
            body.put("tax", result.getTax());
            body.put("total", result.getTotal());
            body.put("loyaltyPointsEarned", result.getLoyaltyPointsEarned());
            body.put("messages", result.getMessages());
            return ResponseEntity.ok(body);
        } catch (RuntimeException e) {
            body.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        }
    }

    @GetMapping("/api/customers/{id}")
    public ResponseEntity<Map<String, Object>> customer(@PathVariable String id) {
        Customer c = customerRepository.findById(id);
        if (c == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", c.getId());
        body.put("name", c.getName());
        body.put("tier", c.getTier());
        body.put("loyaltyPoints", c.getLoyaltyPoints());
        body.put("orders", c.getOrders());
        return ResponseEntity.ok(body);
    }
}
