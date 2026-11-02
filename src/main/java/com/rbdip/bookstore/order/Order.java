package com.rbdip.bookstore.order;

import com.rbdip.bookstore.customer.Customer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(cascade = CascadeType.PERSIST, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Order() {
        // for JPA
    }

    public Order(String customerFullName, String customerAddress, String customerPhone, String status) {
        this.customer = new Customer(customerFullName, customerAddress, customerPhone);
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public List<OrderItem> getItems() {
        return List.copyOf(items);
    }

    public String getCustomerFullName() {
        return customer.getFullName();
    }

    public String getCustomerAddress() {
        return customer.getAddress();
    }

    public String getCustomerPhone() {
        return customer.getPhone();
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
