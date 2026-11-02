package com.rbdip.bookstore.order;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderQueryService {
    private final OrderRepository orders;

    public OrderQueryService(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listOrders() {
        return orders.findAllWithItems().stream().map(order -> Map.<String, Object>of(
                "id", order.getId(),
                "customerFullName", order.getCustomerFullName(),
                "status", order.getStatus(),
                "items", order.getItems().stream().map(item -> Map.of(
                        "productName", item.getProductName(), "quantity", item.getQuantity())).toList()))
                .toList();
    }
}
