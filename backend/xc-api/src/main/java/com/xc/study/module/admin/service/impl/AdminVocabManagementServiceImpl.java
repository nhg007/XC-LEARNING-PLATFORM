package com.xc.study.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminBatchBindMediaAssetDTO;
import com.xc.study.module.admin.dto.AdminBatchUpdateContentStatusDTO;
import com.xc.study.module.admin.dto.AdminUpdateContentStatusDTO;
import com.xc.study.module.admin.dto.AdminUpsertVocabItemDTO;
import com.xc.study.module.admin.dto.AdminUpsertVocabListDTO;
import com.xc.study.module.admin.dto.AdminVocabItemQueryDTO;
import com.xc.study.module.admin.dto.AdminVocabListQueryDTO;
import com.xc.study.module.admin.entity.AdminOperationLog;
import com.xc.study.module.admin.mapper.AdminOperationLogMapper;
import com.xc.study.module.admin.service.AdminVocabManagementService;
import com.xc.study.module.admin.service.support.AdminSorts;
import com.xc.study.module.admin.vo.AdminBatchBindMediaAssetResultVO;
import com.xc.study.module.admin.vo.AdminBatchContentStatusResultVO;
import com.xc.study.module.admin.vo.AdminVocabItemVO;
import com.xc.study.module.admin.vo.AdminVocabListVO;
import com.xc.study.module.media.entity.MediaAsset;
import com.xc.study.module.media.mapper.MediaAssetMapper;
import com.xc.study.module.vocab.entity.VocabItem;
import com.xc.study.module.vocab.entity.VocabList;
import com.xc.study.module.vocab.mapper.VocabItemMapper;
import com.xc.study.module.vocab.mapper.VocabListMapper;
import com.xc.study.security.CurrentUser;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminVocabManagementServiceImpl implements AdminVocabManagementService {

    private final VocabListMapper vocabListMapper;
    private final VocabItemMapper vocabItemMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final ObjectMapper objectMapper;

    public AdminVocabManagementServiceImpl(
            VocabListMapper vocabListMapper,
            VocabItemMapper vocabItemMapper,
            MediaAssetMapper mediaAssetMapper,
            AdminOperationLogMapper adminOperationLogMapper,
            ObjectMapper objectMapper
    ) {
        this.vocabListMapper = vocabListMapper;
        this.vocabItemMapper = vocabItemMapper;
        this.mediaAssetMapper = mediaAssetMapper;
        this.adminOperationLogMapper = adminOperationLogMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public PageResult<AdminVocabListVO> pageLists(AdminVocabListQueryDTO query, CurrentUser admin) {
        requirePermission(admin, "admin:content:read");
        int page = query.getPage() == null ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        LambdaQueryWrapper<VocabList> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getListType())) {
            wrapper.eq(VocabList::getListType, query.getListType());
        }
        if (StringUtils.hasText(query.getLevel())) {
            wrapper.eq(VocabList::getLevel, query.getLevel().trim());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(VocabList::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(VocabList::getName, keyword)
                    .or()
                    .like(VocabList::getDescription, keyword)
                    .or()
                    .like(VocabList::getLevel, keyword));
        }
        boolean sorted = AdminSorts.apply(wrapper, query.getSortBy(), query.getSortDirection(), Map.of(
                "id", VocabList::getId,
                "name", VocabList::getName,
                "listType", VocabList::getListType,
                "level", VocabList::getLevel,
                "sortOrder", VocabList::getSortOrder,
                "status", VocabList::getStatus,
                "createdAt", VocabList::getCreatedAt,
                "updatedAt", VocabList::getUpdatedAt
        ));
        if (!sorted) {
            wrapper.orderByAsc(VocabList::getSortOrder).orderByDesc(VocabList::getUpdatedAt);
        }
        wrapper.orderByDesc(VocabList::getId);
        Page<VocabList> result = vocabListMapper.selectPage(Page.of(page, pageSize), wrapper);
        return PageResult.of(
                result.getRecords().stream().map(this::toListVO).toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }

    @Override
    @Transactional
    public AdminVocabListVO createList(AdminUpsertVocabListDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        OffsetDateTime now = OffsetDateTime.now();
        VocabList list = new VocabList();
        fillList(list, request);
        list.setStatus(StringUtils.hasText(request.status()) ? request.status() : "active");
        list.setCreatedAt(now);
        list.setUpdatedAt(now);
        vocabListMapper.insert(list);
        writeOperationLog(admin.id(), "content.vocab.list.create", "vocab_list", list.getId(), listSnapshot(list), ipAddress);
        return toListVO(list);
    }

    @Override
    @Transactional
    public AdminVocabListVO updateList(Long listId, AdminUpsertVocabListDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        VocabList list = requireList(listId);
        Map<String, Object> before = listSnapshot(list);
        fillList(list, request);
        if (StringUtils.hasText(request.status())) {
            list.setStatus(request.status());
        }
        list.setUpdatedAt(OffsetDateTime.now());
        vocabListMapper.updateById(list);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", before);
        detail.put("after", listSnapshot(list));
        writeOperationLog(admin.id(), "content.vocab.list.update", "vocab_list", listId, detail, ipAddress);
        return toListVO(list);
    }

    @Override
    @Transactional
    public AdminVocabListVO updateListStatus(Long listId, AdminUpdateContentStatusDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        VocabList list = requireList(listId);
        String beforeStatus = list.getStatus();
        list.setStatus(request.status());
        list.setUpdatedAt(OffsetDateTime.now());
        vocabListMapper.updateById(list);
        writeOperationLog(admin.id(), "content.vocab.list.status.update", "vocab_list", listId, Map.of(
                "beforeStatus", beforeStatus,
                "afterStatus", request.status(),
                "reason", request.reason() == null ? "" : request.reason()
        ), ipAddress);
        return toListVO(list);
    }

    @Override
    @Transactional
    public AdminBatchContentStatusResultVO updateListStatuses(
            AdminBatchUpdateContentStatusDTO request,
            CurrentUser admin,
            String ipAddress
    ) {
        requirePermission(admin, "admin:content:update");
        OffsetDateTime now = OffsetDateTime.now();
        List<String> errors = new ArrayList<>();
        List<Map<String, Object>> changes = new ArrayList<>();
        for (Long listId : request.ids()) {
            VocabList list = vocabListMapper.selectById(listId);
            if (list == null) {
                errors.add("词汇表 ID " + listId + " 不存在");
                continue;
            }
            String beforeStatus = list.getStatus();
            list.setStatus(request.status());
            list.setUpdatedAt(now);
            vocabListMapper.updateById(list);
            changes.add(Map.of(
                    "id", listId,
                    "beforeStatus", beforeStatus == null ? "" : beforeStatus,
                    "afterStatus", request.status()
            ));
        }
        writeOperationLog(admin.id(), "content.vocab.list.status.batch_update", "vocab_list", null, Map.of(
                "requestedCount", request.ids().size(),
                "successCount", changes.size(),
                "afterStatus", request.status(),
                "reason", request.reason() == null ? "" : request.reason(),
                "errors", errors,
                "changes", changes
        ), ipAddress);
        return new AdminBatchContentStatusResultVO(request.ids().size(), changes.size(), errors);
    }

    @Override
    public PageResult<AdminVocabItemVO> pageItems(AdminVocabItemQueryDTO query, CurrentUser admin) {
        requirePermission(admin, "admin:content:read");
        int page = query.getPage() == null ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        LambdaQueryWrapper<VocabItem> wrapper = new LambdaQueryWrapper<>();
        if (query.getVocabListId() != null) {
            wrapper.eq(VocabItem::getVocabListId, query.getVocabListId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(VocabItem::getStatus, query.getStatus());
        }
        if (query.getHasAudio() != null) {
            if (query.getHasAudio()) {
                wrapper.isNotNull(VocabItem::getAudioAssetId);
            } else {
                wrapper.isNull(VocabItem::getAudioAssetId);
            }
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(VocabItem::getHanzi, keyword)
                    .or()
                    .like(VocabItem::getPinyin, keyword)
                    .or()
                    .like(VocabItem::getMeaningEn, keyword)
                    .or()
                    .like(VocabItem::getMeaningRu, keyword));
        }
        boolean sorted = AdminSorts.apply(wrapper, query.getSortBy(), query.getSortDirection(), Map.of(
                "id", VocabItem::getId,
                "vocabListId", VocabItem::getVocabListId,
                "hanzi", VocabItem::getHanzi,
                "pinyin", VocabItem::getPinyin,
                "meaningEn", VocabItem::getMeaningEn,
                "meaningRu", VocabItem::getMeaningRu,
                "sortOrder", VocabItem::getSortOrder,
                "status", VocabItem::getStatus,
                "createdAt", VocabItem::getCreatedAt,
                "updatedAt", VocabItem::getUpdatedAt
        ));
        if (!sorted) {
            wrapper.orderByAsc(VocabItem::getSortOrder).orderByDesc(VocabItem::getUpdatedAt);
        }
        wrapper.orderByDesc(VocabItem::getId);
        Page<VocabItem> result = vocabItemMapper.selectPage(Page.of(page, pageSize), wrapper);
        return PageResult.of(toItemVOs(result.getRecords()), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public AdminVocabItemVO createItem(AdminUpsertVocabItemDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        requireList(request.vocabListId());
        OffsetDateTime now = OffsetDateTime.now();
        VocabItem item = new VocabItem();
        fillItem(item, request);
        item.setStatus(StringUtils.hasText(request.status()) ? request.status() : "active");
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        vocabItemMapper.insert(item);
        writeOperationLog(admin.id(), "content.vocab.item.create", "vocab_item", item.getId(), itemSnapshot(item), ipAddress);
        return toItemVOs(List.of(item)).get(0);
    }

    @Override
    @Transactional
    public AdminVocabItemVO updateItem(Long itemId, AdminUpsertVocabItemDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        requireList(request.vocabListId());
        VocabItem item = requireItem(itemId);
        Map<String, Object> before = itemSnapshot(item);
        fillItem(item, request);
        if (StringUtils.hasText(request.status())) {
            item.setStatus(request.status());
        }
        item.setUpdatedAt(OffsetDateTime.now());
        vocabItemMapper.updateById(item);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", before);
        detail.put("after", itemSnapshot(item));
        writeOperationLog(admin.id(), "content.vocab.item.update", "vocab_item", itemId, detail, ipAddress);
        return toItemVOs(List.of(item)).get(0);
    }

    @Override
    @Transactional
    public AdminVocabItemVO updateItemStatus(Long itemId, AdminUpdateContentStatusDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        VocabItem item = requireItem(itemId);
        String beforeStatus = item.getStatus();
        item.setStatus(request.status());
        item.setUpdatedAt(OffsetDateTime.now());
        vocabItemMapper.updateById(item);
        writeOperationLog(admin.id(), "content.vocab.item.status.update", "vocab_item", itemId, Map.of(
                "beforeStatus", beforeStatus,
                "afterStatus", request.status(),
                "reason", request.reason() == null ? "" : request.reason()
        ), ipAddress);
        return toItemVOs(List.of(item)).get(0);
    }

    @Override
    @Transactional
    public AdminBatchContentStatusResultVO updateItemStatuses(
            AdminBatchUpdateContentStatusDTO request,
            CurrentUser admin,
            String ipAddress
    ) {
        requirePermission(admin, "admin:content:update");
        OffsetDateTime now = OffsetDateTime.now();
        List<String> errors = new ArrayList<>();
        List<Map<String, Object>> changes = new ArrayList<>();
        for (Long itemId : request.ids()) {
            VocabItem item = vocabItemMapper.selectById(itemId);
            if (item == null) {
                errors.add("词汇 ID " + itemId + " 不存在");
                continue;
            }
            String beforeStatus = item.getStatus();
            item.setStatus(request.status());
            item.setUpdatedAt(now);
            vocabItemMapper.updateById(item);
            changes.add(Map.of(
                    "id", itemId,
                    "beforeStatus", beforeStatus == null ? "" : beforeStatus,
                    "afterStatus", request.status()
            ));
        }
        writeOperationLog(admin.id(), "content.vocab.item.status.batch_update", "vocab_item", null, Map.of(
                "requestedCount", request.ids().size(),
                "successCount", changes.size(),
                "afterStatus", request.status(),
                "reason", request.reason() == null ? "" : request.reason(),
                "errors", errors,
                "changes", changes
        ), ipAddress);
        return new AdminBatchContentStatusResultVO(request.ids().size(), changes.size(), errors);
    }

    @Override
    @Transactional
    public AdminBatchBindMediaAssetResultVO bindItemAudio(
            AdminBatchBindMediaAssetDTO request,
            CurrentUser admin,
            String ipAddress
    ) {
        requirePermission(admin, "admin:content:update");
        OffsetDateTime now = OffsetDateTime.now();
        List<String> errors = new ArrayList<>();
        List<Map<String, Object>> successfulBindings = new ArrayList<>();
        for (var binding : request.bindings()) {
            Long itemId = binding.targetId();
            Long mediaAssetId = binding.mediaAssetId();
            VocabItem item = vocabItemMapper.selectById(itemId);
            if (item == null) {
                errors.add("词汇 ID " + itemId + " 不存在");
                continue;
            }
            MediaAsset asset = mediaAssetMapper.selectById(mediaAssetId);
            if (!isActiveMediaAsset(asset, "audio")) {
                errors.add("词汇 ID " + itemId + " 的音频资源 ID " + mediaAssetId + " 不存在、已停用或类型不正确");
                continue;
            }
            Long beforeAudioAssetId = item.getAudioAssetId();
            item.setAudioAssetId(mediaAssetId);
            item.setUpdatedAt(now);
            vocabItemMapper.updateById(item);
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("targetId", itemId);
            detail.put("beforeAudioAssetId", beforeAudioAssetId);
            detail.put("afterAudioAssetId", mediaAssetId);
            successfulBindings.add(detail);
        }
        writeOperationLog(admin.id(), "content.vocab.item.audio.batch_bind", "vocab_item", null, Map.of(
                "requestedCount", request.bindings().size(),
                "successCount", successfulBindings.size(),
                "errors", errors,
                "bindings", successfulBindings
        ), ipAddress);
        return new AdminBatchBindMediaAssetResultVO(request.bindings().size(), successfulBindings.size(), errors);
    }

    private void fillList(VocabList list, AdminUpsertVocabListDTO request) {
        list.setName(request.name().trim());
        list.setListType(request.listType());
        list.setLevel(blankToNull(request.level()));
        list.setDescription(blankToNull(request.description()));
        list.setSortOrder(request.sortOrder());
    }

    private void fillItem(VocabItem item, AdminUpsertVocabItemDTO request) {
        validateAudioAsset(request.audioAssetId());
        item.setVocabListId(request.vocabListId());
        item.setHanzi(request.hanzi().trim());
        item.setPinyin(blankToNull(request.pinyin()));
        item.setMeaningEn(blankToNull(request.meaningEn()));
        item.setMeaningRu(blankToNull(request.meaningRu()));
        item.setExampleSentence(blankToNull(request.exampleSentence()));
        item.setAudioAssetId(request.audioAssetId());
        item.setSortOrder(request.sortOrder());
    }

    private AdminVocabListVO toListVO(VocabList list) {
        return new AdminVocabListVO(
                list.getId(),
                list.getName(),
                list.getListType(),
                list.getLevel(),
                list.getDescription(),
                list.getSortOrder(),
                list.getStatus(),
                countItems(list.getId(), "active"),
                countItems(list.getId(), "inactive"),
                list.getCreatedAt(),
                list.getUpdatedAt()
        );
    }

    private List<AdminVocabItemVO> toItemVOs(List<VocabItem> items) {
        if (items.isEmpty()) {
            return List.of();
        }
        Map<Long, VocabList> lists = loadLists(items.stream().map(VocabItem::getVocabListId).toList());
        Map<Long, MediaAsset> audioAssets = loadMediaAssets(items.stream().map(VocabItem::getAudioAssetId).toList());
        return items.stream()
                .map(item -> {
                    VocabList list = lists.get(item.getVocabListId());
                    MediaAsset audio = audioAssets.get(item.getAudioAssetId());
                    return new AdminVocabItemVO(
                            item.getId(),
                            item.getVocabListId(),
                            list == null ? null : list.getName(),
                            list == null ? null : list.getListType(),
                            list == null ? null : list.getLevel(),
                            item.getHanzi(),
                            item.getPinyin(),
                            item.getMeaningEn(),
                            item.getMeaningRu(),
                            item.getExampleSentence(),
                            item.getAudioAssetId(),
                            audio == null ? null : audio.getUrl(),
                            item.getSortOrder(),
                            item.getStatus(),
                            item.getCreatedAt(),
                            item.getUpdatedAt()
                    );
                })
                .toList();
    }

    private Map<Long, VocabList> loadLists(List<Long> ids) {
        List<Long> listIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (listIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return vocabListMapper.selectBatchIds(listIds)
                .stream()
                .collect(Collectors.toMap(VocabList::getId, Function.identity()));
    }

    private Map<Long, MediaAsset> loadMediaAssets(List<Long> ids) {
        List<Long> assetIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (assetIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return mediaAssetMapper.selectBatchIds(assetIds)
                .stream()
                .collect(Collectors.toMap(MediaAsset::getId, Function.identity()));
    }

    private void validateAudioAsset(Long audioAssetId) {
        if (audioAssetId == null) {
            return;
        }
        MediaAsset asset = mediaAssetMapper.selectById(audioAssetId);
        if (asset == null || !"audio".equals(asset.getMediaType()) || !"active".equals(asset.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "音频资源不存在、已停用或类型不正确");
        }
    }

    private boolean isActiveMediaAsset(MediaAsset asset, String mediaType) {
        return asset != null && mediaType.equals(asset.getMediaType()) && "active".equals(asset.getStatus());
    }

    private long countItems(Long listId, String status) {
        return vocabItemMapper.selectCount(new LambdaQueryWrapper<VocabItem>()
                .eq(VocabItem::getVocabListId, listId)
                .eq(VocabItem::getStatus, status));
    }

    private VocabList requireList(Long listId) {
        VocabList list = vocabListMapper.selectById(listId);
        if (list == null) {
            throw BusinessException.notFound("词汇表不存在");
        }
        return list;
    }

    private VocabItem requireItem(Long itemId) {
        VocabItem item = vocabItemMapper.selectById(itemId);
        if (item == null) {
            throw BusinessException.notFound("词汇不存在");
        }
        return item;
    }

    private Map<String, Object> listSnapshot(VocabList list) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", list.getName());
        snapshot.put("listType", list.getListType());
        snapshot.put("level", list.getLevel());
        snapshot.put("description", list.getDescription());
        snapshot.put("sortOrder", list.getSortOrder());
        snapshot.put("status", list.getStatus());
        return snapshot;
    }

    private Map<String, Object> itemSnapshot(VocabItem item) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("vocabListId", item.getVocabListId());
        snapshot.put("hanzi", item.getHanzi());
        snapshot.put("pinyin", item.getPinyin());
        snapshot.put("meaningEn", item.getMeaningEn());
        snapshot.put("meaningRu", item.getMeaningRu());
        snapshot.put("exampleSentence", item.getExampleSentence());
        snapshot.put("audioAssetId", item.getAudioAssetId());
        snapshot.put("sortOrder", item.getSortOrder());
        snapshot.put("status", item.getStatus());
        return snapshot;
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

    private String blankToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
