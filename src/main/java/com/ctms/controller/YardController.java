package com.ctms.controller;

import com.ctms.model.Container;
import com.ctms.model.User;
import com.ctms.model.YardSlot;
import com.ctms.service.ContainerService;
import com.ctms.service.UserService;
import com.ctms.service.YardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/yard")
public class YardController {

    private final YardService yardService;
    private final ContainerService containerService;
    private final UserService userService;

    public YardController(YardService yardService, ContainerService containerService, UserService userService) {
        this.yardService = yardService;
        this.containerService = containerService;
        this.userService = userService;
    }

    @GetMapping
    public String yardOverview(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) {
            model.addAttribute("currentUser", userService.findByUsername(userDetails.getUsername()));
        }

        model.addAttribute("blockASlots", yardService.getSlotsByBlock(YardSlot.YardBlock.BLOCK_A));
        model.addAttribute("blockBSlots", yardService.getSlotsByBlock(YardSlot.YardBlock.BLOCK_B));
        model.addAttribute("blockCSlots", yardService.getSlotsByBlock(YardSlot.YardBlock.BLOCK_C));
        model.addAttribute("occupiedCount", yardService.getOccupiedCount());
        model.addAttribute("totalCount", yardService.getTotalCount());

        // Lấy danh sách container đang chờ hạ bãi (GATE_IN hoặc EXPECTED chưa xếp ô; loại bỏ cont đã GATE_OUT)
        List<Container> waitingContainers = containerService.findAll().stream()
                .filter(c -> c.getCurrentStatus() != Container.TrackingStatus.GATE_OUT
                        && (c.getCurrentStatus() == Container.TrackingStatus.GATE_IN || c.getCurrentSlot() == null))
                .toList();
        model.addAttribute("waitingContainers", waitingContainers);

        // Lấy danh sách các ô trống để làm dropdown chuyển bãi
        model.addAttribute("emptySlots", yardService.getEmptySlots());
        model.addAttribute("allSlots", yardService.getAllSlots());

        return "yard/index";
    }

    /**
     * Use Case: Assign Container to Slot
     */
    @PostMapping("/assign")
    public String assignContainer(@RequestParam("containerId") Long containerId,
                                  @RequestParam("slotId") Long slotId,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        try {
            String operatorName = userDetails != null ? userDetails.getUsername() : "Operator";
            yardService.assignContainerToSlot(containerId, slotId, operatorName);
            redirectAttributes.addFlashAttribute("successMessage", "Gán container vào ô bãi thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + com.ctms.util.SafeMessage.of(e));
        }
        return "redirect:/yard";
    }

    /**
     * Use Case: Move Container Between Slots
     */
    @PostMapping("/move")
    public String moveContainer(@RequestParam("fromSlotId") Long fromSlotId,
                                @RequestParam("toSlotId") Long toSlotId,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        try {
            String operatorName = userDetails != null ? userDetails.getUsername() : "Operator";
            yardService.moveContainerBetweenSlots(fromSlotId, toSlotId, operatorName);
            redirectAttributes.addFlashAttribute("successMessage", "Đảo chuyển vị trí container trong bãi thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi đảo bãi: " + com.ctms.util.SafeMessage.of(e));
        }
        return "redirect:/yard";
    }
}
