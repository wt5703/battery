package com.leapmotor.energy.portal.server.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "新增/修改应用参数")
public class AppAddUpdateDto {
    private String name;
    private String code;
    private String contextPath;
    private String indexPath;
    private String domain;
    private String ip;
    private String port;
    private String businessBp;
    private String description;
    private String icon;
    private String businessScene;
    private String capabilityTag;
    private AppSource source;
}

