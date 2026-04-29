package com.xc.study.module.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.module.admin.dto.AdminSentenceWordOptionDTO;
import com.xc.study.module.admin.dto.AdminUpsertDialogueLineDTO;
import com.xc.study.module.admin.dto.AdminUpsertDialogueLineVocabDTO;
import com.xc.study.module.admin.dto.AdminUpsertExerciseSetDTO;
import com.xc.study.module.admin.dto.AdminUpsertSentenceExerciseDTO;
import com.xc.study.module.admin.dto.AdminUpsertVideoMaterialDTO;
import com.xc.study.module.admin.dto.AdminUpsertVocabItemDTO;
import com.xc.study.module.admin.dto.AdminUpsertVocabListDTO;
import com.xc.study.module.admin.entity.AdminOperationLog;
import com.xc.study.module.admin.mapper.AdminOperationLogMapper;
import com.xc.study.module.admin.service.AdminContentImportService;
import com.xc.study.module.admin.service.AdminDialogueManagementService;
import com.xc.study.module.admin.service.AdminExerciseManagementService;
import com.xc.study.module.admin.service.AdminVocabManagementService;
import com.xc.study.module.admin.vo.AdminContentImportResultVO;
import com.xc.study.module.admin.vo.AdminContentImportTemplateVO;
import com.xc.study.security.CurrentUser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminContentImportServiceImpl implements AdminContentImportService {

    private static final int MAX_ROWS = 1000;
    private static final long MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024;

    private static final List<String> VOCAB_LIST_HEADERS = List.of(
            "id", "name", "list_type", "level", "description", "sort_order", "status"
    );
    private static final List<String> VOCAB_ITEM_HEADERS = List.of(
            "id", "vocab_list_id", "hanzi", "pinyin", "meaning_en", "meaning_ru", "example_sentence",
            "audio_asset_id", "sort_order", "status"
    );
    private static final List<String> EXERCISE_SET_HEADERS = List.of(
            "id", "title", "exercise_type", "level", "status"
    );
    private static final List<String> SENTENCE_EXERCISE_HEADERS = List.of(
            "id", "exercise_set_id", "exercise_type", "hanzi_answer", "pinyin_prompt", "translation_en",
            "translation_ru", "audio_zh_asset_id", "explanation", "sort_order", "status", "word_options"
    );
    private static final List<String> VIDEO_MATERIAL_HEADERS = List.of(
            "id", "title", "material_type", "description", "cover_asset_id", "status"
    );
    private static final List<String> DIALOGUE_LINE_HEADERS = List.of(
            "id", "material_id", "line_no", "hanzi_text", "pinyin_text", "translation_en",
            "translation_ru", "audio_asset_id", "start_ms", "end_ms"
    );
    private static final List<String> DIALOGUE_LINE_VOCAB_HEADERS = List.of(
            "id", "dialogue_line_id", "vocab_item_id", "word_text", "pinyin", "meaning_en", "meaning_ru", "explanation"
    );

    private static final Set<String> CONTENT_STATUSES = Set.of("active", "inactive");
    private static final Set<String> VOCAB_LIST_TYPES = Set.of("HSK", "YCT", "category", "professional", "custom");
    private static final Set<String> EXERCISE_TYPES = Set.of("audio_order", "audio_dictation", "pinyin_dictation", "translation_order");
    private static final Set<String> MATERIAL_TYPES = Set.of("drama", "short_video", "cartoon");

    private final AdminVocabManagementService adminVocabManagementService;
    private final AdminExerciseManagementService adminExerciseManagementService;
    private final AdminDialogueManagementService adminDialogueManagementService;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final ObjectMapper objectMapper;

    public AdminContentImportServiceImpl(
            AdminVocabManagementService adminVocabManagementService,
            AdminExerciseManagementService adminExerciseManagementService,
            AdminDialogueManagementService adminDialogueManagementService,
            AdminOperationLogMapper adminOperationLogMapper,
            ObjectMapper objectMapper
    ) {
        this.adminVocabManagementService = adminVocabManagementService;
        this.adminExerciseManagementService = adminExerciseManagementService;
        this.adminDialogueManagementService = adminDialogueManagementService;
        this.adminOperationLogMapper = adminOperationLogMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public AdminContentImportTemplateVO template(String importType, CurrentUser admin) {
        requirePermission(admin, "admin:content:read");
        String type = normalizeType(importType);
        StringBuilder builder = new StringBuilder("\uFEFF");
        builder.append(toCsvLine(headersFor(type))).append("\n");
        for (List<String> row : templateCommentRows(type)) {
            builder.append(toCsvLine(row)).append("\n");
        }
        return new AdminContentImportTemplateVO(type + "-template.csv", builder.toString());
    }

    @Override
    public AdminContentImportResultVO importCsv(String importType, MultipartFile file, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        String type = normalizeType(importType);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "请选择 CSV 文件");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "CSV 文件不能超过 2MB");
        }
        List<Map<String, String>> rows = parseRows(file, headersFor(type));
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        for (int index = 0; index < rows.size(); index++) {
            int rowNumber = index + 2;
            try {
                importRow(type, rows.get(index), admin, ipAddress);
                successCount++;
            } catch (BusinessException ex) {
                errors.add("第 " + rowNumber + " 行：" + ex.getMessage());
            } catch (RuntimeException ex) {
                errors.add("第 " + rowNumber + " 行：导入失败");
            }
        }
        writeOperationLog(admin.id(), "content.import." + type, "content_import", null, Map.of(
                "importType", type,
                "requestedCount", rows.size(),
                "successCount", successCount,
                "errors", errors
        ), ipAddress);
        return new AdminContentImportResultVO(type, rows.size(), successCount, errors);
    }

    private void importRow(String type, Map<String, String> row, CurrentUser admin, String ipAddress) {
        Long id = optionalLong(row, "id");
        switch (type) {
            case "vocab-lists" -> importVocabList(id, row, admin, ipAddress);
            case "vocab-items" -> importVocabItem(id, row, admin, ipAddress);
            case "exercise-sets" -> importExerciseSet(id, row, admin, ipAddress);
            case "sentence-exercises" -> importSentenceExercise(id, row, admin, ipAddress);
            case "video-materials" -> importVideoMaterial(id, row, admin, ipAddress);
            case "dialogue-lines" -> importDialogueLine(id, row, admin, ipAddress);
            case "dialogue-line-vocab" -> importDialogueLineVocab(id, row, admin, ipAddress);
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR, "不支持的导入类型");
        }
    }

    private void importVocabList(Long id, Map<String, String> row, CurrentUser admin, String ipAddress) {
        AdminUpsertVocabListDTO request = new AdminUpsertVocabListDTO(
                required(row, "name"),
                enumValue(required(row, "list_type"), VOCAB_LIST_TYPES, "list_type"),
                blankToNull(text(row, "level")),
                blankToNull(text(row, "description")),
                requiredInt(row, "sort_order"),
                statusValue(text(row, "status"))
        );
        if (id == null) {
            adminVocabManagementService.createList(request, admin, ipAddress);
        } else {
            adminVocabManagementService.updateList(id, request, admin, ipAddress);
        }
    }

    private void importVocabItem(Long id, Map<String, String> row, CurrentUser admin, String ipAddress) {
        AdminUpsertVocabItemDTO request = new AdminUpsertVocabItemDTO(
                requiredLong(row, "vocab_list_id"),
                required(row, "hanzi"),
                blankToNull(text(row, "pinyin")),
                blankToNull(text(row, "meaning_en")),
                blankToNull(text(row, "meaning_ru")),
                blankToNull(text(row, "example_sentence")),
                optionalLong(row, "audio_asset_id"),
                requiredInt(row, "sort_order"),
                statusValue(text(row, "status"))
        );
        if (id == null) {
            adminVocabManagementService.createItem(request, admin, ipAddress);
        } else {
            adminVocabManagementService.updateItem(id, request, admin, ipAddress);
        }
    }

    private void importExerciseSet(Long id, Map<String, String> row, CurrentUser admin, String ipAddress) {
        AdminUpsertExerciseSetDTO request = new AdminUpsertExerciseSetDTO(
                required(row, "title"),
                enumValue(required(row, "exercise_type"), EXERCISE_TYPES, "exercise_type"),
                blankToNull(text(row, "level")),
                statusValue(text(row, "status"))
        );
        if (id == null) {
            adminExerciseManagementService.createSet(request, admin, ipAddress);
        } else {
            adminExerciseManagementService.updateSet(id, request, admin, ipAddress);
        }
    }

    private void importSentenceExercise(Long id, Map<String, String> row, CurrentUser admin, String ipAddress) {
        AdminUpsertSentenceExerciseDTO request = new AdminUpsertSentenceExerciseDTO(
                requiredLong(row, "exercise_set_id"),
                enumValue(required(row, "exercise_type"), EXERCISE_TYPES, "exercise_type"),
                required(row, "hanzi_answer"),
                blankToNull(text(row, "pinyin_prompt")),
                blankToNull(text(row, "translation_en")),
                blankToNull(text(row, "translation_ru")),
                optionalLong(row, "audio_zh_asset_id"),
                blankToNull(text(row, "explanation")),
                requiredInt(row, "sort_order"),
                statusValue(text(row, "status")),
                parseWordOptions(text(row, "word_options"))
        );
        if (id == null) {
            adminExerciseManagementService.createSentenceExercise(request, admin, ipAddress);
        } else {
            adminExerciseManagementService.updateSentenceExercise(id, request, admin, ipAddress);
        }
    }

    private void importVideoMaterial(Long id, Map<String, String> row, CurrentUser admin, String ipAddress) {
        AdminUpsertVideoMaterialDTO request = new AdminUpsertVideoMaterialDTO(
                required(row, "title"),
                enumValue(required(row, "material_type"), MATERIAL_TYPES, "material_type"),
                blankToNull(text(row, "description")),
                optionalLong(row, "cover_asset_id"),
                statusValue(text(row, "status"))
        );
        if (id == null) {
            adminDialogueManagementService.createMaterial(request, admin, ipAddress);
        } else {
            adminDialogueManagementService.updateMaterial(id, request, admin, ipAddress);
        }
    }

    private void importDialogueLine(Long id, Map<String, String> row, CurrentUser admin, String ipAddress) {
        AdminUpsertDialogueLineDTO request = new AdminUpsertDialogueLineDTO(
                requiredLong(row, "material_id"),
                requiredInt(row, "line_no"),
                required(row, "hanzi_text"),
                blankToNull(text(row, "pinyin_text")),
                blankToNull(text(row, "translation_en")),
                blankToNull(text(row, "translation_ru")),
                optionalLong(row, "audio_asset_id"),
                optionalInt(row, "start_ms"),
                optionalInt(row, "end_ms")
        );
        if (id == null) {
            adminDialogueManagementService.createLine(request, admin, ipAddress);
        } else {
            adminDialogueManagementService.updateLine(id, request, admin, ipAddress);
        }
    }

    private void importDialogueLineVocab(Long id, Map<String, String> row, CurrentUser admin, String ipAddress) {
        AdminUpsertDialogueLineVocabDTO request = new AdminUpsertDialogueLineVocabDTO(
                requiredLong(row, "dialogue_line_id"),
                optionalLong(row, "vocab_item_id"),
                required(row, "word_text"),
                blankToNull(text(row, "pinyin")),
                blankToNull(text(row, "meaning_en")),
                blankToNull(text(row, "meaning_ru")),
                blankToNull(text(row, "explanation"))
        );
        if (id == null) {
            adminDialogueManagementService.createLineVocab(request, admin, ipAddress);
        } else {
            adminDialogueManagementService.updateLineVocab(id, request, admin, ipAddress);
        }
    }

    private List<Map<String, String>> parseRows(MultipartFile file, List<String> expectedHeaders) {
        List<List<String>> csvRows;
        try {
            csvRows = parseCsv(new String(file.getBytes(), StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "读取 CSV 文件失败");
        }
        csvRows = csvRows.stream().filter(row -> row.stream().anyMatch(StringUtils::hasText)).toList();
        if (csvRows.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "CSV 文件没有表头");
        }
        List<String> headers = csvRows.get(0).stream().map(this::normalizeHeader).toList();
        for (String expectedHeader : expectedHeaders) {
            if (!headers.contains(expectedHeader)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "CSV 缺少表头：" + expectedHeader);
            }
        }
        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 1; i < csvRows.size(); i++) {
            List<String> values = csvRows.get(i);
            if (values.stream().noneMatch(StringUtils::hasText)) {
                continue;
            }
            if (!values.isEmpty() && normalizeHeader(values.get(0)).startsWith("#")) {
                continue;
            }
            Map<String, String> row = new LinkedHashMap<>();
            for (String expectedHeader : expectedHeaders) {
                int columnIndex = headers.indexOf(expectedHeader);
                row.put(expectedHeader, columnIndex < values.size() ? values.get(columnIndex).trim() : "");
            }
            rows.add(row);
        }
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "CSV 文件没有可导入的数据行");
        }
        if (rows.size() > MAX_ROWS) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "一次最多导入 " + MAX_ROWS + " 行");
        }
        return rows;
    }

    private List<List<String>> templateCommentRows(String importType) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(templateNote("说明：不要修改第一行英文表头；id 留空表示新增，有值表示更新对应记录。"));
        rows.add(templateNote("说明：导入时会自动忽略第一列以 # 开头的说明或示例行。"));
        rows.add(templateNote("枚举：status 可填 active 或 inactive；空值按业务默认处理。"));
        rows.add(templateNote("示例：可以复制示例行，把第一列 # 改为空或真实 id 后再导入。"));
        rows.add(sampleRow(importType));
        return rows;
    }

    private List<String> templateNote(String note) {
        List<String> row = new ArrayList<>();
        row.add("# " + note);
        return row;
    }

    private List<String> sampleRow(String importType) {
        return switch (importType) {
            case "vocab-lists" -> List.of("#", "HSK1 基础词汇", "HSK", "HSK1", "基础词汇", "0", "active");
            case "vocab-items" -> List.of("#", "1", "中国", "zhong guo", "China", "Китай", "我来自中国。", "101", "0", "active");
            case "exercise-sets" -> List.of("#", "HSK1 听音频排序", "audio_order", "HSK1", "active");
            case "sentence-exercises" -> List.of(
                    "#", "1", "audio_order", "我是学生", "wo shi xue sheng", "I am a student",
                    "Я студент", "101", "主语 + 是 + 身份", "0", "active", "我|是|学生"
            );
            case "video-materials" -> List.of("#", "Demo HSK1 日常对话", "short_video", "日常问候片段", "201", "active");
            case "dialogue-lines" -> List.of(
                    "#", "1", "1", "你好，我是学生。", "ni hao, wo shi xue sheng",
                    "Hello, I am a student.", "Привет, я студент.", "101", "0", "3000"
            );
            case "dialogue-line-vocab" -> List.of("#", "1", "1001", "学生", "xue sheng", "student", "студент", "身份词");
            default -> List.of("#");
        };
    }

    private String toCsvLine(List<String> values) {
        return values.stream().map(this::escapeCsvValue).reduce((left, right) -> left + "," + right).orElse("");
    }

    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private List<List<String>> parseCsv(String content) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < content.length(); i++) {
            char current = content.charAt(i);
            if (current == '"') {
                if (quoted && i + 1 < content.length() && content.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                row.add(cell.toString());
                cell.setLength(0);
            } else if ((current == '\n' || current == '\r') && !quoted) {
                row.add(cell.toString());
                cell.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
                if (current == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') {
                    i++;
                }
            } else {
                cell.append(current);
            }
        }
        if (!quoted && (cell.length() > 0 || !row.isEmpty())) {
            row.add(cell.toString());
            rows.add(row);
        }
        if (quoted) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "CSV 引号没有闭合");
        }
        return rows;
    }

    private String normalizeType(String importType) {
        return switch (importType) {
            case "vocab-lists", "vocab-items", "exercise-sets", "sentence-exercises",
                    "video-materials", "dialogue-lines", "dialogue-line-vocab" -> importType;
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR, "不支持的导入类型：" + importType);
        };
    }

    private List<String> headersFor(String importType) {
        return switch (importType) {
            case "vocab-lists" -> VOCAB_LIST_HEADERS;
            case "vocab-items" -> VOCAB_ITEM_HEADERS;
            case "exercise-sets" -> EXERCISE_SET_HEADERS;
            case "sentence-exercises" -> SENTENCE_EXERCISE_HEADERS;
            case "video-materials" -> VIDEO_MATERIAL_HEADERS;
            case "dialogue-lines" -> DIALOGUE_LINE_HEADERS;
            case "dialogue-line-vocab" -> DIALOGUE_LINE_VOCAB_HEADERS;
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR, "不支持的导入类型：" + importType);
        };
    }

    private String text(Map<String, String> row, String field) {
        return row.getOrDefault(field, "").trim();
    }

    private String required(Map<String, String> row, String field) {
        String value = text(row, field);
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "字段 " + field + " 不能为空");
        }
        return value;
    }

    private Long requiredLong(Map<String, String> row, String field) {
        Long value = optionalLong(row, field);
        if (value == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "字段 " + field + " 不能为空");
        }
        return value;
    }

    private Long optionalLong(Map<String, String> row, String field) {
        String value = text(row, field);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "字段 " + field + " 必须是整数");
        }
    }

    private Integer requiredInt(Map<String, String> row, String field) {
        Integer value = optionalInt(row, field);
        if (value == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "字段 " + field + " 不能为空");
        }
        return value;
    }

    private Integer optionalInt(Map<String, String> row, String field) {
        String value = text(row, field);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "字段 " + field + " 必须是整数");
        }
    }

    private String enumValue(String value, Set<String> allowedValues, String field) {
        if (!allowedValues.contains(value)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "字段 " + field + " 的值不正确：" + value);
        }
        return value;
    }

    private String statusValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return enumValue(value.trim(), CONTENT_STATUSES, "status");
    }

    private List<AdminSentenceWordOptionDTO> parseWordOptions(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        List<String> words = Arrays.stream(value.split("\\|"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
        List<AdminSentenceWordOptionDTO> options = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            options.add(new AdminSentenceWordOptionDTO(words.get(i), i + 1));
        }
        return options;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        String value = header.trim();
        if (value.startsWith("\uFEFF")) {
            value = value.substring(1);
        }
        return value;
    }

    private void requirePermission(CurrentUser admin, String permission) {
        if (admin.permissions().contains("admin:*") || admin.permissions().contains(permission)) {
            return;
        }
        throw BusinessException.forbidden(ErrorCode.FORBIDDEN, "缺少后台权限：" + permission);
    }

    private void writeOperationLog(
            Long adminUserId,
            String action,
            String targetType,
            Long targetId,
            Map<String, Object> detail,
            String ipAddress
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminUserId(adminUserId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(toJson(detail));
        log.setIpAddress(ipAddress);
        log.setCreatedAt(now);
        log.setUpdatedAt(now);
        adminOperationLogMapper.insertLog(log);
    }

    private String toJson(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(new HashMap<>(detail));
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
