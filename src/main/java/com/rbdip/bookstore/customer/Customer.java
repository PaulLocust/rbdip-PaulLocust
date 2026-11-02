package com.rbdip.bookstore.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "first_name", nullable = false)
    private String firstName;
    @Column(name = "last_name", nullable = false)
    private String lastName;
    private String address;
    private String phone;

    protected Customer() {
        // for JPA
    }

    public Customer(String fullName, String address, String phone) {
        int separator = fullName.indexOf(' ');
        boolean singlePart = separator < 0 || separator == fullName.length() - 1;
        this.firstName = singlePart ? fullName : fullName.substring(0, separator);
        this.lastName = singlePart ? "" : fullName.substring(separator + 1);
        this.address = address;
        this.phone = phone;
    }

    public String getFullName() {
        return firstName + (lastName.isEmpty() ? "" : " " + lastName);
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }
}
