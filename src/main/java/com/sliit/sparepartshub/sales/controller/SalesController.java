package com.sliit.sparepartshub.sales.controller;

import com.sliit.sparepartshub.entity.Product;
import com.sliit.sparepartshub.entity.Sale;
import com.sliit.sparepartshub.sales.dto.SalesCart;
import com.sliit.sparepartshub.sales.service.SalesService;
import com.sliit.sparepartshub.security.CustomUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/sales")
@SessionAttributes("cart")
public class SalesController {

    private final SalesService service;

    public SalesController(SalesService service) {
        this.service = service;
    }

    @ModelAttribute("cart")
    public SalesCart cart() {
        return new SalesCart();
    }

    @GetMapping
    public String index(@RequestParam(required = false) String q,
                        @RequestParam(required = false) String category,
                        @RequestParam(required = false) String brand,
                        @ModelAttribute("cart") SalesCart cart,
                        Model model) {
        List<Product> visibleProducts = service.search(q, category, brand);

        model.addAttribute("products", visibleProducts);
        model.addAttribute("categories", service.categories());
        model.addAttribute("brands", service.brands());
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        model.addAttribute("brand", brand);
        model.addAttribute("conflicts", service.compatibility(cart));
        model.addAttribute("serialAvailability", service.serialAvailability(visibleProducts));
        model.addAttribute("serialOptions", service.availableSerialsForCart(cart));

        return "sales/index";
    }

    @PostMapping("/cart/add")
    public String add(@RequestParam Integer productId,
                      @RequestParam(defaultValue = "1") Integer quantity,
                      @ModelAttribute("cart") SalesCart cart,
                      RedirectAttributes redirectAttributes) {
        try {
            service.addToCart(cart, productId, quantity);
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/sales";
    }

    @PostMapping("/cart/update")
    public String update(@RequestParam int index,
                         @RequestParam int quantity,
                         @RequestParam(defaultValue = "0") BigDecimal discountAmount,
                         @RequestParam(required = false) String discountReason,
                         @ModelAttribute("cart") SalesCart cart,
                         RedirectAttributes redirectAttributes) {
        try {
            service.updateLine(cart, index, quantity, discountAmount, discountReason);
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Cart updated. Re-select serial numbers if the quantity changed."
            );
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/sales";
    }

    @PostMapping("/cart/serials")
    public String serials(@RequestParam int index,
                          @RequestParam(required = false) List<Integer> serialIds,
                          @ModelAttribute("cart") SalesCart cart,
                          RedirectAttributes redirectAttributes) {
        try {
            service.selectSerials(cart, index, serialIds);
            redirectAttributes.addFlashAttribute("success", "Serial numbers selected.");
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/sales";
    }

    @PostMapping("/cart/remove")
    public String remove(@RequestParam int index,
                         @ModelAttribute("cart") SalesCart cart,
                         RedirectAttributes redirectAttributes) {
        if (index >= 0 && index < cart.getLines().size()) {
            cart.getLines().remove(index);
            redirectAttributes.addFlashAttribute("success", "Item removed from cart.");
        }
        return "redirect:/sales";
    }

    @PostMapping("/cart/clear")
    public String clear(@ModelAttribute("cart") SalesCart cart,
                        RedirectAttributes redirectAttributes) {
        cart.clear();
        redirectAttributes.addFlashAttribute("success", "Cart cleared.");
        return "redirect:/sales";
    }

    @PostMapping("/checkout")
    public String checkout(@RequestParam(required = false) String compatibilityOverrideReason,
                           @RequestParam(defaultValue = "false") boolean paymentConfirmed,
                           @ModelAttribute("cart") SalesCart cart,
                           @AuthenticationPrincipal CustomUserPrincipal principal,
                           RedirectAttributes redirectAttributes) {
        try {
            Sale sale = service.checkout(
                    cart,
                    principal.getUser(),
                    compatibilityOverrideReason,
                    paymentConfirmed
            );

            // Do not clear the session cart inside the @Transactional method.
            // Reaching this line means the transaction committed successfully.
            cart.clear();

            redirectAttributes.addFlashAttribute("success", "Sale completed successfully.");
            return "redirect:/sales/receipt/" + sale.getSaleId();
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/sales";
        }
    }

    @GetMapping("/receipt/{id}")
    public String receipt(@PathVariable Integer id, Model model) {
        var items = service.items(id);
        model.addAttribute("sale", service.getSale(id));
        model.addAttribute("items", items);
        model.addAttribute("receiptSubtotal", service.receiptSubtotal(items));
        model.addAttribute("receiptDiscount", service.receiptDiscount(items));
        model.addAttribute("ticket", service.ticketForSale(id));
        model.addAttribute("serialsByProduct", service.serialValuesForSale(id));
        return "sales/receipt";
    }
}
