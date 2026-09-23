package com.sliit.sparepartshub.supplier.repository;

import com.sliit.sparepartshub.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Narrow supplier-module repository. Function 5 reads Product data in order to
 * compare supplier terms and to let Admin link existing inventory products to
 * a supplier. It does not create a second Product entity or copy products into
 * Supplier records.
 */
public interface ProductRepository extends JpaRepository<Product, Integer> {

    List<Product> findAllByOrderByCategoryAscNameAsc();
}
