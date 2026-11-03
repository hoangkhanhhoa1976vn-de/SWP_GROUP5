package com.ctms.controller;

import com.ctms.model.Vessel;
import com.ctms.service.BerthService;
import com.ctms.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/berths")
@RequiredArgsConstructor
public class BerthController {

    private final BerthService berthService;
    private final UserService userService;

    @GetMapping
    public String berthManagement(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) {
            model.addAttribute("currentUser", userService.findByUsername(userDetails.getUsername()));
        }
        model.addAttribute("berths", berthService.getAllBerths());
        model.addAttribute("vessels", berthService.getAllVessels());
        model.addAttribute("scheduledVessels", berthService.getAllVessels().stream()
                .filter(v -> v.getStatus() == Vessel.VesselStatus.SCHEDULED)
                .toList());
        return "vessel/berth-overview";
    }

    /**
     * Use Case: Assign Vessel to Berth
     * <<include>> Update Berth / Vessel Status
     */
    @PostMapping("/assign")
    public String assignVesselToBerth(@RequestParam("vesselId") Long vesselId,
                                      @RequestParam("berthId") Long berthId,
                                      RedirectAttributes redirectAttributes) {
        try {
            berthService.assignVesselToBerth(vesselId, berthId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Phân bổ tàu vào cầu bến thành công! Trạng thái cầu bến & tàu đã tự động cập nhật.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi phân bổ bến: " + com.ctms.util.SafeMessage.of(e));
        }
        return "redirect:/berths";
    }

    @PostMapping("/depart")
    public String departVessel(@RequestParam("vesselId") Long vesselId,
                               RedirectAttributes redirectAttributes) {
        try {
            berthService.departVessel(vesselId);
            redirectAttributes.addFlashAttribute("successMessage", "Tàu đã rời bến, giải phóng cầu cảng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + com.ctms.util.SafeMessage.of(e));
        }
        return "redirect:/berths";
    }
}
