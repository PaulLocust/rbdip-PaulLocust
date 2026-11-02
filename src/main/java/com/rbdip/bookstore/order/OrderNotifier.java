package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OrderNotifier {
    private static final Logger LOG = LoggerFactory.getLogger(OrderNotifier.class);

    public void confirm(String customerName, Long orderId, BigDecimal total) {
        // The training application has no mail transport; log the confirmation.
        LOG.info("[email] Dear {}, your order #{} for {} has been placed.", customerName, orderId, total);
    }
}
