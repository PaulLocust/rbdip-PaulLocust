package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderValidator validator;
    private final OrderWriter writer;
    private final OrderNotifier notifier;
    private final PricingCalculator pricingCalculator = new PricingCalculator();

    public OrderService(OrderValidator validator, OrderWriter writer, OrderNotifier notifier) {
        this.validator = validator;
        this.writer = writer;
        this.notifier = notifier;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        List<OrderValidator.ValidatedItem> items = validator.validate(request);
        BigDecimal total = pricingCalculator.calculateOrderTotal(
                items.stream().map(OrderValidator.ValidatedItem::priceLine).toList(),
                request.customerType(), request.couponCode());
        Order order = writer.save(request, items);
        notifier.confirm(request.customerFullName(), order.getId(), total);
        return order;
    }
}
