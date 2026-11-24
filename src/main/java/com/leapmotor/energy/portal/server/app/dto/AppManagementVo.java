package com.leapmotor.energy.portal.server.app.dto;

import com.leapmotor.energy.common.dto.AppVo;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AppManagementVo extends AppVo {
    private String businessBp;
    private String description;
    private String icon;
    private String businessScene;
    private String capabilityTag;
    private AppSource source;
    private String updatedBy;
    private LocalDateTime updateTime;
}

