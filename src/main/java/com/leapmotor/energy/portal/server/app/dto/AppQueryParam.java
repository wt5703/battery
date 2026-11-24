package com.leapmotor.energy.portal.server.app.dto;

import com.leapmotor.energy.common.dto.BaseQueryParam;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppQueryParam extends BaseQueryParam {
    private String name;
    private String businessScene;
    private String updatedBy;
}

