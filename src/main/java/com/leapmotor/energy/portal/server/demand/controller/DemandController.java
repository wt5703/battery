package com.leapmotor.energy.portal.server.demand.controller;

import com.leapmotor.energy.portal.server.demand.dto.DemandModels.Application;
import com.leapmotor.energy.portal.server.demand.dto.DemandModels.Created;
import com.leapmotor.energy.portal.server.demand.dto.DemandModels.Detail;
import com.leapmotor.energy.portal.server.demand.dto.DemandModels.Page;
import com.leapmotor.energy.portal.server.demand.dto.DemandModels.Query;
import com.leapmotor.energy.portal.server.demand.dto.DemandModels.Review;
import com.leapmotor.energy.portal.server.demand.dto.DemandModels.Withdrawal;
import com.leapmotor.energy.portal.server.demand.service.DemandService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@RestController
@RequestMapping("/demand")
public class DemandController {
    private final DemandService service;

    public DemandController(DemandService service) {
        this.service = service;
    }

    @PutMapping
    public Created submit(@RequestBody Application application, Principal principal) {
        return service.submit(userId(principal), application);
    }

    @PostMapping("/mine/page")
    public Page mine(@RequestBody Query query, Principal principal) {
        return service.page(query, userId(principal));
    }

    @PostMapping("/all/page")
    public Page all(@RequestBody Query query, Principal principal) {
        userId(principal);
        return service.page(query, null);
    }

    @GetMapping("/mine/{id}")
    public Detail mineDetail(@PathVariable("id") String id, Principal principal) {
        return service.detail(id, userId(principal));
    }

    @GetMapping("/all/{id}")
    public Detail allDetail(@PathVariable("id") String id, Principal principal) {
        userId(principal);
        return service.detail(id, null);
    }

    @PostMapping("/{id}/resubmit")
    public void resubmit(@PathVariable("id") String id, @RequestBody Application application, Principal principal) {
        service.resubmit(id, userId(principal), application);
    }

    @PostMapping("/{id}/withdraw")
    public void withdraw(@PathVariable("id") String id, @RequestBody Withdrawal withdrawal, Principal principal) {
        service.withdraw(id, userId(principal), withdrawal.reason());
    }

    @PostMapping("/{id}/review")
    public void review(@PathVariable("id") String id, @RequestBody Review review, Principal principal) {
        if (review.approved() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择审核结果");
        }
        service.review(id, userId(principal), review.approved(), review.opinion());
    }

    private static String userId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return principal.getName();
    }
}

