package com.rbdip.bookstore.order;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderWriter {
    private final OrderRepository orders;
    private final OrderItemRepository orderItems;

    public OrderWriter(OrderRepository orders, OrderItemRepository orderItems) {
        this.orders = orders;
        this.orderItems = orderItems;
    }

    public Order save(CreateOrderRequest request, List<OrderValidator.ValidatedItem> items) {
        Order order = orders.save(new Order(
                request.customerFullName(), request.customerAddress(), request.customerPhone(), "new"));
        for (OrderValidator.ValidatedItem item : items) {
            orderItems.save(new OrderItem(
                    order.getId(), item.product(), item.quantity()));
        }
        return order;
    }
}
