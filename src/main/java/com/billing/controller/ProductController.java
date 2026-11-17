package com.billing.controller;


import com.billing.model.Product;
import com.billing.repository.ProductRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/products")
public class ProductController {
    private final ProductRepository products;

    public ProductController(ProductRepository products) {
        this.products = products;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("products", products.findAllByOrderByNameAsc());
        return "products";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("product") Product product,
                       BindingResult result, Model model, RedirectAttributes flash) {
        if (result.hasErrors()) {
            model.addAttribute("products", products.findAllByOrderByNameAsc());
            return "products";
        }
        products.save(product);
        flash.addFlashAttribute("message", product.getName() + " was added.");
        return "redirect:/products";
    }
}
