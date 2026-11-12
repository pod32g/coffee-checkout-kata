package com.example.checkout;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Checks every pricing rule through the HTTP API, the way a client sees it. Because these tests
 * only use the API, you can change anything inside the app and they still tell you whether the
 * behaviour stayed the same.
 *
 * <p>Seeded customers: c-regular (REGULAR, 3 past orders), c-gold (GOLD), c-platinum (PLATINUM)
 * and c-new (REGULAR, no orders yet). Only the WELCOME10 test uses c-new.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CheckoutApiTest {

    @Autowired
    private MockMvc mvc;

    private ResultActions checkout(String json) throws Exception {
        return mvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    // ------------------------------------------------------------------ prices and discounts

    @Test
    void regularCustomerPaysFullPricePlusDeliveryAndTax() throws Exception {
        checkout("""
                {"customerId": "c-regular", "distanceKm": 2, "deliveryTime": "10:00",
                 "items": [{"name": "Latte", "category": "DRINK", "price": 4.50, "quantity": 2},
                           {"name": "Croissant", "category": "FOOD", "price": 3.25, "quantity": 1}]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(12.25))
                .andExpect(jsonPath("$.discount").value(0.0))
                .andExpect(jsonPath("$.deliveryFee").value(2.99))
                .andExpect(jsonPath("$.tax").value(0.98))
                .andExpect(jsonPath("$.total").value(16.22))
                .andExpect(jsonPath("$.loyaltyPointsEarned").value(16))
                .andExpect(jsonPath("$.messages", empty()));
    }

    @Test
    void goldGetsFivePercentOffDrinksAndFoodButNotMerch() throws Exception {
        checkout("""
                {"customerId": "c-gold", "distanceKm": 2, "deliveryTime": "10:00",
                 "items": [{"name": "Latte", "category": "DRINK", "price": 4.50, "quantity": 2},
                           {"name": "Mug", "category": "MERCH", "price": 12.00, "quantity": 1}]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(21.0))
                .andExpect(jsonPath("$.discount").value(0.45))
                .andExpect(jsonPath("$.deliveryFee").value(2.99))
                .andExpect(jsonPath("$.tax").value(1.64))
                .andExpect(jsonPath("$.total").value(25.18))
                .andExpect(jsonPath("$.loyaltyPointsEarned").value(50)); // 25 x 2
    }

    @Test
    void platinumGetsTenPercentOffEverythingAndFreeDelivery() throws Exception {
        checkout("""
                {"customerId": "c-platinum", "distanceKm": 9, "deliveryTime": "18:00",
                 "items": [{"name": "Coffee beans", "category": "MERCH", "price": 16.00, "quantity": 1},
                           {"name": "Sandwich", "category": "FOOD", "price": 7.50, "quantity": 1}]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(23.5))
                .andExpect(jsonPath("$.discount").value(2.35))
                .andExpect(jsonPath("$.deliveryFee").value(0.0))
                .andExpect(jsonPath("$.tax").value(1.69))
                .andExpect(jsonPath("$.total").value(22.84))
                .andExpect(jsonPath("$.loyaltyPointsEarned").value(66)) // 22 x 3
                .andExpect(jsonPath("$.messages", empty()));
    }

    // ------------------------------------------------------------------ delivery fee

    @Test
    void deliveryIsFreeFromFortyDollars() throws Exception {
        checkout("""
                {"customerId": "c-regular", "distanceKm": 6, "deliveryTime": "12:00",
                 "items": [{"name": "Sandwich", "category": "FOOD", "price": 8.50, "quantity": 5}]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(42.5))
                .andExpect(jsonPath("$.deliveryFee").value(0.0))
                .andExpect(jsonPath("$.tax").value(3.4))
                .andExpect(jsonPath("$.total").value(45.9))
                .andExpect(jsonPath("$.loyaltyPointsEarned").value(45));
    }

    @Test
    void everyStartedKilometreAfterThreeCostsFiftyCents() throws Exception {
        checkout("""
                {"customerId": "c-regular", "distanceKm": 5.2, "deliveryTime": "10:00",
                 "items": [{"name": "Latte", "category": "DRINK", "price": 4.50, "quantity": 1}]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryFee").value(4.49)) // 2.99 + 3 x 0.50
                .andExpect(jsonPath("$.tax").value(0.36))
                .andExpect(jsonPath("$.total").value(9.35));
    }

    @Test
    void peakTimeAddsOneDollarToDelivery() throws Exception {
        checkout("""
                {"customerId": "c-regular", "distanceKm": 1, "deliveryTime": "17:30",
                 "items": [{"name": "Latte", "category": "DRINK", "price": 4.50, "quantity": 1}]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryFee").value(3.99))
                .andExpect(jsonPath("$.total").value(8.85))
                .andExpect(jsonPath("$.messages", contains("Peak time delivery surcharge applied")));
    }

    @Test
    void peakTimeEndsAtSeven() throws Exception {
        checkout("""
                {"customerId": "c-regular", "distanceKm": 1, "deliveryTime": "19:00",
                 "items": [{"name": "Latte", "category": "DRINK", "price": 4.50, "quantity": 1}]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryFee").value(2.99))
                .andExpect(jsonPath("$.messages", empty()));
    }

    // ------------------------------------------------------------------ promo codes

    @Test
    void welcome10TakesTenPercentOffAFirstOrder() throws Exception {
        checkout("""
                {"customerId": "c-new", "promoCode": "WELCOME10", "distanceKm": 2, "deliveryTime": "10:00",
                 "items": [{"name": "Sandwich", "category": "FOOD", "price": 8.50, "quantity": 2}]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(17.0))
                .andExpect(jsonPath("$.discount").value(1.7))
                .andExpect(jsonPath("$.tax").value(1.22))
                .andExpect(jsonPath("$.total").value(19.51))
                .andExpect(jsonPath("$.messages", empty()));
    }

    @Test
    void welcome10IsRefusedForReturningCustomers() throws Exception {
        checkout("""
                {"customerId": "c-regular", "promoCode": "WELCOME10", "distanceKm": 2, "deliveryTime": "10:00",
                 "items": [{"name": "Sandwich", "category": "FOOD", "price": 8.50, "quantity": 2}]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discount").value(0.0))
                .andExpect(jsonPath("$.total").value(21.35))
                .andExpect(jsonPath("$.messages", contains("WELCOME10 is only valid on your first order")));
    }

    @Test
    void save5TakesFiveDollarsOffFromThirtyDollars() throws Exception {
        checkout("""
                {"customerId": "c-regular", "promoCode": "SAVE5", "distanceKm": 2, "deliveryTime": "10:00",
                 "items": [{"name": "Sandwich", "category": "FOOD", "price": 8.50, "quantity": 4}]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(34.0))
                .andExpect(jsonPath("$.discount").value(5.0))
                .andExpect(jsonPath("$.deliveryFee").value(2.99))
                .andExpect(jsonPath("$.tax").value(2.32))
                .andExpect(jsonPath("$.total").value(34.31));
    }

    @Test
    void save5IsRefusedUnderThirtyDollars() throws Exception {
        checkout("""
                {"customerId": "c-regular", "promoCode": "SAVE5", "distanceKm": 2, "deliveryTime": "10:00",
                 "items": [{"name": "Latte", "category": "DRINK", "price": 4.50, "quantity": 1}]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discount").value(0.0))
                .andExpect(jsonPath("$.messages", contains("SAVE5 needs an order of at least $30.00")));
    }

    @Test
    void freeDeliveryCodeRemovesTheFeeEvenFarAwayAtPeakTime() throws Exception {
        checkout("""
                {"customerId": "c-regular", "promoCode": "FREEDELIVERY", "distanceKm": 8, "deliveryTime": "18:00",
                 "items": [{"name": "Latte", "category": "DRINK", "price": 4.50, "quantity": 1}]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryFee").value(0.0))
                .andExpect(jsonPath("$.total").value(4.86))
                .andExpect(jsonPath("$.messages", empty()));
    }

    @Test
    void promoCodesIgnoreCaseAndSpaces() throws Exception {
        checkout("""
                {"customerId": "c-regular", "promoCode": " save5 ", "distanceKm": 2, "deliveryTime": "10:00",
                 "items": [{"name": "Sandwich", "category": "FOOD", "price": 8.50, "quantity": 4}]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discount").value(5.0));
    }

    @Test
    void unknownPromoCodeIsIgnoredWithAMessage() throws Exception {
        checkout("""
                {"customerId": "c-regular", "promoCode": "BOGUS", "distanceKm": 2, "deliveryTime": "10:00",
                 "items": [{"name": "Latte", "category": "DRINK", "price": 4.50, "quantity": 1}]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discount").value(0.0))
                .andExpect(jsonPath("$.messages", contains("Unknown promo code: BOGUS")));
    }

    // ------------------------------------------------------------------ loyalty

    @Test
    void checkoutAddsPointsAndAnOrderToTheCustomer() throws Exception {
        String before = mvc.perform(get("/api/customers/c-gold")).andReturn().getResponse().getContentAsString();
        int pointsBefore = com.jayway.jsonpath.JsonPath.read(before, "$.loyaltyPoints");
        int ordersBefore = com.jayway.jsonpath.JsonPath.read(before, "$.orders");

        checkout("""
                {"customerId": "c-gold", "distanceKm": 2, "deliveryTime": "10:00",
                 "items": [{"name": "Latte", "category": "DRINK", "price": 4.50, "quantity": 2}]}
                """)
                .andExpect(jsonPath("$.loyaltyPointsEarned").value(24)); // floor(12.22) x 2

        mvc.perform(get("/api/customers/c-gold"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loyaltyPoints").value(pointsBefore + 24))
                .andExpect(jsonPath("$.orders").value(ordersBefore + 1));
    }

    // ------------------------------------------------------------------ bad requests

    @Test
    void unknownCustomerIsABadRequest() throws Exception {
        checkout("""
                {"customerId": "nobody", "distanceKm": 2, "deliveryTime": "10:00",
                 "items": [{"name": "Latte", "category": "DRINK", "price": 4.50, "quantity": 1}]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Customer not found: nobody"));
    }

    @Test
    void missingCustomerIdIsABadRequest() throws Exception {
        checkout("""
                {"distanceKm": 2, "deliveryTime": "10:00",
                 "items": [{"name": "Latte", "category": "DRINK", "price": 4.50, "quantity": 1}]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("customerId is required"));
    }

    @Test
    void emptyOrderIsABadRequest() throws Exception {
        checkout("""
                {"customerId": "c-regular", "distanceKm": 2, "deliveryTime": "10:00", "items": []}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Order has no items"));
    }

    @Test
    void zeroQuantityIsABadRequest() throws Exception {
        checkout("""
                {"customerId": "c-regular", "distanceKm": 2, "deliveryTime": "10:00",
                 "items": [{"name": "Latte", "category": "DRINK", "price": 4.50, "quantity": 0}]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Quantity must be at least 1 for Latte"));
    }

    @Test
    void moreThanFifteenKilometresIsOutOfRange() throws Exception {
        checkout("""
                {"customerId": "c-regular", "distanceKm": 15.1, "deliveryTime": "10:00",
                 "items": [{"name": "Latte", "category": "DRINK", "price": 4.50, "quantity": 1}]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Address is out of delivery range"));
    }

    @Test
    void badDeliveryTimeIsABadRequest() throws Exception {
        checkout("""
                {"customerId": "c-regular", "distanceKm": 2, "deliveryTime": "6pm",
                 "items": [{"name": "Latte", "category": "DRINK", "price": 4.50, "quantity": 1}]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("deliveryTime must look like HH:mm"));
    }

    @Test
    void unknownCustomerLookupIsNotFound() throws Exception {
        mvc.perform(get("/api/customers/nobody")).andExpect(status().isNotFound());
    }
}
