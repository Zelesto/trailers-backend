// src/main/java/com/pgsa/trailers/service/CustomerService.java
package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.CustomerRequestDTO;
import com.pgsa.trailers.dto.CustomerResponseDTO;
import com.pgsa.trailers.entity.ops.Customer;
import com.pgsa.trailers.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerResponseDTO createCustomer(CustomerRequestDTO request) {
        // Check if customer code already exists
        if (request.getCustomerCode() != null && 
            customerRepository.findByCustomerCode(request.getCustomerCode()).isPresent()) {
            throw new RuntimeException("Customer code already exists: " + request.getCustomerCode());
        }

        Customer customer = Customer.builder()
                .customerCode(request.getCustomerCode())
                .name(request.getName())
                .registrationNumber(request.getRegistrationNumber())
                .vatNumber(request.getVatNumber())
                .email(request.getEmail())
                .phone(request.getPhone())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .province(request.getProvince())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .contactPerson(request.getContactPerson())
                .contactPhone(request.getContactPhone())
                .contactEmail(request.getContactEmail())
                .paymentTerms(request.getPaymentTerms())
                .creditLimit(request.getCreditLimit())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .notes(request.getNotes())
                .build();

        Customer saved = customerRepository.save(customer);
        log.info("Created customer with ID: {}", saved.getId());
        return mapToResponseDTO(saved);
    }

    public CustomerResponseDTO updateCustomer(Long id, CustomerRequestDTO request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + id));

        // Check if customer code is being changed and if it already exists
        if (request.getCustomerCode() != null && 
            !customer.getCustomerCode().equals(request.getCustomerCode()) &&
            customerRepository.findByCustomerCode(request.getCustomerCode()).isPresent()) {
            throw new RuntimeException("Customer code already exists: " + request.getCustomerCode());
        }

        customer.setCustomerCode(request.getCustomerCode());
        customer.setName(request.getName());
        customer.setRegistrationNumber(request.getRegistrationNumber());
        customer.setVatNumber(request.getVatNumber());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setAddressLine1(request.getAddressLine1());
        customer.setAddressLine2(request.getAddressLine2());
        customer.setCity(request.getCity());
        customer.setProvince(request.getProvince());
        customer.setPostalCode(request.getPostalCode());
        customer.setCountry(request.getCountry());
        customer.setContactPerson(request.getContactPerson());
        customer.setContactPhone(request.getContactPhone());
        customer.setContactEmail(request.getContactEmail());
        customer.setPaymentTerms(request.getPaymentTerms());
        customer.setCreditLimit(request.getCreditLimit());
        customer.setIsActive(request.getIsActive());
        customer.setNotes(request.getNotes());

        Customer updated = customerRepository.save(customer);
        log.info("Updated customer with ID: {}", updated.getId());
        return mapToResponseDTO(updated);
    }

    @Transactional(readOnly = true)
    public CustomerResponseDTO getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + id));
        return mapToResponseDTO(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponseDTO getCustomerByCode(String customerCode) {
        Customer customer = customerRepository.findByCustomerCode(customerCode)
                .orElseThrow(() -> new RuntimeException("Customer not found with code: " + customerCode));
        return mapToResponseDTO(customer);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponseDTO> getAllCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable)
                .map(this::mapToResponseDTO);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponseDTO> searchCustomers(String search, Pageable pageable) {
        return customerRepository.searchCustomers(search, pageable)
                .map(this::mapToResponseDTO);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponseDTO> getActiveCustomers() {
        return customerRepository.findByIsActiveTrue()
                .stream()
                .map(this::mapToResponseDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    public void deleteCustomer(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new RuntimeException("Customer not found with ID: " + id);
        }
        customerRepository.deleteById(id);
        log.info("Deleted customer with ID: {}", id);
    }

    private CustomerResponseDTO mapToResponseDTO(Customer customer) {
        Long tripCount = customerRepository.countTripsByCustomer(customer.getId());
        Double totalSpent = customerRepository.getTotalSpentByCustomer(customer.getId());

        return CustomerResponseDTO.builder()
                .id(customer.getId())
                .customerCode(customer.getCustomerCode())
                .name(customer.getName())
                .registrationNumber(customer.getRegistrationNumber())
                .vatNumber(customer.getVatNumber())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .addressLine1(customer.getAddressLine1())
                .addressLine2(customer.getAddressLine2())
                .city(customer.getCity())
                .province(customer.getProvince())
                .postalCode(customer.getPostalCode())
                .country(customer.getCountry())
                .contactPerson(customer.getContactPerson())
                .contactPhone(customer.getContactPhone())
                .contactEmail(customer.getContactEmail())
                .paymentTerms(customer.getPaymentTerms())
                .creditLimit(customer.getCreditLimit())
                .isActive(customer.getIsActive())
                .notes(customer.getNotes())
                .createdAt(customer.getCreatedAt())
                .createdBy(customer.getCreatedBy())
                .updatedAt(customer.getUpdatedAt())
                .updatedBy(customer.getUpdatedBy())
                .tripCount(tripCount != null ? tripCount.intValue() : 0)
                .totalSpent(BigDecimal.valueOf(totalSpent != null ? totalSpent : 0))
                .build();
    }
}
