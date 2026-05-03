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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminContentImportServiceImpl implements AdminContentImportService {

    private static final Logger log = LoggerFactory.getLogger(AdminContentImportServiceImpl.class);

    private static final int MAX_ROWS = 1000;
    private static final long MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024;

    private static final List<ImportColumn> VOCAB_LIST_COLUMNS = List.of(
            column("id", "记录ID（更新时填）", "记录ID"),
            column("name", "词表名称", "名称"),
            column("list_type", "词表类型", "类型"),
            column("level", "级别/分类", "级别", "等级"),
            column("description", "描述", "说明"),
            column("sort_order", "排序", "排序值"),
            column("status", "状态", "启用状态")
    );
    private static final List<ImportColumn> VOCAB_ITEM_COLUMNS = List.of(
            column("id", "记录ID（更新时填）", "记录ID"),
            column("vocab_list_id", "所属词表ID", "词表ID", "所属词表"),
            column("hanzi", "汉字/词语", "汉字", "词语"),
            column("pinyin", "拼音"),
            column("meaning_en", "英文释义", "英语释义"),
            column("meaning_ru", "俄文释义", "俄语释义"),
            column("example_sentence", "例句", "示例句"),
            column("audio_asset_id", "音频素材ID（可选）", "音频素材ID", "音频资源ID"),
            column("sort_order", "排序", "排序值"),
            column("status", "状态", "启用状态")
    );
    private static final List<ImportColumn> EXERCISE_SET_COLUMNS = List.of(
            column("id", "记录ID（更新时填）", "记录ID"),
            column("title", "题组标题", "标题", "名称"),
            column("exercise_type", "题型", "练习类型"),
            column("level", "级别/分类", "级别", "等级"),
            column("status", "状态", "启用状态")
    );
    private static final List<ImportColumn> SENTENCE_EXERCISE_COLUMNS = List.of(
            column("id", "记录ID（更新时填）", "记录ID"),
            column("exercise_set_id", "所属题组ID", "题组ID", "所属题组"),
            column("exercise_type", "题型", "练习类型"),
            column("hanzi_answer", "汉语答案", "答案"),
            column("pinyin_prompt", "拼音提示"),
            column("translation_en", "英文提示", "英语提示"),
            column("translation_ru", "俄文提示", "俄语提示"),
            column("audio_zh_asset_id", "中文音频素材ID（可选）", "中文音频素材ID", "音频素材ID", "音频资源ID"),
            column("explanation", "解析", "说明"),
            column("sort_order", "排序", "排序值"),
            column("status", "状态", "启用状态"),
            column("word_options", "排序词块（用|分隔）", "排序词块", "词块")
    );
    private static final List<ImportColumn> VIDEO_MATERIAL_COLUMNS = List.of(
            column("id", "记录ID（更新时填）", "记录ID"),
            column("title", "材料标题", "标题", "名称"),
            column("material_type", "材料类型", "类型"),
            column("description", "描述", "说明"),
            column("cover_asset_id", "封面素材ID（可选）", "封面素材ID", "封面资源ID"),
            column("status", "状态", "启用状态")
    );
    private static final List<ImportColumn> DIALOGUE_LINE_COLUMNS = List.of(
            column("id", "记录ID（更新时填）", "记录ID"),
            column("material_id", "所属材料ID", "材料ID", "所属材料"),
            column("line_no", "行号"),
            column("hanzi_text", "汉语台词", "台词", "汉语文本"),
            column("pinyin_text", "拼音"),
            column("translation_en", "英文翻译", "英语翻译"),
            column("translation_ru", "俄文翻译", "俄语翻译"),
            column("audio_asset_id", "音频素材ID（可选）", "音频素材ID", "音频资源ID"),
            column("start_ms", "开始时间（毫秒）", "开始时间"),
            column("end_ms", "结束时间（毫秒）", "结束时间")
    );
    private static final List<ImportColumn> DIALOGUE_LINE_VOCAB_COLUMNS = List.of(
            column("id", "记录ID（更新时填）", "记录ID"),
            column("dialogue_line_id", "所属台词行ID", "台词行ID", "所属台词行"),
            column("vocab_item_id", "关联词汇ID（可选）", "关联词汇ID", "词汇ID"),
            column("word_text", "词语", "词汇"),
            column("pinyin", "拼音"),
            column("meaning_en", "英文释义", "英语释义"),
            column("meaning_ru", "俄文释义", "俄语释义"),
            column("explanation", "补充说明", "说明", "解析")
    );

    private static final Set<String> CONTENT_STATUSES = Set.of("active", "inactive");
    private static final Set<String> VOCAB_LIST_TYPES = Set.of("HSK", "YCT", "category", "professional", "custom");
    private static final Set<String> EXERCISE_TYPES = Set.of("audio_order", "audio_dictation", "pinyin_dictation", "translation_order");
    private static final Set<String> MATERIAL_TYPES = Set.of("drama", "short_video", "cartoon");
    private static final Set<String> OPTIONAL_MEDIA_FIELDS = Set.of("audio_asset_id", "audio_zh_asset_id", "cover_asset_id");
    private static final Map<String, String> CONTENT_STATUS_ALIASES = Map.of(
            "启用", "active",
            "已启用", "active",
            "有效", "active",
            "停用", "inactive",
            "已停用", "inactive",
            "禁用", "inactive"
    );
    private static final Map<String, String> VOCAB_LIST_TYPE_ALIASES = Map.of(
            "分类", "category",
            "专业词汇", "professional",
            "自定义", "custom"
    );
    private static final Map<String, String> EXERCISE_TYPE_ALIASES = Map.of(
            "听音排序", "audio_order",
            "听音频排序", "audio_order",
            "听写", "audio_dictation",
            "听写汉字", "audio_dictation",
            "拼音写句", "pinyin_dictation",
            "看拼音写汉字", "pinyin_dictation",
            "按拼音排序", "translation_order"
    );
    private static final Map<String, String> MATERIAL_TYPE_ALIASES = Map.of(
            "剧集", "drama",
            "短视频", "short_video",
            "动画", "cartoon"
    );

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

    private record ImportColumn(String field, String label, List<String> aliases) {

        boolean matches(String header) {
            return aliases.contains(normalizeHeaderValue(header));
        }
    }

    private record ImportContext(String field, String name) {
    }

    private record ParsedImportRow(int rowNumber, Map<String, String> values) {
    }

    private static ImportColumn column(String field, String label, String... aliases) {
        List<String> allAliases = new ArrayList<>();
        allAliases.add(field);
        allAliases.add(label);
        allAliases.addAll(Arrays.asList(aliases));
        return new ImportColumn(field, label, allAliases.stream()
                .map(AdminContentImportServiceImpl::normalizeHeaderValue)
                .distinct()
                .toList());
    }

    @Override
    public AdminContentImportTemplateVO template(String importType, Long contextId, String contextName, CurrentUser admin) {
        requirePermission(admin, "admin:content:read");
        String type = normalizeType(importType);
        StringBuilder builder = new StringBuilder("\uFEFF");
        List<ImportColumn> columns = templateColumnsFor(type, contextId);
        builder.append(toCsvLine(columns.stream().map(ImportColumn::label).toList())).append("\n");
        for (List<String> row : templateCommentRows(type, contextId, contextName)) {
            builder.append(toCsvLine(row)).append("\n");
        }
        return new AdminContentImportTemplateVO(templateFilename(type), builder.toString());
    }

    @Override
    public AdminContentImportResultVO importCsv(
            String importType,
            MultipartFile file,
            Long contextId,
            CurrentUser admin,
            String ipAddress
    ) {
        requirePermission(admin, "admin:content:update");
        String type = normalizeType(importType);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "请选择 CSV 文件");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "CSV 文件不能超过 2MB");
        }
        validateCsvFilename(file);
        List<ParsedImportRow> rows = parseRows(file, requiredHeaderColumnsFor(type, contextId), columnsFor(type));
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        for (ParsedImportRow row : rows) {
            try {
                importRow(type, row.values(), contextId, admin, ipAddress);
                successCount++;
            } catch (BusinessException ex) {
                errors.add(rowErrorPrefix(row) + ex.getMessage());
            } catch (RuntimeException ex) {
                log.warn("CSV import row failed. type={}, rowNumber={}, adminUserId={}", type, row.rowNumber(), admin.id(), ex);
                errors.add(rowErrorPrefix(row) + "导入失败，请检查这一行是否符合页面表单规则；如仍失败，请联系管理员查看日志");
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

    private void importRow(String type, Map<String, String> row, Long contextId, CurrentUser admin, String ipAddress) {
        applyImportContext(type, row, contextId);
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
                vocabListTypeValue(required(row, "list_type")),
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
                exerciseTypeValue(required(row, "exercise_type")),
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
                exerciseTypeValue(required(row, "exercise_type")),
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
                materialTypeValue(required(row, "material_type")),
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

    private List<ParsedImportRow> parseRows(
            MultipartFile file,
            List<ImportColumn> requiredColumns,
            List<ImportColumn> allColumns
    ) {
        List<List<String>> csvRows;
        try {
            csvRows = parseCsv(new String(file.getBytes(), StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "读取 CSV 文件失败");
        }
        if (csvRows.stream().noneMatch(row -> row.stream().anyMatch(StringUtils::hasText))) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "CSV 文件没有表头");
        }
        int headerRowIndex = firstContentRowIndex(csvRows);
        List<String> headers = csvRows.get(headerRowIndex).stream().map(AdminContentImportServiceImpl::normalizeHeaderValue).toList();
        Map<String, Integer> headerIndexByField = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            for (ImportColumn column : allColumns) {
                if (column.matches(header)) {
                    headerIndexByField.putIfAbsent(column.field(), i);
                    break;
                }
            }
        }
        for (ImportColumn requiredColumn : requiredColumns) {
            if (!headerIndexByField.containsKey(requiredColumn.field())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "CSV 缺少表头：" + requiredColumn.label()
                                + "。请下载最新模板，并确认第一行表头没有被删除或改名。当前识别到的表头："
                                + recognizedHeaders(headers));
            }
        }
        List<ParsedImportRow> rows = new ArrayList<>();
        for (int i = headerRowIndex + 1; i < csvRows.size(); i++) {
            List<String> values = csvRows.get(i);
            if (values.stream().noneMatch(StringUtils::hasText)) {
                continue;
            }
            if (!values.isEmpty() && normalizeHeader(values.get(0)).startsWith("#")) {
                continue;
            }
            Map<String, String> row = new LinkedHashMap<>();
            for (ImportColumn column : allColumns) {
                Integer columnIndex = headerIndexByField.get(column.field());
                row.put(column.field(), columnIndex != null && columnIndex < values.size() ? values.get(columnIndex).trim() : "");
            }
            rows.add(new ParsedImportRow(i + 1, row));
        }
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "CSV 文件没有可导入的数据行");
        }
        if (rows.size() > MAX_ROWS) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "一次最多导入 " + MAX_ROWS + " 行");
        }
        return rows;
    }

    private void validateCsvFilename(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (StringUtils.hasText(filename) && !filename.toLowerCase().endsWith(".csv")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "文件格式不正确：请上传 .csv 文件，当前文件是“" + filename + "”");
        }
    }

    private int firstContentRowIndex(List<List<String>> csvRows) {
        for (int i = 0; i < csvRows.size(); i++) {
            if (csvRows.get(i).stream().anyMatch(StringUtils::hasText)) {
                return i;
            }
        }
        return 0;
    }

    private String recognizedHeaders(List<String> headers) {
        List<String> visibleHeaders = headers.stream()
                .filter(StringUtils::hasText)
                .toList();
        if (visibleHeaders.isEmpty()) {
            return "未识别到任何表头";
        }
        return String.join("、", visibleHeaders);
    }

    private String rowErrorPrefix(ParsedImportRow row) {
        String displayName = rowDisplayName(row.values());
        if (StringUtils.hasText(displayName)) {
            return "第 " + row.rowNumber() + " 行（" + displayName + "）：";
        }
        return "第 " + row.rowNumber() + " 行：";
    }

    private String rowDisplayName(Map<String, String> row) {
        for (String field : List.of(
                "id",
                "name",
                "hanzi",
                "title",
                "hanzi_answer",
                "hanzi_text",
                "word_text"
        )) {
            String value = text(row, field);
            if (StringUtils.hasText(value)) {
                return labelFor(field) + "=" + value;
            }
        }
        return "";
    }

    private void applyImportContext(String importType, Map<String, String> row, Long contextId) {
        ImportContext context = contextFor(importType);
        if (context == null || contextId == null) {
            return;
        }
        Long csvContextId = optionalLong(row, context.field());
        if (csvContextId != null && !csvContextId.equals(contextId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "CSV 中的“" + labelFor(context.field()) + "”是 " + csvContextId
                            + "，但本次选择的" + context.name() + "是 " + contextId
                            + "。请删除该列内容，或重新选择正确的" + context.name());
        }
        row.put(context.field(), String.valueOf(contextId));
    }

    private List<List<String>> templateCommentRows(String importType, Long contextId, String contextName) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(templateNote("说明：第一行是业务列名，请不要修改；记录ID留空表示新增，有值表示更新对应记录。"));
        rows.add(templateNote("说明：导入时会自动忽略第一列以 # 开头的说明或示例行。"));
        ImportContext context = contextFor(importType);
        if (context != null && contextId != null) {
            rows.add(templateNote("所属" + context.name() + "：" + contextDisplayName(contextId, contextName)
                    + "；CSV 不需要再填写" + labelFor(context.field()) + "。"));
        } else if (context != null) {
            rows.add(templateNote("建议从具体" + context.name() + "筛选后下载模板，系统会自动带入所属" + context.name()
                    + "；当前模板需填写“" + labelFor(context.field()) + "”。"));
        }
        rows.add(templateNote("枚举：状态可填 启用/停用 或 active/inactive；空值按业务默认处理。"));
        String enumNote = enumTemplateNote(importType);
        if (StringUtils.hasText(enumNote)) {
            rows.add(templateNote(enumNote));
        }
        String mediaNote = mediaTemplateNote(importType);
        if (StringUtils.hasText(mediaNote)) {
            rows.add(templateNote(mediaNote));
        }
        rows.add(templateNote("示例：可以复制示例行，把第一列 # 改为空或真实记录ID后再导入。"));
        rows.add(sampleRow(importType, contextId != null));
        return rows;
    }

    private List<String> templateNote(String note) {
        List<String> row = new ArrayList<>();
        row.add("# " + note);
        return row;
    }

    private List<String> sampleRow(String importType, boolean hasContext) {
        return switch (importType) {
            case "vocab-lists" -> List.of("#", "HSK1 基础词汇", "HSK", "HSK1", "基础词汇", "0", "active");
            case "vocab-items" -> hasContext
                    ? List.of("#", "中国", "zhong guo", "China", "Китай", "我来自中国。", "", "0", "active")
                    : List.of("#", "1", "中国", "zhong guo", "China", "Китай", "我来自中国。", "", "0", "active");
            case "exercise-sets" -> List.of("#", "HSK1 听音频排序", "audio_order", "HSK1", "active");
            case "sentence-exercises" -> hasContext
                    ? List.of(
                            "#", "audio_order", "我是学生", "wo shi xue sheng", "I am a student",
                            "Я студент", "", "主语 + 是 + 身份", "0", "active", "我|是|学生"
                    )
                    : List.of(
                            "#", "1", "audio_order", "我是学生", "wo shi xue sheng", "I am a student",
                            "Я студент", "", "主语 + 是 + 身份", "0", "active", "我|是|学生"
                    );
            case "video-materials" -> List.of("#", "Demo HSK1 日常对话", "short_video", "日常问候片段", "", "active");
            case "dialogue-lines" -> hasContext
                    ? List.of(
                            "#", "1", "你好，我是学生。", "ni hao, wo shi xue sheng",
                            "Hello, I am a student.", "Привет, я студент.", "", "0", "3000"
                    )
                    : List.of(
                            "#", "1", "1", "你好，我是学生。", "ni hao, wo shi xue sheng",
                            "Hello, I am a student.", "Привет, я студент.", "", "0", "3000"
                    );
            case "dialogue-line-vocab" -> hasContext
                    ? List.of("#", "1001", "学生", "xue sheng", "student", "студент", "身份词")
                    : List.of("#", "1", "1001", "学生", "xue sheng", "student", "студент", "身份词");
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
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "CSV 格式错误：存在未闭合的英文双引号。请检查包含逗号、换行或引号的单元格是否正确成对使用双引号");
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

    private List<ImportColumn> columnsFor(String importType) {
        return switch (importType) {
            case "vocab-lists" -> VOCAB_LIST_COLUMNS;
            case "vocab-items" -> VOCAB_ITEM_COLUMNS;
            case "exercise-sets" -> EXERCISE_SET_COLUMNS;
            case "sentence-exercises" -> SENTENCE_EXERCISE_COLUMNS;
            case "video-materials" -> VIDEO_MATERIAL_COLUMNS;
            case "dialogue-lines" -> DIALOGUE_LINE_COLUMNS;
            case "dialogue-line-vocab" -> DIALOGUE_LINE_VOCAB_COLUMNS;
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR, "不支持的导入类型：" + importType);
        };
    }

    private List<ImportColumn> templateColumnsFor(String importType, Long contextId) {
        ImportContext context = contextFor(importType);
        if (context == null || contextId == null) {
            return columnsFor(importType);
        }
        return columnsFor(importType).stream()
                .filter(column -> !column.field().equals(context.field()))
                .toList();
    }

    private List<ImportColumn> requiredHeaderColumnsFor(String importType, Long contextId) {
        return templateColumnsFor(importType, contextId).stream()
                .filter(column -> !OPTIONAL_MEDIA_FIELDS.contains(column.field()))
                .toList();
    }

    private ImportContext contextFor(String importType) {
        return switch (importType) {
            case "vocab-items" -> new ImportContext("vocab_list_id", "词表");
            case "sentence-exercises" -> new ImportContext("exercise_set_id", "题组");
            case "dialogue-lines" -> new ImportContext("material_id", "台词材料");
            case "dialogue-line-vocab" -> new ImportContext("dialogue_line_id", "台词行");
            default -> null;
        };
    }

    private String labelFor(String field) {
        for (String importType : List.of(
                "vocab-lists",
                "vocab-items",
                "exercise-sets",
                "sentence-exercises",
                "video-materials",
                "dialogue-lines",
                "dialogue-line-vocab"
        )) {
            for (ImportColumn column : columnsFor(importType)) {
                if (column.field().equals(field)) {
                    return column.label();
                }
            }
        }
        return field;
    }

    private String contextDisplayName(Long contextId, String contextName) {
        if (StringUtils.hasText(contextName)) {
            return contextName.trim() + "（ID: " + contextId + "）";
        }
        return "ID: " + contextId;
    }

    private String enumTemplateNote(String importType) {
        return switch (importType) {
            case "vocab-lists" -> "词表类型可填 HSK、YCT、分类、专业词汇、自定义，或 category/professional/custom。";
            case "exercise-sets", "sentence-exercises" ->
                    "题型可填 听音频排序、听写汉字、看拼音写汉字、按拼音排序，或对应英文枚举。";
            case "video-materials" -> "材料类型可填 剧集、短视频、动画，或 drama/short_video/cartoon。";
            default -> "";
        };
    }

    private String mediaTemplateNote(String importType) {
        return switch (importType) {
            case "vocab-items", "sentence-exercises", "dialogue-lines" ->
                    "音频素材ID可留空；导入后可在列表中上传媒体并手动或批量绑定。";
            case "video-materials" -> "封面素材ID可留空；导入后可在列表中上传图片并手动或批量绑定。";
            default -> "";
        };
    }

    private String templateFilename(String importType) {
        return switch (importType) {
            case "vocab-lists" -> "词汇表导入模板.csv";
            case "vocab-items" -> "词汇条目导入模板.csv";
            case "exercise-sets" -> "题组导入模板.csv";
            case "sentence-exercises" -> "句子题导入模板.csv";
            case "video-materials" -> "台词材料导入模板.csv";
            case "dialogue-lines" -> "台词行导入模板.csv";
            case "dialogue-line-vocab" -> "词汇解析导入模板.csv";
            default -> importType + "-template.csv";
        };
    }

    private String text(Map<String, String> row, String field) {
        return row.getOrDefault(field, "").trim();
    }

    private String required(Map<String, String> row, String field) {
        String value = text(row, field);
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "字段“" + labelFor(field) + "”不能为空，请填写后再导入");
        }
        return value;
    }

    private Long requiredLong(Map<String, String> row, String field) {
        Long value = optionalLong(row, field);
        if (value == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "字段“" + labelFor(field) + "”不能为空，请填写整数 ID");
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
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "字段“" + labelFor(field) + "”必须是整数，当前值是“" + value + "”");
        }
    }

    private Integer requiredInt(Map<String, String> row, String field) {
        Integer value = optionalInt(row, field);
        if (value == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "字段“" + labelFor(field) + "”不能为空，请填写整数");
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
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "字段“" + labelFor(field) + "”必须是整数，当前值是“" + value + "”");
        }
    }

    private String enumValue(String value, Set<String> allowedValues, String field) {
        if (!allowedValues.contains(value)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "字段“" + labelFor(field) + "”的值不正确："
                            + (StringUtils.hasText(value) ? "“" + value + "”" : "空值")
                            + "。可填写：" + allowedValueHint(field));
        }
        return value;
    }

    private String mappedEnumValue(String value, Map<String, String> aliases, Set<String> allowedValues, String field) {
        String normalized = value.trim();
        String canonical = aliases.get(normalized);
        if (canonical != null) {
            return canonical;
        }
        return enumValue(normalized, allowedValues, field);
    }

    private String vocabListTypeValue(String value) {
        return mappedEnumValue(value, VOCAB_LIST_TYPE_ALIASES, VOCAB_LIST_TYPES, "list_type");
    }

    private String exerciseTypeValue(String value) {
        return mappedEnumValue(value, EXERCISE_TYPE_ALIASES, EXERCISE_TYPES, "exercise_type");
    }

    private String materialTypeValue(String value) {
        return mappedEnumValue(value, MATERIAL_TYPE_ALIASES, MATERIAL_TYPES, "material_type");
    }

    private String statusValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return mappedEnumValue(value, CONTENT_STATUS_ALIASES, CONTENT_STATUSES, "status");
    }

    private String allowedValueHint(String field) {
        return switch (field) {
            case "status" -> "启用、停用、active、inactive";
            case "list_type" -> "HSK、YCT、分类、专业词汇、自定义、category、professional、custom";
            case "exercise_type" -> "听音频排序、听写汉字、看拼音写汉字、按拼音排序、audio_order、audio_dictation、pinyin_dictation、translation_order";
            case "material_type" -> "剧集、短视频、动画、drama、short_video、cartoon";
            default -> "请参考下载模板中的说明";
        };
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
        return normalizeHeaderValue(header);
    }

    private static String normalizeHeaderValue(String header) {
        if (header == null) {
            return "";
        }
        String value = header.trim();
        if (value.startsWith("\uFEFF")) {
            value = value.substring(1);
        }
        return value.trim();
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
