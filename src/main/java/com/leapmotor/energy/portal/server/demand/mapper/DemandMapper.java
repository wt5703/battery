package com.leapmotor.energy.portal.server.demand.mapper;

import com.leapmotor.energy.portal.server.demand.dto.DemandModels.ListItem;
import com.leapmotor.energy.portal.server.demand.dto.DemandModels.Log;
import com.leapmotor.energy.portal.server.demand.dto.DemandModels.Query;
import com.leapmotor.energy.portal.server.demand.dto.DemandModels.Request;
import com.leapmotor.energy.portal.server.demand.dto.DemandModels.Status;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DemandMapper {
    void ensureSequence(@Param("date") LocalDate date);

    int incrementSequence(@Param("date") LocalDate date);

    int currentSequence(@Param("date") LocalDate date);

    void insert(@Param("request") Request request);

    Request find(@Param("id") String id);

    Request lock(@Param("id") String id);

    int updateStatus(@Param("id") String id, @Param("status") Status status,
            @Param("updateTime") LocalDateTime updateTime);

    int updateApplication(@Param("request") Request request);

    void insertLog(@Param("id") String id, @Param("log") Log log);

    List<Log> logs(@Param("id") String id);

    List<ListItem> page(@Param("query") Query query, @Param("applicantId") String applicantId,
            @Param("offset") long offset, @Param("limit") int limit);

    long count(@Param("query") Query query, @Param("applicantId") String applicantId);
}

