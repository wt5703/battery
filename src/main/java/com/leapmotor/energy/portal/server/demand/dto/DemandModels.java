package com.leapmotor.energy.portal.server.demand.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class DemandModels {
    private DemandModels() {
    }

    public enum Status {
        PENDING, APPROVED, REJECTED, WITHDRAWN
    }

    public enum Category {
        PROCESS_NEW, PROCESS_CHANGE, SYSTEM_INTEGRATION, TECHNICAL_OPERATIONS
    }

    public enum Urgency {
        LOW, MEDIUM, HIGH, URGENT
    }

    public enum Action {
        SUBMIT, RESUBMIT, APPROVE, REJECT, WITHDRAW
    }

    public record Attachment(String fileId, String fileName) {
    }

    public record Application(String name, Category category, String description, String businessValue,
            String userScope, List<Attachment> processFiles, List<Attachment> requirementFiles,
            Urgency urgency, LocalDate expectedDate) {
    }

    public record Review(Boolean approved, String opinion) {
    }

    public record Withdrawal(String reason) {
    }

    public record Query(String name, String number, Category category, Status status, Integer pageNum,
            Integer pageSize) {
    }

    public record Request(String id, String number, String name, Category category, String description,
            String applicantId, Status status, LocalDateTime createTime, LocalDateTime updateTime,
            String formJson) {
    }

    public record ListItem(String id, String number, String name, Category category, String description,
            String applicantId, Status status, LocalDateTime createTime) {
    }

    public record Log(LocalDateTime operationTime, String operatorId, Status previousStatus, Action action,
            String opinion, Status nextStatus, String nextOperatorId, Long stageSeconds) {
    }

    public record Detail(String id, String number, String applicantId, Status status, LocalDateTime createTime,
            Application application, List<Log> logs) {
    }

    public record Page(List<ListItem> list, long total, int pageNum, int pageSize) {
    }

    public record Created(String id, String number) {
    }
}

