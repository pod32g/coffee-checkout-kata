package com.example.checkout;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    @Autowired
    private CustomerRepository customerRepository;

    // TODO: this method is getting big, clean it up some day
    public CheckoutResult checkout(CheckoutRequest req) {
        CheckoutResult r = new CheckoutResult();

        Customer c = customerRepository.findById(req.getCustomerId());
        if (c == null) {
            throw new RuntimeException("Customer not found: " + req.getCustomerId());
        }
        if (req.getItems() == null || req.getItems().size() == 0) {
            throw new RuntimeException("Order has no items");
        }
        if (req.getDistanceKm() > 15) {
            throw new RuntimeException("Address is out of delivery range");
        }

        // work out the subtotal and the tier discount
        double sub = 0;
        double d = 0;
        for (int i = 0; i < req.getItems().size(); i++) {
            OrderItem item = req.getItems().get(i);
            double line = item.getPrice() * item.getQuantity();
            sub = sub + line;
            if (c.getTier().equals("GOLD")) {
                // gold gets 5% off drinks and food, but not merch
                if (item.getCategory().equals("DRINK") || item.getCategory().equals("FOOD")) {
                    d = d + line * 0.05;
                }
            } else if (c.getTier().equals("PLATINUM")) {
                d = d + line * 0.1;
            }
        }
        sub = Math.round(sub * 100.0) / 100.0;

        // promo codes
        boolean flag = false;
        if (req.getPromoCode() != null && !req.getPromoCode().equals("")) {
            String code = req.getPromoCode().toUpperCase().trim();
            if (code.equals("WELCOME10")) {
                if (c.getOrders() == 0) {
                    d = d + (sub - d) * 0.1;
                } else {
                    r.getMessages().add("WELCOME10 is only valid on your first order");
                }
            } else if (code.equals("SAVE5")) {
                if (sub >= 30) {
                    d = d + 5;
                } else {
                    r.getMessages().add("SAVE5 needs an order of at least $30.00");
                }
            } else if (code.equals("FREEDELIVERY")) {
                flag = true;
            } else {
                r.getMessages().add("Unknown promo code: " + req.getPromoCode());
            }
        }
        d = Math.round(d * 100.0) / 100.0;
        double tmp = sub - d;

        // delivery fee
        double fee = 0;
        if (!flag) {
            if (!c.getTier().equals("PLATINUM")) {
                if (tmp < 40) {
                    fee = 2.99;
                    if (req.getDistanceKm() > 3) {
                        fee = fee + Math.ceil(req.getDistanceKm() - 3) * 0.5;
                    }
                    int hour = Integer.parseInt(req.getDeliveryTime().substring(0, 2));
                    if (hour >= 17 && hour < 19) {
                        fee = fee + 1.0;
                        r.getMessages().add("Peak time delivery surcharge applied");
                    }
                }
            }
        }
        fee = Math.round(fee * 100.0) / 100.0;

        // tax is 8% but not on delivery
        double tax = Math.round(tmp * 0.08 * 100.0) / 100.0;
        double total = Math.round((tmp + fee + tax) * 100.0) / 100.0;

        // loyalty points: 1 per dollar, gold x2, platinum x3
        int pts = (int) Math.floor(total);
        if (c.getTier().equals("GOLD")) {
            pts = pts * 2;
        }
        if (c.getTier().equals("PLATINUM")) {
            pts = pts * 3;
        }
        c.setLoyaltyPoints(c.getLoyaltyPoints() + pts);
        c.setOrders(c.getOrders() + 1);
        customerRepository.save(c);

        r.setSubtotal(sub);
        r.setDiscount(d);
        r.setDeliveryFee(fee);
        r.setTax(tax);
        r.setTotal(total);
        r.setLoyaltyPointsEarned(pts);
        return r;
    }

    // old delivery fee, before the peak time surcharge. not used anymore?
    private double calcDeliveryOld(double distance) {
        double fee = 2.99;
        if (distance > 3) {
            fee = fee + Math.ceil(distance - 3) * 0.5;
        }
        return fee;
    }
}
