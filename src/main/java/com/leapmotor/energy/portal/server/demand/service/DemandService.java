package com.leapmotor.energy.portal.server.demand.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leapmotor.energy.portal.server.demand.dto.DemandModels.Action;
import com.leapmotor.energy.portal.server.demand.dto.DemandModels.Application;
import com.leapmotor.energy.portal.server.demand.dto.DemandModels.Attachment;
import com.leapmotor.energy.portal.server.demand.dto.DemandModels.Created;
import com.leapmotor.energy.portal.server.demand.dto.DemandModels.Detail;
import com.leapmotor.energy.portal.server.demand.dto.DemandModels.Log;
import com.leapmotor.energy.portal.server.demand.dto.DemandModels.Page;
import com.leapmotor.energy.portal.server.demand.dto.DemandModels.Query;
import com.leapmotor.energy.portal.server.demand.dto.DemandModels.Request;
import com.leapmotor.energy.portal.server.demand.dto.DemandModels.Status;
import com.leapmotor.energy.portal.server.demand.mapper.DemandMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DemandService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.ofPattern("yy-MM-dd", Locale.ROOT);

    private final DemandMapper mapper;
    private final ObjectMapper objectMapper;

    public DemandService(DemandMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Created submit(String applicantId, Application application) {
        validate(application);
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        LocalDate date = now.toLocalDate();
        mapper.ensureSequence(date);
        if (mapper.incrementSequence(date) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当日工单流水号已用完");
        }
        String number = NUMBER_DATE.format(date) + "-" + String.format(Locale.ROOT, "%04d",
                mapper.currentSequence(date));
        String id = UUID.randomUUID().toString();
        mapper.insert(new Request(id, number, application.name(), application.category(), application.description(),
                applicantId, Status.PENDING, now, now, encode(application)));
        mapper.insertLog(id, new Log(now, applicantId, null, Action.SUBMIT, null, Status.PENDING, null, null));
        return new Created(id, number);
    }

    @Transactional
    public void resubmit(String id, String applicantId, Application application) {
        validate(application);
        Request current = requireOwner(mapper.lock(id), applicantId);
        if (current.status() == Status.PENDING) {
            throw conflict("待审核工单不能重新提交");
        }
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        mapper.updateApplication(new Request(id, current.number(), application.name(), application.category(),
                application.description(), applicantId, Status.PENDING, current.createTime(), now,
                encode(application)));
        appendLog(current, now, applicantId, Action.RESUBMIT, null, Status.PENDING);
    }

    @Transactional
    public void withdraw(String id, String applicantId, String reason) {
        validateOpinion(reason, true, 100);
        Request current = requireOwner(mapper.lock(id), applicantId);
        transition(current, applicantId, Action.WITHDRAW, reason, Status.WITHDRAWN);
    }

    @Transactional
    public void review(String id, String reviewerId, boolean approved, String opinion) {
        validateOpinion(opinion, !approved, 1000);
        Request current = requireRequest(mapper.lock(id));
        transition(current, reviewerId, approved ? Action.APPROVE : Action.REJECT, opinion,
                approved ? Status.APPROVED : Status.REJECTED);
    }

    @Transactional(readOnly = true)
    public Detail detail(String id, String applicantId) {
        Request current = requireRequest(mapper.find(id));
        if (applicantId != null) {
            requireOwner(current, applicantId);
        }
        return new Detail(id, current.number(), current.applicantId(), current.status(), current.createTime(),
                decode(current.formJson()), mapper.logs(id));
    }

    @Transactional(readOnly = true)
    public Page page(Query query, String applicantId) {
        int pageNum = query.pageNum() == null ? 1 : query.pageNum();
        int pageSize = query.pageSize() == null ? 20 : query.pageSize();
        if (pageNum < 1 || pageSize < 1) {
            throw invalid("页码和每页条数必须大于零");
        }
        return new Page(mapper.page(query, applicantId, (pageNum - 1L) * pageSize, pageSize),
                mapper.count(query, applicantId), pageNum, pageSize);
    }

    private void transition(Request current, String operatorId, Action action, String opinion, Status next) {
        if (current.status() != Status.PENDING) {
            throw conflict("只有待审核工单可以执行此操作");
        }
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        mapper.updateStatus(current.id(), next, now);
        appendLog(current, now, operatorId, action, opinion, next);
    }

    private void appendLog(Request current, LocalDateTime now, String operatorId, Action action,
            String opinion, Status next) {
        long elapsed = Math.max(0, Duration.between(current.updateTime(), now).getSeconds());
        mapper.insertLog(current.id(), new Log(now, operatorId, current.status(), action, opinion, next, null,
                elapsed));
    }

    private static Request requireOwner(Request request, String applicantId) {
        Request current = requireRequest(request);
        if (!current.applicantId().equals(applicantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅发起人可操作本人工单");
        }
        return current;
    }

    private static Request requireRequest(Request request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        }
        return request;
    }

    private static void validate(Application application) {
        if (application == null || blank(application.name()) || application.category() == null
                || blank(application.description()) || blank(application.businessValue())
                || blank(application.userScope()) || application.urgency() == null) {
            throw invalid("请填写需求申请必填项");
        }
        validateAttachments(application.processFiles(), "业务流程图");
        validateAttachments(application.requirementFiles(), "业务需求文档");
    }

    private static void validateAttachments(List<Attachment> files, String label) {
        if (files == null || files.isEmpty()) {
            throw invalid(label + "至少需要一个附件");
        }
        for (Attachment attachment : files) {
            if (attachment == null || blank(attachment.fileId()) || blank(attachment.fileName())) {
                throw invalid(label + "包含无效附件");
            }
        }
    }

    private static void validateOpinion(String text, boolean required, int limit) {
        if (required && blank(text)) {
            throw invalid("请填写处理原因");
        }
        if (text != null && text.codePointCount(0, text.length()) > limit) {
            throw invalid("意见或原因最多 " + limit + " 字");
        }
    }

    private String encode(Application application) {
        try {
            return objectMapper.writeValueAsString(application);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("无法保存申请表单", e);
        }
    }

    private Application decode(String json) {
        try {
            return objectMapper.readValue(json, Application.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("无法读取申请表单", e);
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}

