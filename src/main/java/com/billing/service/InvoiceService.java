package com.billing.service;

import com.billing.model.Invoice;
import com.billing.model.InvoiceItem;
import com.billing.model.Product;
import com.billing.model.Customer;
import com.billing.repository.CustomerRepository;
import com.billing.repository.InvoiceRepository;
import com.billing.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InvoiceService {
    private final CustomerRepository customerRepo;
    private final ProductRepository productRepo;
    private final InvoiceRepository invoiceRepo;
    private final BigDecimal taxRate;

    public InvoiceService(CustomerRepository customerRepo,
                          ProductRepository productRepo,
                          InvoiceRepository invoiceRepo,
                          @Value("${billing.tax-rate:0.10}") BigDecimal taxRate) {
        this.customerRepo = customerRepo;
        this.productRepo = productRepo;
        this.invoiceRepo = invoiceRepo;
        this.taxRate = taxRate;
    }

    /**
     * Creates a bill. productIds and quantities are parallel lists coming from the form rows.
     * Throws IllegalArgumentException with a message that is safe to show to the user.
     */
    @Transactional
    public Invoice create(Long customerId, List<Long> productIds, List<Integer> quantities) {
        if (customerId == null) {
            throw new IllegalArgumentException("Choose a customer.");
        }
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("That customer no longer exists."));

        // Merge duplicate products, so the same item picked twice is billed as one line.
        Map<Long, Integer> wanted = new LinkedHashMap<>();
        int rows = productIds == null ? 0 : productIds.size();
        for (int i = 0; i < rows; i++) {
            Long productId = productIds.get(i);
            if (productId == null) {
                continue; // empty row
            }
            Integer qty = (quantities != null && i < quantities.size()) ? quantities.get(i) : null;
            if (qty == null || qty < 1) {
                throw new IllegalArgumentException("Enter a quantity of 1 or more for every item.");
            }
            wanted.merge(productId, qty, Integer::sum);
        }
        if (wanted.isEmpty()) {
            throw new IllegalArgumentException("Add at least one item to the bill.");
        }

        Invoice invoice = new Invoice();
        invoice.setCustomer(customer);
        BigDecimal subtotal = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : wanted.entrySet()) {
            Product product = productRepo.findById(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("An item on this bill no longer exists."));
            int qty = entry.getValue();
            if (qty > product.getStock()) {
                throw new IllegalArgumentException("Only " + product.getStock() + " of "
                        + product.getName() + " in stock.");
            }
            product.setStock(product.getStock() - qty);

            InvoiceItem item = new InvoiceItem();
            item.setInvoice(invoice);
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setUnitPrice(product.getPrice());
            item.setQuantity(qty);
            item.setLineTotal(product.getPrice().multiply(BigDecimal.valueOf(qty)));
            invoice.getItems().add(item);
            subtotal = subtotal.add(item.getLineTotal());
        }

        BigDecimal tax = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        invoice.setSubtotal(subtotal);
        invoice.setTax(tax);
        invoice.setTotal(subtotal.add(tax));
        return invoiceRepo.save(invoice);
    }
}
