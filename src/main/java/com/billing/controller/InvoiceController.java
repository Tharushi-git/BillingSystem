package com.billing.controller;

import com.billing.model.Invoice;
import com.billing.repository.CustomerRepository;
import com.billing.repository.InvoiceRepository;
import com.billing.repository.ProductRepository;
import com.billing.service.EmailService;
import com.billing.service.InvoiceService;
import com.billing.service.PdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/invoices")
public class InvoiceController {
    private final InvoiceRepository invoices;
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final InvoiceService invoiceService;
    private final PdfService pdfService;
    private final EmailService emailService;

    public InvoiceController(InvoiceRepository invoices, CustomerRepository customers,
                             ProductRepository products, InvoiceService invoiceService,
                             PdfService pdfService, EmailService emailService) {
        this.invoices = invoices;
        this.customers = customers;
        this.products = products;
        this.invoiceService = invoiceService;
        this.pdfService = pdfService;
        this.emailService = emailService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("invoices", invoices.findAllByOrderByCreatedAtDesc());
        return "invoices";
    }

    @GetMapping("/new")
    public String form(Model model) {
        fillForm(model);
        return "invoice-form";
    }

    @PostMapping
    public String create(@RequestParam(required = false) Long customerId,
                         @RequestParam(required = false) List<Long> productId,
                         @RequestParam(required = false) List<Integer> quantity,
                         Model model) {
        try {
            Invoice invoice = invoiceService.create(customerId, productId, quantity);
            return "redirect:/invoices/" + invoice.getId();
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            fillForm(model);
            return "invoice-form";
        }
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("invoice", invoices.findById(id).orElseThrow());
        return "invoice-view";
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        Invoice invoice = invoices.findById(id).orElseThrow();
        byte[] pdf = pdfService.generate(invoice);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/{id}/email")
    public String email(@PathVariable Long id, RedirectAttributes flash) {
        Invoice invoice = invoices.findById(id).orElseThrow();
        try {
            emailService.sendInvoice(invoice, pdfService.generate(invoice));
            invoice.setEmailedAt(LocalDateTime.now());
            invoices.save(invoice);
            flash.addFlashAttribute("message", "Bill sent to " + invoice.getCustomer().getEmail() + ".");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "The email was not sent: " + e.getMessage());
        }
        return "redirect:/invoices/" + id;
    }

    private void fillForm(Model model) {
        model.addAttribute("customers", customers.findAllByOrderByNameAsc());
        model.addAttribute("products", products.findAllByOrderByNameAsc());
    }
}
