package com.ctms.controller;

import com.ctms.model.ShippingLine;
import com.ctms.model.Vessel;
import com.ctms.service.BerthService;
import com.ctms.service.ShippingLineService;
import com.ctms.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class ShippingLineAdminController {

    private final ShippingLineService shippingLineService;
    private final BerthService berthService;
    private final UserService userService;

    /**
     * Use Case: Manage Shipping Lines
     */
    @GetMapping("/shipping-lines")
    public String listShippingLines(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) {
            model.addAttribute("currentUser", userService.findByUsername(userDetails.getUsername()));
        }
        model.addAttribute("shippingLines", shippingLineService.getAllShippingLines());
        return "vessel/shipping-lines";
    }

    @PostMapping("/shipping-lines/create")
    public String createShippingLine(@RequestParam("code") String code,
                                     @RequestParam("name") String name,
                                     @RequestParam("email") String email,
                                     @RequestParam("phone") String phone,
                                     @RequestParam("address") String address,
                                     RedirectAttributes redirectAttributes) {
        try {
            ShippingLine line = ShippingLine.builder()
                    .code(code.trim().toUpperCase())
                    .name(name.trim())
                    .contactEmail(email)
                    .contactPhone(phone)
                    .address(address)
                    .build();
            shippingLineService.save(line);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm hãng tàu mới thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + com.ctms.util.SafeMessage.of(e));
        }
        return "redirect:/shipping-lines";
    }

    /**
     * Use Case: Manage Vessels
     */
    @GetMapping("/vessels")
    public String listVessels(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) {
            model.addAttribute("currentUser", userService.findByUsername(userDetails.getUsername()));
        }
        model.addAttribute("vessels", berthService.getAllVessels());
        model.addAttribute("shippingLines", shippingLineService.getAllShippingLines());
        return "vessel/manage-vessels";
    }

    @PostMapping("/vessels/create")
    public String createVessel(@RequestParam("name") String name,
                               @RequestParam("imoNumber") String imoNumber,
                               @RequestParam("shippingLineId") Long shippingLineId,
                               @RequestParam("capacityTeu") Integer capacityTeu,
                               @RequestParam("lengthMeters") Double lengthMeters,
                               @RequestParam("eta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime eta,
                               RedirectAttributes redirectAttributes) {
        try {
            ShippingLine line = shippingLineService.findById(shippingLineId)
                    .orElseThrow(() -> new IllegalArgumentException("Hãng tàu không hợp lệ!"));

            Vessel vessel = Vessel.builder()
                    .name(name.trim().toUpperCase())
                    .imoNumber(imoNumber.trim().toUpperCase())
                    .shippingLine(line)
                    .capacityTeu(capacityTeu)
                    .lengthMeters(lengthMeters)
                    .status(Vessel.VesselStatus.SCHEDULED)
                    .eta(eta)
                    .etd(eta.plusDays(2))
                    .build();

            berthService.saveVessel(vessel);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm thông tin tàu mới thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tạo tàu: " + com.ctms.util.SafeMessage.of(e));
        }
        return "redirect:/vessels";
    }
}
