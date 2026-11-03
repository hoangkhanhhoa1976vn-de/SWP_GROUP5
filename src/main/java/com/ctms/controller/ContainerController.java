package com.ctms.controller;

import com.ctms.model.Container;
import com.ctms.model.Role;
import com.ctms.model.User;
import com.ctms.service.ContainerService;
import com.ctms.service.ShippingLineService;
import com.ctms.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
@RequestMapping("/containers")
public class ContainerController {

    private final ContainerService containerService;
    private final ShippingLineService shippingLineService;
    private final UserService userService;

    public ContainerController(ContainerService containerService,
                               ShippingLineService shippingLineService,
                               UserService userService) {
        this.containerService = containerService;
        this.shippingLineService = shippingLineService;
        this.userService = userService;
    }

    /**
     * Use Case: Search & Filter Containers.
     * Tài khoản hãng tàu chỉ thấy container thuộc hãng của mình.
     */
    @GetMapping
    public String searchContainers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Container.TrackingStatus status,
            @RequestParam(required = false) Long shippingLineId,
            @RequestParam(required = false) Container.ContainerType type,
            @RequestParam(required = false) Container.CargoStatus cargoStatus,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        User currentUser = userDetails != null ? userService.findByUsername(userDetails.getUsername()) : null;
        model.addAttribute("currentUser", currentUser);

        boolean restricted = isShippingLineUser(currentUser);
        if (restricted) {
            // Ép bộ lọc theo hãng của người dùng, bỏ qua tham số do client gửi lên
            shippingLineId = currentUser.getShippingLine() != null ? currentUser.getShippingLine().getId() : -1L;
        }

        List<Container> containers = containerService.searchContainers(keyword, status, shippingLineId, type, cargoStatus);

        model.addAttribute("containers", containers);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedShippingLine", shippingLineId);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedCargoStatus", cargoStatus);
        model.addAttribute("shippingLines", restricted
                ? shippingLineService.getAllShippingLines().stream()
                        .filter(l -> l.getId().equals(currentUser.getShippingLine().getId())).toList()
                : shippingLineService.getAllShippingLines());
        model.addAttribute("trackingStatuses", Container.TrackingStatus.values());
        model.addAttribute("containerTypes", Container.ContainerType.values());
        model.addAttribute("cargoStatuses", Container.CargoStatus.values());

        return "container/list";
    }

    /**
     * Use Case: View Container Tracking Details.
     * Hãng tàu không được xem chi tiết container của hãng khác.
     */
    @GetMapping("/{id}")
    public String getContainerDetail(
            @PathVariable Long id,
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
        model.addAttribute("history", containerService.getMovementHistory(id));

        return "container/detail";
    }

    private boolean isShippingLineUser(User user) {
        return user != null && user.getRole() == Role.ROLE_SHIPPING_LINE;
    }
}
