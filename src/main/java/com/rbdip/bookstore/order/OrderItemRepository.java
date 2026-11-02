package com.rbdip.bookstore.order;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query("select count(i) > 0 from OrderItem i join Order o on o.id = i.orderId"
            + " where i.product.id = :productId and concat(o.customer.firstName,"
            + " case when o.customer.lastName = '' then '' else concat(' ', o.customer.lastName) end) = :name")
    boolean wasPurchasedBy(@Param("productId") Long productId, @Param("name") String customerName);
}
