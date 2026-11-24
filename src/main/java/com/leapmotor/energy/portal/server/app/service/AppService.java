package com.leapmotor.energy.portal.server.app.service;

import com.leapmotor.energy.common.dto.AppVo;
import com.leapmotor.energy.common.dto.RoleVo;
import com.leapmotor.energy.common.dto.RolesInAppDto;
import com.leapmotor.energy.common.dto.UserVo;
import com.leapmotor.energy.common.enums.ResultCode;
import com.leapmotor.energy.common.exception.BusinessException;
import com.leapmotor.energy.common.page.PageResult;
import com.leapmotor.energy.portal.server.app.dto.AppAddUpdateDto;
import com.leapmotor.energy.portal.server.app.dto.AppManagementVo;
import com.leapmotor.energy.portal.server.app.dto.AppQueryParam;
import com.leapmotor.energy.portal.server.app.dto.AppSource;
import com.leapmotor.energy.portal.server.app.mapper.AppMapper;
import com.leapmotor.energy.portal.server.role.dto.RoleAddUpdateDto;
import com.leapmotor.energy.portal.server.role.dto.RoleQueryDto;
import com.leapmotor.energy.portal.server.role.service.RoleService;
import com.leapmotor.energy.portal.server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.leapmotor.energy.common.constant.CommonConstants.ADMIN_ID;
import static com.leapmotor.energy.common.constant.PortalConstants.ADMIN_ROLE_CODE;
import static com.leapmotor.energy.common.constant.PortalConstants.ADMIN_ROLE_NAME;
import static com.leapmotor.energy.common.constant.PortalConstants.PLAIN_USER_ROLE_CODE;
import static com.leapmotor.energy.common.constant.PortalConstants.PLAIN_USER_ROLE_NAME;

@Service
@RequiredArgsConstructor
public class AppService {
    private final AppMapper appMapper;
    private final RoleService roleService;
    private final UserService userService;

    public PageResult<AppVo> page(AppQueryParam param) {
        PageResult<AppVo> result = new PageResult<>();
        result.setList(appMapper.page(param));
        result.setPageNum(param.getPageNum());
        result.setPageSize(param.getPageSize());
        result.setTotal(appMapper.count(param));
        return result;
    }

    public List<AppVo> queryAllApps() {
        return appMapper.queryAllApps();
    }

