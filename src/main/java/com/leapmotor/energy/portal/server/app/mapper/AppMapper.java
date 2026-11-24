package com.leapmotor.energy.portal.server.app.mapper;

import com.leapmotor.energy.common.dto.AppVo;
import com.leapmotor.energy.portal.server.app.dto.AppAddUpdateDto;
import com.leapmotor.energy.portal.server.app.dto.AppManagementVo;
import com.leapmotor.energy.portal.server.app.dto.AppQueryParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AppMapper {
    List<AppVo> page(AppQueryParam param);

    int count(AppQueryParam param);

    List<AppVo> queryAllApps();

    AppVo queryAppById(@Param("appId") String appId);

    AppVo queryAppByCode(@Param("code") String code);

    AppManagementVo queryManagementApp(@Param("appId") String appId);

    int lockAppWrites();

    List<AppVo> queryPrefixesForUpdate();

    void addApp(@Param("appId") String appId, @Param("dto") AppAddUpdateDto dto,
            @Param("updatedBy") String updatedBy);

    void updateApp(@Param("appId") String appId, @Param("dto") AppAddUpdateDto dto,
            @Param("updatedBy") String updatedBy);

    void deleteApp(@Param("appId") String appId);
}

