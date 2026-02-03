package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.suppliers.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;



@Repository
public interface SupplierInvoiceRepository extends JpaRepository<Invoice, Long> {
    // Standard CRUD is enough for now
}
