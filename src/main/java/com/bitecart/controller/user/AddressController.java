package com.bitecart.controller.user;

import com.bitecart.dto.AddressDTO;
import com.bitecart.dto.AddressForm;
import com.bitecart.exception.ResourceNotFoundException;
import com.bitecart.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping("/addresses")
    public String addresses(Model model) {
        model.addAttribute("addresses", addressService.getCurrentUsersAddresses());
        return "addresses";
    }

    @GetMapping("/addresses/new")
    public String newAddress(@RequestParam(defaultValue = "false") boolean returnToCheckout, Model model) {
        model.addAttribute("addressForm", new AddressForm());
        model.addAttribute("returnToCheckout", returnToCheckout);
        model.addAttribute("formTitle", "Add delivery address");
        return "address-form";
    }

    @PostMapping("/addresses")
    public String createAddress(@Valid @ModelAttribute("addressForm") AddressForm form, BindingResult result,
                                @RequestParam(defaultValue = "false") boolean returnToCheckout, Model model,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("returnToCheckout", returnToCheckout);
            model.addAttribute("formTitle", "Add delivery address");
            return "address-form";
        }
        addressService.createAddress(form);
        redirectAttributes.addFlashAttribute("addressSuccess", "Delivery address added.");
        return "redirect:" + (returnToCheckout ? "/checkout" : "/addresses");
    }

    @GetMapping("/addresses/{id}/edit")
    public String editAddress(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("addressForm", toForm(addressService.getCurrentUsersAddress(id)));
            model.addAttribute("returnToCheckout", false);
            model.addAttribute("formTitle", "Edit delivery address");
            model.addAttribute("addressId", id);
            return "address-form";
        } catch (ResourceNotFoundException exception) {
            redirectAttributes.addFlashAttribute("addressError", "That delivery address is unavailable.");
            return "redirect:/addresses";
        }
    }

    @PostMapping("/addresses/{id}")
    public String updateAddress(@PathVariable Long id, @Valid @ModelAttribute("addressForm") AddressForm form,
                                BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("returnToCheckout", false);
            model.addAttribute("formTitle", "Edit delivery address");
            model.addAttribute("addressId", id);
            return "address-form";
        }
        try {
            addressService.updateAddress(id, form);
            redirectAttributes.addFlashAttribute("addressSuccess", "Delivery address updated.");
        } catch (ResourceNotFoundException exception) {
            redirectAttributes.addFlashAttribute("addressError", "That delivery address is unavailable.");
        }
        return "redirect:/addresses";
    }

    @PostMapping("/addresses/{id}/default")
    public String setDefaultAddress(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            addressService.setDefaultAddress(id);
            redirectAttributes.addFlashAttribute("addressSuccess", "Default delivery address updated.");
        } catch (ResourceNotFoundException exception) {
            redirectAttributes.addFlashAttribute("addressError", "That delivery address is unavailable.");
        }
        return "redirect:/addresses";
    }

    @PostMapping("/addresses/{id}/delete")
    public String deleteAddress(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            addressService.deleteAddress(id);
            redirectAttributes.addFlashAttribute("addressSuccess", "Delivery address removed.");
        } catch (ResourceNotFoundException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("addressError", exception.getMessage());
        }
        return "redirect:/addresses";
    }

    private AddressForm toForm(AddressDTO address) {
        AddressForm form = new AddressForm();
        form.setAddressLine1(address.addressLine1());
        form.setAddressLine2(address.addressLine2());
        form.setCity(address.city());
        form.setState(address.state());
        form.setPostalCode(address.postalCode());
        form.setCountry(address.country());
        form.setPhone(address.phone());
        form.setDefaultAddress(address.defaultAddress());
        return form;
    }
}
