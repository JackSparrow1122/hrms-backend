package com.gahrms.backend.modules.attendance.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gahrms.backend.modules.attendance.dto.BiometricPushBatchRequest;
import com.gahrms.backend.modules.attendance.dto.SyncSummaryDto;
import com.gahrms.backend.modules.attendance.service.AttendanceSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rest_api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Biometric Attendance Legacy Webhook", description = "Drop-in compatible webhook endpoint for on-premise PHP attendance sync script")
public class BiometricWebhookController {

    private final AttendanceSyncService attendanceSyncService;
    private final ObjectMapper objectMapper;

    @PostMapping("/attendancePhp")
    @Operation(
            summary = "Legacy PHP attendance sync push endpoint",
            description = "Accepts punch batches directly from the existing on-premise PHP sync script without requiring path or structure changes."
    )
    public ResponseEntity<Map<String, Object>> ingestLegacyPhpAttendance(
            @RequestBody JsonNode rootNode
    ) {
        BiometricPushBatchRequest batchRequest = new BiometricPushBatchRequest();
        List<BiometricPushBatchRequest.BiometricPushItemDto> items = new ArrayList<>();

        try {
            if (rootNode.isArray()) {
                items = objectMapper.readerFor(new TypeReference<List<BiometricPushBatchRequest.BiometricPushItemDto>>() {})
                        .readValue(rootNode);
            } else if (rootNode.has("data") && rootNode.get("data").isArray()) {
                items = objectMapper.readerFor(new TypeReference<List<BiometricPushBatchRequest.BiometricPushItemDto>>() {})
                        .readValue(rootNode.get("data"));
            } else if (rootNode.has("punches") && rootNode.get("punches").isArray()) {
                items = objectMapper.readerFor(new TypeReference<List<BiometricPushBatchRequest.BiometricPushItemDto>>() {})
                        .readValue(rootNode.get("punches"));
            } else {
                BiometricPushBatchRequest.BiometricPushItemDto single = objectMapper.treeToValue(
                        rootNode, BiometricPushBatchRequest.BiometricPushItemDto.class);
                if (single != null && single.getSerial() != null) {
                    items.add(single);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse biometric payload: {}", e.getMessage(), e);
        }

        batchRequest.setData(items);
        SyncSummaryDto summary = attendanceSyncService.ingestPushBatch(batchRequest);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("status", "SUCCESS");
        response.put("message", "Processed " + summary.getFetchedCount() + " records (Inserted: " + summary.getInsertedCount() + ", Duplicates: " + summary.getSkippedDuplicatesCount() + ")");
        response.put("inserted", summary.getInsertedCount());
        response.put("cursor", summary.getNewCursorId());

        return ResponseEntity.ok(response);
    }
}