    public AppManagementVo detail(UUID appId) {
        AppManagementVo app = appMapper.queryManagementApp(appId.toString());
        if (app == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "应用不存在");
        }
        return app;
    }

    @Transactional(rollbackFor = Exception.class)
    public void addApp(AppAddUpdateDto dto, String updatedBy) {
        appMapper.lockAppWrites();
        validate(dto, null);
        checkContextPaths(null, dto.getContextPath());

        String appId = UUID.randomUUID().toString();
        try {
            appMapper.addApp(appId, dto, updatedBy);
        } catch (DuplicateKeyException e) {
            throw BusinessException.getInstance(ResultCode.DUPLICATE_APP);
        }

        String adminRoleId = roleService.addRole(RoleAddUpdateDto.builder()
                .name(ADMIN_ROLE_NAME)
                .code(ADMIN_ROLE_CODE)
                .appId(appId)
                .buildIn(true)
                .description(ADMIN_ROLE_NAME)
                .build());
        roleService.addRole(RoleAddUpdateDto.builder()
                .name(PLAIN_USER_ROLE_NAME)
                .code(PLAIN_USER_ROLE_CODE)
                .appId(appId)
                .buildIn(true)
                .description(PLAIN_USER_ROLE_NAME)
                .build());
        UserVo admin = userService.queryUserByLeapId(ADMIN_ID);
        List<RolesInAppDto> roleIds = new ArrayList<>(admin.getRoleIds());
        roleIds.add(RolesInAppDto.builder().appId(appId).roleIds(List.of(adminRoleId)).build());
        userService.setRole(ADMIN_ID, roleIds);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateApp(UUID appId, AppAddUpdateDto dto, String updatedBy) {
        appMapper.lockAppWrites();
        AppManagementVo existing = detail(appId);
        validate(dto, existing);

        String contextPath = blank(dto.getContextPath()) ? existing.getContextPath() : dto.getContextPath();
        checkContextPaths(appId.toString(), contextPath);

        try {
            appMapper.updateApp(appId.toString(), dto, updatedBy);
        } catch (DuplicateKeyException e) {
            throw BusinessException.getInstance(ResultCode.DUPLICATE_APP);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteApp(String appId) {
        appMapper.lockAppWrites();
        List<RoleVo> roleVos = roleService.queryRoleList(RoleQueryDto.builder().appId(appId).build());
        roleVos.forEach(roleVo -> roleService.deleteRole(roleVo.getId()));
        appMapper.deleteApp(appId);
    }

    public AppVo queryAppByCode(String code) {
        return appMapper.queryAppByCode(code);
    }

    private void validate(AppAddUpdateDto dto, AppManagementVo existing) {
        boolean isNew = existing == null;
        if (isNew && (blank(dto.getName()) || blank(dto.getCode()) || dto.getSource() == null)) {
            throw invalid("请填写应用名称、编码和来源");
        }
        if (isNew || !java.util.Objects.equals(existing.getName(), dto.getName())) {
            validateField(dto.getName(), null, "应用名称", 20, false);
        }
        validateField(dto.getBusinessBp(), isNew ? null : existing.getBusinessBp(), "业务BP", 128, isNew);
        validateField(dto.getDescription(), isNew ? null : existing.getDescription(), "应用简介", 100, isNew);
        validateField(dto.getIcon(), isNew ? null : existing.getIcon(), "应用图标", null, isNew);
        validateField(dto.getBusinessScene(), isNew ? null : existing.getBusinessScene(), "业务场景", 128, isNew);
        validateField(dto.getCapabilityTag(), isNew ? null : existing.getCapabilityTag(), "能力标签", 128, isNew);
        if (!blank(dto.getBusinessBp()) && userService.queryUserByLeapId(dto.getBusinessBp()) == null) {
            throw invalid("业务BP不存在");
        }

        AppSource source = dto.getSource();
        if (source == null && !isNew) {
            source = existing.getSource();
        }
        if (source != AppSource.EXTERNAL) {
            String contextPath = !isNew && blank(dto.getContextPath()) ? existing.getContextPath() : dto.getContextPath();
            String indexPath = !isNew && blank(dto.getIndexPath()) ? existing.getIndexPath() : dto.getIndexPath();
            if (prefixes(contextPath).isEmpty() || blank(indexPath)) {
                throw invalid("自研应用必须填写后端URL前缀和前端首页路径");
            }
        }
    }

    private static void validateField(String value, String oldValue, String label, Integer maxLength,
            boolean required) {
        if ((required && blank(value)) || (value != null && value.isBlank() && !blank(oldValue))) {
            throw invalid(label + "不能为空");
        }
        if (value != null && maxLength != null && value.codePointCount(0, value.length()) > maxLength) {
            throw invalid(label + "最多" + maxLength + "字");
        }
    }

    private void checkContextPaths(String currentId, String contextPath) {
        List<String> submitted = prefixes(contextPath);
        if (submitted.isEmpty()) {
            return;
        }
        for (AppVo app : appMapper.queryPrefixesForUpdate()) {
            if (currentId != null && currentId.equals(String.valueOf(app.getId()))) {
                continue;
            }
            for (String existing : prefixes(app.getContextPath())) {
                for (String candidate : submitted) {
                    if (existing.startsWith(candidate) || candidate.startsWith(existing)) {
                        throw BusinessException.getInstance(ResultCode.APP_URL_CONTAINED);
                    }
                }
            }
        }
    }

    private static List<String> prefixes(String value) {
        if (blank(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(prefix -> !prefix.isEmpty()).toList();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}

