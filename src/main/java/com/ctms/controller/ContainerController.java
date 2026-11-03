package com.ctms.controller;

import com.ctms.model.Container;
import com.ctms.model.Role;
import com.ctms.model.User;
import com.ctms.service.ContainerService;
import com.ctms.service.ShippingLineService;
import com.ctms.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/containers")
@RequiredArgsConstructor
public class ContainerController {

    private final ContainerService containerService;
    private final ShippingLineService shippingLineService;
    private final UserService userService;

    /**
     * Use Case: Search & Filter Containers.
     * Tài khoản hãng tàu chỉ thấy container thuộc hãng của mình.
     */
    @GetMapping
    public String listContainers(@RequestParam(value = "query", required = false) String query,
                                 @RequestParam(value = "containerType", required = false) Container.ContainerType containerType,
                                 @RequestParam(value = "cargoStatus", required = false) Container.CargoStatus cargoStatus,
                                 @RequestParam(value = "condition", required = false) Container.Condition condition,
                                 @RequestParam(value = "currentStatus", required = false) Container.TrackingStatus currentStatus,
                                 @RequestParam(value = "shippingLineId", required = false) Long shippingLineId,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 Model model) {
        User currentUser = userDetails != null ? userService.findByUsername(userDetails.getUsername()) : null;
        model.addAttribute("currentUser", currentUser);

        boolean restricted = isShippingLineUser(currentUser);
        if (restricted) {
            // Ép bộ lọc theo hãng của người dùng, bỏ qua tham số do client gửi lên
            shippingLineId = currentUser.getShippingLine() != null ? currentUser.getShippingLine().getId() : -1L;
        }

        model.addAttribute("containers", containerService.search(query, containerType, cargoStatus, condition, currentStatus, shippingLineId));
        model.addAttribute("shippingLines", restricted
                ? shippingLineService.getAllShippingLines().stream()
                        .filter(l -> l.getId().equals(currentUser.getShippingLine().getId())).toList()
                : shippingLineService.getAllShippingLines());

        model.addAttribute("paramQuery", query);
        model.addAttribute("paramType", containerType);
        model.addAttribute("paramCargo", cargoStatus);
        model.addAttribute("paramCondition", condition);
        model.addAttribute("paramStatus", currentStatus);
        model.addAttribute("paramShippingLineId", shippingLineId);

        return "container/list";
    }

    /**
     * Use Case: View Movement History & Container Details
     */
    @GetMapping("/{id}")
    public String containerDetail(@PathVariable("id") Long id,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Model model) {
        User currentUser = userDetails != null ? userService.findByUsername(userDetails.getUsername()) : null;
        model.addAttribute("currentUser", currentUser);

        Container container = containerService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy container"));

        if (isShippingLineUser(currentUser)) {
            Long ownLine = currentUser.getShippingLine() != null ? currentUser.getShippingLine().getId() : null;
            Long containerLine = container.getShippingLine() != null ? container.getShippingLine().getId() : null;
            if (ownLine == null || !ownLine.equals(containerLine)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem container này");
            }
        }

        model.addAttribute("container", container);
        model.addAttribute("movements", containerService.getMovementHistory(id));
        return "container/detail";
    }

    private boolean isShippingLineUser(User user) {
        return user != null && user.getRole() == Role.ROLE_SHIPPING_LINE;
    }
}
