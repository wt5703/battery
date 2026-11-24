package com.leapmotor.energy.portal.server.app.controller;

import com.leapmotor.energy.common.dto.AppVo;
import com.leapmotor.energy.common.page.PageResult;
import com.leapmotor.energy.portal.server.app.dto.AppAddUpdateDto;
import com.leapmotor.energy.portal.server.app.dto.AppManagementVo;
import com.leapmotor.energy.portal.server.app.dto.AppQueryParam;
import com.leapmotor.energy.portal.server.app.service.AppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app")
@Tag(name = "应用管理")
public class AppController {
    private final AppService appService;

    @PostMapping("/page")
    @Operation(summary = "分页查询应用", description = "按创建时间倒序")
    public PageResult<AppVo> page(@RequestBody AppQueryParam param) {
        return appService.page(param);
    }

    @GetMapping("/all")
    @Operation(summary = "查询所有应用", description = "结果按名称排序")
    public List<AppVo> queryAllApps() {
        return appService.queryAllApps();
    }

    @GetMapping("/{appId}")
    @Operation(summary = "查询应用设置")
    public AppManagementVo detail(@PathVariable("appId") UUID appId) {
        return appService.detail(appId);
    }

    @PutMapping
    @Operation(summary = "新增并上架应用")
    public void addApp(@RequestBody AppAddUpdateDto dto, Principal principal) {
        appService.addApp(dto, operator(principal));
    }

    @PostMapping("/{appId}")
    @Operation(summary = "修改应用")
    public void updateApp(@Parameter(description = "应用Id") @PathVariable("appId") UUID appId,
            @RequestBody AppAddUpdateDto dto, Principal principal) {
        appService.updateApp(appId, dto, operator(principal));
    }

    @DeleteMapping("/{appId}")
    @Operation(summary = "删除应用")
    public void deleteApp(@PathVariable("appId") UUID appId) {
        appService.deleteApp(appId.toString());
    }

    private static String operator(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return principal.getName();
    }
}

