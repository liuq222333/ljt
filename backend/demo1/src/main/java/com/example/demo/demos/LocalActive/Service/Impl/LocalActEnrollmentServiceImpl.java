package com.example.demo.demos.LocalActive.Service.Impl;

import com.example.demo.demos.LocalActive.DTO.LocalActEnrollmentItem;
import com.example.demo.demos.LocalActive.DTO.LocalActEnrollmentListResponse;
import com.example.demo.demos.LocalActive.DTO.LocalActEnrollmentQuery;
import com.example.demo.demos.LocalActive.DTO.LocalActEnrollmentRecord;
import com.example.demo.demos.LocalActive.DTO.LocalActEnrollmentStats;
import com.example.demo.demos.LocalActive.Dao.LocalActEnrollmentMapper;
import com.example.demo.demos.LocalActive.Service.LocalActEnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocalActEnrollmentServiceImpl implements LocalActEnrollmentService {

    private final LocalActEnrollmentMapper mapper;
    private static final DateTimeFormatter REMINDER_FORMATTER =
            DateTimeFormatter.ofPattern("MM/dd HH:mm", Locale.getDefault());

    @Override
    public LocalActEnrollmentListResponse getUserEnrollments(LocalActEnrollmentQuery query) {
        if (query == null || query.getUsername() == null || query.getUsername().trim().isEmpty()) {
            return new LocalActEnrollmentListResponse(Collections.emptyList(),
                    new LocalActEnrollmentStats(0, 0, 0));
        }
        List<LocalActEnrollmentRecord> records = mapper.findEnrollments(query);
        List<LocalActEnrollmentItem> items = records.stream()
                .map(this::toItem)
                .collect(Collectors.toList());
        LocalDateTime now = LocalDateTime.now();
        int upcoming = (int) records.stream()
                .filter(r -> r.getStartAt() != null && !r.getStartAt().isBefore(now))
                .count();
        int confirmed = (int) records.stream()
                .filter(r -> "confirmed".equalsIgnoreCase(r.getStatus()))
                .count();
        int volunteerHours = confirmed * 2;
        return new LocalActEnrollmentListResponse(items,
                new LocalActEnrollmentStats(upcoming, confirmed, volunteerHours));
    }

    private LocalActEnrollmentItem toItem(LocalActEnrollmentRecord record) {
        LocalActEnrollmentItem item = new LocalActEnrollmentItem();
        item.setId(record.getEnrollmentId());
        item.setActivityId(record.getActivityId());
        item.setTitle(record.getTitle());
        item.setOrganizer(record.getOrganizer());
        item.setLocation(record.getLocation());
        item.setStatus(record.getStatus());
        item.setStartAt(record.getStartAt());
        item.setReminder(buildReminder(record));
        item.setTags(parseTags(record.getTagsCsv()));
        return item;
    }

    private List<String> parseTags(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String buildReminder(LocalActEnrollmentRecord record) {
        if ("pending".equalsIgnoreCase(record.getStatus())) {
            return "待审核 · 预计 12 小时内反馈";
        }
        if ("waitlist".equalsIgnoreCase(record.getStatus())) {
            String rank = record.getWaitlistRank() != null ? String.valueOf(record.getWaitlistRank()) : "-";
            return String.format("候补第 %s 位 · 有空位自动通知", rank);
        }
        if (record.getStartAt() != null) {
            return "活动前 24 小时提醒 · " + record.getStartAt().format(REMINDER_FORMATTER);
        }
        return "活动提醒";
    }
}
