package com.billing.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.billing.model.Customer;
import com.billing.repository.CustomerRepository;

@Controller
@RequestMapping("/customers")
public class CustomerController {
    private final CustomerRepository customers;

    public CustomerController(CustomerRepository customers) {
        this.customers = customers;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("customer", new Customer());
        model.addAttribute("customers", customers.findAllByOrderByNameAsc());
        return "customers";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("customer") Customer customer,
                       BindingResult result, Model model, RedirectAttributes flash) {
        if (result.hasErrors()) {
            model.addAttribute("customers", customers.findAllByOrderByNameAsc());
            return "customers";
        }
        customers.save(customer);
        flash.addFlashAttribute("message", customer.getName() + " was added.");
        return "redirect:/customers";
    }
}
