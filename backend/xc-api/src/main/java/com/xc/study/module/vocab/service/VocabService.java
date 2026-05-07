package com.xc.study.module.vocab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.xc.study.common.BusinessException;
import com.xc.study.common.PageParams;
import com.xc.study.common.PageResult;
import com.xc.study.common.cache.MasterDataCache;
import com.xc.study.module.stats.service.LearningStatsRecorder;
import com.xc.study.module.media.entity.MediaAsset;
import com.xc.study.module.media.mapper.MediaAssetMapper;
import com.xc.study.module.vocab.dto.UpdateVocabProgressRequest;
import com.xc.study.module.vocab.entity.UserVocabFavorite;
import com.xc.study.module.vocab.entity.UserVocabItemProgress;
import com.xc.study.module.vocab.entity.UserVocabProgress;
import com.xc.study.module.vocab.entity.VocabItem;
import com.xc.study.module.vocab.entity.VocabList;
import com.xc.study.module.vocab.mapper.UserVocabFavoriteMapper;
import com.xc.study.module.vocab.mapper.UserVocabItemProgressMapper;
import com.xc.study.module.vocab.mapper.UserVocabProgressMapper;
import com.xc.study.module.vocab.mapper.VocabItemMapper;
import com.xc.study.module.vocab.mapper.VocabListMapper;
import com.xc.study.module.vocab.vo.FavoriteStatusVO;
import com.xc.study.module.vocab.vo.VocabItemVO;
import com.xc.study.module.vocab.vo.VocabListVO;
import com.xc.study.module.vocab.vo.VocabProgressVO;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VocabService {

    private static final String ITEM_STATUS_LEARNING = "learning";
    private static final String ITEM_STATUS_LEARNED = "learned";
    private static final String ITEM_STATUS_REVIEWING = "reviewing";
    private static final String ITEM_STATUS_MASTERED = "mastered";
    private static final Set<String> LEARNED_ITEM_STATUSES = Set.of(
            ITEM_STATUS_LEARNED,
            ITEM_STATUS_REVIEWING,
            ITEM_STATUS_MASTERED
    );

    private static final TypeReference<PageResult<VocabListVO>> VOCAB_LIST_PAGE_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<VocabListVO> VOCAB_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<PageResult<VocabItemVO>> VOCAB_ITEM_PAGE_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<VocabItemVO> VOCAB_ITEM_TYPE = new TypeReference<>() {
    };

    private final VocabListMapper vocabListMapper;
    private final VocabItemMapper vocabItemMapper;
    private final UserVocabProgressMapper userVocabProgressMapper;
    private final UserVocabItemProgressMapper userVocabItemProgressMapper;
    private final UserVocabFavoriteMapper userVocabFavoriteMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final LearningStatsRecorder learningStatsRecorder;
    private final MasterDataCache masterDataCache;

    public VocabService(
            VocabListMapper vocabListMapper,
            VocabItemMapper vocabItemMapper,
            UserVocabProgressMapper userVocabProgressMapper,
            UserVocabItemProgressMapper userVocabItemProgressMapper,
            UserVocabFavoriteMapper userVocabFavoriteMapper,
            MediaAssetMapper mediaAssetMapper,
            LearningStatsRecorder learningStatsRecorder,
            MasterDataCache masterDataCache
    ) {
        this.vocabListMapper = vocabListMapper;
        this.vocabItemMapper = vocabItemMapper;
        this.userVocabProgressMapper = userVocabProgressMapper;
        this.userVocabItemProgressMapper = userVocabItemProgressMapper;
        this.userVocabFavoriteMapper = userVocabFavoriteMapper;
        this.mediaAssetMapper = mediaAssetMapper;
        this.learningStatsRecorder = learningStatsRecorder;
        this.masterDataCache = masterDataCache;
    }

    public PageResult<VocabListVO> listLists(long page, long pageSize, String listType, String level, Long parentId) {
        PageParams params = PageParams.normalize(page, pageSize);
        return masterDataCache.get(
                listCacheKey(params.page(), params.pageSize(), listType, level, parentId),
                VOCAB_LIST_PAGE_TYPE,
                () -> loadLists(params.page(), params.pageSize(), listType, level, parentId)
        );
    }

    private PageResult<VocabListVO> loadLists(long page, long pageSize, String listType, String level, Long parentId) {
        if (parentId != null) {
            ensureActiveListHierarchy(parentId);
        }
        Page<VocabList> result = vocabListMapper.selectPage(Page.of(page, pageSize), new LambdaQueryWrapper<VocabList>()
                .eq(VocabList::getStatus, "active")
                .isNull(parentId == null, VocabList::getParentId)
                .eq(parentId != null, VocabList::getParentId, parentId)
                .eq(listType != null && !listType.isBlank(), VocabList::getListType, listType)
                .eq(level != null && !level.isBlank(), VocabList::getLevel, level)
                .orderByAsc(VocabList::getSortOrder)
                .orderByAsc(VocabList::getId));
        return PageResult.of(result.getRecords().stream().map(this::toListVO).toList(), result.getTotal(), page, pageSize);
    }

    public VocabListVO getList(Long vocabListId) {
        return masterDataCache.get(
                listDetailCacheKey(vocabListId),
                VOCAB_LIST_TYPE,
                () -> loadList(vocabListId)
        );
    }

    private VocabListVO loadList(Long vocabListId) {
        VocabList list = ensureActiveListHierarchy(vocabListId);
        return toListVO(list);
    }

    public PageResult<VocabItemVO> listItems(Long userId, Long vocabListId, long page, long pageSize) {
        PageParams params = PageParams.normalize(page, pageSize);
        PageResult<VocabItemVO> itemPage = masterDataCache.get(
                itemCacheKey(vocabListId, params.page(), params.pageSize()),
                VOCAB_ITEM_PAGE_TYPE,
                () -> loadItems(vocabListId, params.page(), params.pageSize())
        );
        return withUserState(userId, itemPage);
    }

    private PageResult<VocabItemVO> loadItems(Long vocabListId, long page, long pageSize) {
        List<VocabList> scopeLists = resolveActiveListScope(vocabListId);
        List<Long> scopeIds = scopeLists.stream().map(VocabList::getId).toList();
        if (scopeIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, pageSize);
        }
        Map<Long, Integer> listOrder = new HashMap<>();
        for (int i = 0; i < scopeIds.size(); i++) {
            listOrder.put(scopeIds.get(i), i);
        }
        List<VocabItem> allItems = vocabItemMapper.selectList(new LambdaQueryWrapper<VocabItem>()
                        .in(VocabItem::getVocabListId, scopeIds)
                        .eq(VocabItem::getStatus, "active"))
                .stream()
                .sorted(Comparator
                        .comparing((VocabItem item) -> listOrder.getOrDefault(item.getVocabListId(), Integer.MAX_VALUE))
                        .thenComparing(item -> item.getSortOrder() == null ? Integer.MAX_VALUE : item.getSortOrder())
                        .thenComparing(VocabItem::getId))
                .toList();
        List<VocabItem> pageItems = slice(allItems, page, pageSize);
        Map<Long, MediaAsset> audioAssets = loadMediaAssets(pageItems.stream().map(VocabItem::getAudioAssetId).toList());
        return PageResult.of(pageItems.stream()
                .map(item -> toItemVO(item, false, audioAssets.get(item.getAudioAssetId())))
                .toList(), allItems.size(), page, pageSize);
    }

    public VocabItemVO getItem(Long userId, Long vocabItemId) {
        VocabItemVO item = masterDataCache.get(
                itemDetailCacheKey(vocabItemId),
                VOCAB_ITEM_TYPE,
                () -> loadItem(vocabItemId)
        );
        return withUserState(userId, PageResult.of(List.of(item), 1, 1, 1)).records().get(0);
    }

    private VocabItemVO loadItem(Long vocabItemId) {
        VocabItem item = vocabItemMapper.selectById(vocabItemId);
        if (item == null || !"active".equals(item.getStatus())) {
            throw BusinessException.notFound("词汇不存在");
        }
        MediaAsset audioAsset = item.getAudioAssetId() == null ? null : loadMediaAssets(List.of(item.getAudioAssetId())).get(item.getAudioAssetId());
        return toItemVO(item, false, audioAsset);
    }

    public VocabProgressVO getProgress(Long userId, Long vocabListId) {
        ensureActiveListHierarchy(vocabListId);
        long totalCount = countActiveItems(vocabListId);
        long learnedCount = countLearnedItems(userId, vocabListId);
        long reviewingCount = countItemsByStatus(userId, vocabListId, ITEM_STATUS_REVIEWING);
        long masteredCount = countItemsByStatus(userId, vocabListId, ITEM_STATUS_MASTERED);
        UserVocabProgress progress = userVocabProgressMapper.selectOne(new LambdaQueryWrapper<UserVocabProgress>()
                .eq(UserVocabProgress::getUserId, userId)
                .eq(UserVocabProgress::getVocabListId, vocabListId)
                .last("limit 1"));
        if (progress == null) {
            return new VocabProgressVO(
                    vocabListId,
                    0,
                    null,
                    Math.toIntExact(learnedCount),
                    Math.toIntExact(learnedCount),
                    Math.toIntExact(reviewingCount),
                    Math.toIntExact(masteredCount),
                    totalCount
            );
        }
        int reviewedCount = learnedCount == 0 ? safeInt(progress.getReviewedCount()) : Math.toIntExact(learnedCount);
        return new VocabProgressVO(
                vocabListId,
                progress.getCurrentIndex(),
                progress.getLastVocabItemId(),
                reviewedCount,
                Math.toIntExact(learnedCount),
                Math.toIntExact(reviewingCount),
                Math.toIntExact(masteredCount),
                totalCount
        );
    }

    @Transactional
    public VocabProgressVO updateProgress(Long userId, Long vocabListId, UpdateVocabProgressRequest request) {
        ensureActiveListHierarchy(vocabListId);
        validateLastItem(vocabListId, request.lastVocabItemId());

        UserVocabProgress progress = userVocabProgressMapper.selectOne(new LambdaQueryWrapper<UserVocabProgress>()
                .eq(UserVocabProgress::getUserId, userId)
                .eq(UserVocabProgress::getVocabListId, vocabListId)
                .last("limit 1"));
        updateItemProgress(userId, vocabListId, request.lastVocabItemId(), request.itemStatus());
        long learnedCount = countLearnedItems(userId, vocabListId);
        int requestedReviewedCount = request.reviewedCount() == null ? request.currentIndex() : request.reviewedCount();
        int reviewedCount = Math.toIntExact(Math.max(learnedCount, requestedReviewedCount));
        if (progress == null) {
            progress = new UserVocabProgress();
            progress.setUserId(userId);
            progress.setVocabListId(vocabListId);
            progress.setCurrentIndex(request.currentIndex());
            progress.setLastVocabItemId(request.lastVocabItemId());
            progress.setReviewedCount(reviewedCount);
            userVocabProgressMapper.insert(progress);
        } else {
            progress.setCurrentIndex(request.currentIndex());
            progress.setLastVocabItemId(request.lastVocabItemId());
            progress.setReviewedCount(Math.max(reviewedCount, safeInt(progress.getReviewedCount())));
            userVocabProgressMapper.updateById(progress);
        }
        if (request.lastVocabItemId() != null) {
            recordVocabStudyEvent(userId, request.lastVocabItemId(), request.durationSeconds());
        }
        return getProgress(userId, vocabListId);
    }

    @Transactional
    public FavoriteStatusVO favorite(Long userId, Long vocabItemId) {
        ensureItemExists(vocabItemId);
        UserVocabFavorite favorite = userVocabFavoriteMapper.selectOne(new LambdaQueryWrapper<UserVocabFavorite>()
                .eq(UserVocabFavorite::getUserId, userId)
                .eq(UserVocabFavorite::getVocabItemId, vocabItemId)
                .last("limit 1"));
        if (favorite == null) {
            favorite = new UserVocabFavorite();
            favorite.setUserId(userId);
            favorite.setVocabItemId(vocabItemId);
            userVocabFavoriteMapper.insert(favorite);
        }
        return new FavoriteStatusVO(vocabItemId, true);
    }

    @Transactional
    public FavoriteStatusVO unfavorite(Long userId, Long vocabItemId) {
        UserVocabFavorite favorite = userVocabFavoriteMapper.selectOne(new LambdaQueryWrapper<UserVocabFavorite>()
                .eq(UserVocabFavorite::getUserId, userId)
                .eq(UserVocabFavorite::getVocabItemId, vocabItemId)
                .last("limit 1"));
        if (favorite != null) {
            userVocabFavoriteMapper.deleteById(favorite.getId());
        }
        return new FavoriteStatusVO(vocabItemId, false);
    }

    public PageResult<VocabItemVO> listFavorites(Long userId, long page, long pageSize) {
        PageParams params = PageParams.normalize(page, pageSize);
        List<UserVocabFavorite> favorites = userVocabFavoriteMapper.selectList(new LambdaQueryWrapper<UserVocabFavorite>()
                .eq(UserVocabFavorite::getUserId, userId)
                .orderByDesc(UserVocabFavorite::getCreatedAt)
                .orderByDesc(UserVocabFavorite::getId));
        List<Long> itemIds = favorites.stream().map(UserVocabFavorite::getVocabItemId).toList();
        if (itemIds.isEmpty()) {
            return PageResult.of(List.of(), 0, params.page(), params.pageSize());
        }
        Map<Long, VocabItem> itemById = vocabItemMapper.selectBatchIds(itemIds)
                .stream()
                .collect(Collectors.toMap(VocabItem::getId, Function.identity()));
        Map<Long, MediaAsset> audioAssets = loadMediaAssets(itemById.values().stream().map(VocabItem::getAudioAssetId).toList());
        List<VocabItemVO> records = itemIds.stream()
                .map(itemById::get)
                .filter(item -> item != null && "active".equals(item.getStatus()))
                .map(item -> toItemVO(item, true, audioAssets.get(item.getAudioAssetId())))
                .toList();
        List<VocabItemVO> pageRecords = sliceRecords(records, params.page(), params.pageSize());
        return withUserState(userId, PageResult.of(pageRecords, records.size(), params.page(), params.pageSize()));
    }

    private VocabListVO toListVO(VocabList list) {
        VocabList parent = list.getParentId() == null ? null : vocabListMapper.selectById(list.getParentId());
        return new VocabListVO(
                list.getId(),
                list.getName(),
                list.getParentId(),
                parent == null ? null : parent.getName(),
                list.getListType(),
                list.getLevel(),
                list.getDescription(),
                list.getSortOrder(),
                countActiveChildren(list.getId()),
                countDirectActiveItems(list.getId()),
                countActiveItems(list.getId())
        );
    }

    private VocabItemVO toItemVO(VocabItem item, boolean favorite, MediaAsset audioAsset) {
        return new VocabItemVO(
                item.getId(),
                item.getVocabListId(),
                item.getHanzi(),
                item.getPinyin(),
                item.getMeaningEn(),
                item.getMeaningRu(),
                item.getExampleSentence(),
                item.getAudioAssetId(),
                audioAsset == null ? null : audioAsset.getUrl(),
                item.getSortOrder(),
                favorite,
                null,
                0,
                null,
                null,
                null
        );
    }

    private Map<Long, MediaAsset> loadMediaAssets(List<Long> ids) {
        List<Long> assetIds = ids.stream().filter(id -> id != null).distinct().toList();
        if (assetIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return mediaAssetMapper.selectList(new LambdaQueryWrapper<MediaAsset>()
                        .in(MediaAsset::getId, assetIds)
                        .eq(MediaAsset::getStatus, "active"))
                .stream()
                .collect(Collectors.toMap(MediaAsset::getId, Function.identity()));
    }

    private void ensureListExists(Long vocabListId) {
        VocabList list = vocabListMapper.selectById(vocabListId);
        if (list == null || !"active".equals(list.getStatus())) {
            throw BusinessException.notFound("词汇表不存在");
        }
    }

    private VocabList ensureActiveListHierarchy(Long vocabListId) {
        VocabList list = vocabListMapper.selectById(vocabListId);
        if (list == null || !"active".equals(list.getStatus())) {
            throw BusinessException.notFound("词汇表不存在");
        }
        Set<Long> visited = new HashSet<>();
        VocabList current = list;
        while (current.getParentId() != null) {
            if (!visited.add(current.getId())) {
                throw BusinessException.conflict("词汇表层级存在循环引用");
            }
            VocabList parent = vocabListMapper.selectById(current.getParentId());
            if (parent == null || !"active".equals(parent.getStatus())) {
                throw BusinessException.notFound("词汇表不存在");
            }
            current = parent;
        }
        return list;
    }

    private void ensureItemExists(Long vocabItemId) {
        VocabItem item = vocabItemMapper.selectById(vocabItemId);
        if (item == null || !"active".equals(item.getStatus())) {
            throw BusinessException.notFound("词汇不存在");
        }
    }

    private void validateLastItem(Long vocabListId, Long vocabItemId) {
        if (vocabItemId == null) {
            return;
        }
        List<Long> scopeIds = resolveActiveListScope(vocabListId).stream().map(VocabList::getId).toList();
        VocabItem item = vocabItemMapper.selectById(vocabItemId);
        if (item == null || !"active".equals(item.getStatus()) || !scopeIds.contains(item.getVocabListId())) {
            throw BusinessException.notFound("最近学习词汇不属于当前词汇表");
        }
    }

    private long countActiveItems(Long vocabListId) {
        List<Long> scopeIds = resolveActiveListScope(vocabListId).stream().map(VocabList::getId).toList();
        if (scopeIds.isEmpty()) {
            return 0;
        }
        return vocabItemMapper.selectCount(new LambdaQueryWrapper<VocabItem>()
                .in(VocabItem::getVocabListId, scopeIds)
                .eq(VocabItem::getStatus, "active"));
    }

    private long countDirectActiveItems(Long vocabListId) {
        return vocabItemMapper.selectCount(new LambdaQueryWrapper<VocabItem>()
                .eq(VocabItem::getVocabListId, vocabListId)
                .eq(VocabItem::getStatus, "active"));
    }

    private long countActiveChildren(Long vocabListId) {
        return vocabListMapper.selectCount(new LambdaQueryWrapper<VocabList>()
                .eq(VocabList::getParentId, vocabListId)
                .eq(VocabList::getStatus, "active"));
    }

    private long countLearnedItems(Long userId, Long vocabListId) {
        List<Long> activeItemIds = activeItemIds(vocabListId);
        if (activeItemIds.isEmpty()) {
            return 0;
        }
        return userVocabItemProgressMapper.selectList(new LambdaQueryWrapper<UserVocabItemProgress>()
                        .eq(UserVocabItemProgress::getUserId, userId)
                        .in(UserVocabItemProgress::getVocabItemId, activeItemIds)
                        .in(UserVocabItemProgress::getStatus, LEARNED_ITEM_STATUSES))
                .stream()
                .map(UserVocabItemProgress::getVocabItemId)
                .distinct()
                .count();
    }

    private long countItemsByStatus(Long userId, Long vocabListId, String status) {
        List<Long> activeItemIds = activeItemIds(vocabListId);
        if (activeItemIds.isEmpty()) {
            return 0;
        }
        return userVocabItemProgressMapper.selectList(new LambdaQueryWrapper<UserVocabItemProgress>()
                        .eq(UserVocabItemProgress::getUserId, userId)
                        .in(UserVocabItemProgress::getVocabItemId, activeItemIds)
                        .eq(UserVocabItemProgress::getStatus, status))
                .stream()
                .map(UserVocabItemProgress::getVocabItemId)
                .distinct()
                .count();
    }

    private List<Long> activeItemIds(Long vocabListId) {
        List<Long> scopeIds = resolveActiveListScope(vocabListId).stream().map(VocabList::getId).toList();
        if (scopeIds.isEmpty()) {
            return List.of();
        }
        return vocabItemMapper.selectList(new LambdaQueryWrapper<VocabItem>()
                        .select(VocabItem::getId)
                        .in(VocabItem::getVocabListId, scopeIds)
                        .eq(VocabItem::getStatus, "active"))
                .stream()
                .map(VocabItem::getId)
                .toList();
    }

    private void updateItemProgress(Long userId, Long vocabListId, Long vocabItemId, String requestedStatus) {
        if (vocabItemId == null) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        String nextStatus = requestedStatus == null || requestedStatus.isBlank() ? ITEM_STATUS_LEARNED : requestedStatus;
        VocabItem item = vocabItemMapper.selectById(vocabItemId);
        if (item == null) {
            throw BusinessException.notFound("词汇不存在");
        }
        UserVocabItemProgress progress = userVocabItemProgressMapper.selectOne(new LambdaQueryWrapper<UserVocabItemProgress>()
                .eq(UserVocabItemProgress::getUserId, userId)
                .eq(UserVocabItemProgress::getVocabItemId, vocabItemId)
                .orderByDesc(UserVocabItemProgress::getUpdatedAt)
                .last("limit 1"));
        if (progress == null) {
            progress = new UserVocabItemProgress();
            progress.setUserId(userId);
            progress.setVocabListId(item.getVocabListId());
            progress.setVocabItemId(vocabItemId);
            progress.setStatus(nextStatus);
            progress.setReviewCount(1);
            if (isLearnedStatus(nextStatus)) {
                progress.setLearnedAt(now);
            }
            progress.setLastReviewedAt(now);
            userVocabItemProgressMapper.insert(progress);
            return;
        }
        String previousStatus = progress.getStatus();
        progress.setStatus(resolveNextItemStatus(previousStatus, nextStatus));
        progress.setReviewCount(safeInt(progress.getReviewCount()) + 1);
        if (progress.getLearnedAt() == null && isLearnedStatus(progress.getStatus())) {
            progress.setLearnedAt(now);
        }
        progress.setLastReviewedAt(now);
        progress.setUpdatedAt(now);
        userVocabItemProgressMapper.updateById(progress);
    }

    private String resolveNextItemStatus(String previousStatus, String requestedStatus) {
        if (statusRank(requestedStatus) >= statusRank(previousStatus)) {
            return requestedStatus;
        }
        return previousStatus;
    }

    private int statusRank(String status) {
        return switch (status == null ? ITEM_STATUS_LEARNING : status) {
            case ITEM_STATUS_MASTERED -> 4;
            case ITEM_STATUS_REVIEWING -> 3;
            case ITEM_STATUS_LEARNED -> 2;
            default -> 1;
        };
    }

    private boolean isLearnedStatus(String status) {
        return LEARNED_ITEM_STATUSES.contains(status);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String listCacheKey(long page, long pageSize, String listType, String level, Long parentId) {
        return "vocab:lists:page:%d:size:%d:type:%s:level:%s:parent:%s".formatted(
                page,
                pageSize,
                cachePart(listType),
                cachePart(level),
                parentId == null ? "_" : parentId
        );
    }

    private String listDetailCacheKey(Long vocabListId) {
        return "vocab:lists:id:%d".formatted(vocabListId);
    }

    private String itemCacheKey(Long vocabListId, long page, long pageSize) {
        return "vocab:items:list:%d:page:%d:size:%d".formatted(vocabListId, page, pageSize);
    }

    private String itemDetailCacheKey(Long vocabItemId) {
        return "vocab:items:id:%d".formatted(vocabItemId);
    }

    private PageResult<VocabItemVO> withUserState(Long userId, PageResult<VocabItemVO> itemPage) {
        List<Long> itemIds = itemPage.records().stream().map(VocabItemVO::id).toList();
        Set<Long> favoriteIds = favoriteIds(userId, itemIds);
        Map<Long, UserVocabItemProgress> progressByItemId = itemProgressByItemId(userId, itemIds);
        return PageResult.of(
                itemPage.records().stream()
                        .map(item -> withFavorite(item, favoriteIds.contains(item.id())))
                        .map(item -> withItemProgress(item, progressByItemId.get(item.id())))
                        .toList(),
                itemPage.total(),
                itemPage.page(),
                itemPage.pageSize()
        );
    }

    private VocabItemVO withFavorite(VocabItemVO item, boolean favorite) {
        return new VocabItemVO(
                item.id(),
                item.vocabListId(),
                item.hanzi(),
                item.pinyin(),
                item.meaningEn(),
                item.meaningRu(),
                item.exampleSentence(),
                item.audioAssetId(),
                item.audioUrl(),
                item.sortOrder(),
                favorite,
                item.progressStatus(),
                item.reviewCount(),
                item.learnedAt(),
                item.lastReviewedAt(),
                item.nextReviewAt()
        );
    }

    private VocabItemVO withItemProgress(VocabItemVO item, UserVocabItemProgress progress) {
        if (progress == null) {
            return item;
        }
        return new VocabItemVO(
                item.id(),
                item.vocabListId(),
                item.hanzi(),
                item.pinyin(),
                item.meaningEn(),
                item.meaningRu(),
                item.exampleSentence(),
                item.audioAssetId(),
                item.audioUrl(),
                item.sortOrder(),
                item.favorite(),
                progress.getStatus(),
                progress.getReviewCount(),
                progress.getLearnedAt(),
                progress.getLastReviewedAt(),
                progress.getNextReviewAt()
        );
    }

    private String cachePart(String value) {
        return value == null || value.isBlank() ? "_" : value.trim().replace(":", "%3A");
    }

    private Set<Long> favoriteIds(Long userId, List<Long> itemIds) {
        if (itemIds.isEmpty()) {
            return Collections.emptySet();
        }
        return userVocabFavoriteMapper.selectList(new LambdaQueryWrapper<UserVocabFavorite>()
                        .eq(UserVocabFavorite::getUserId, userId)
                        .in(UserVocabFavorite::getVocabItemId, itemIds))
                .stream()
                .map(UserVocabFavorite::getVocabItemId)
                .collect(Collectors.toSet());
    }

    private Map<Long, UserVocabItemProgress> itemProgressByItemId(Long userId, List<Long> itemIds) {
        if (itemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userVocabItemProgressMapper.selectList(new LambdaQueryWrapper<UserVocabItemProgress>()
                        .eq(UserVocabItemProgress::getUserId, userId)
                        .in(UserVocabItemProgress::getVocabItemId, itemIds))
                .stream()
                .collect(Collectors.toMap(
                        UserVocabItemProgress::getVocabItemId,
                        Function.identity(),
                        this::betterProgress
                ));
    }

    private UserVocabItemProgress betterProgress(UserVocabItemProgress left, UserVocabItemProgress right) {
        if (statusRank(right.getStatus()) != statusRank(left.getStatus())) {
            return statusRank(right.getStatus()) > statusRank(left.getStatus()) ? right : left;
        }
        OffsetDateTime leftReviewed = left.getLastReviewedAt();
        OffsetDateTime rightReviewed = right.getLastReviewedAt();
        if (leftReviewed == null) {
            return rightReviewed == null ? left : right;
        }
        return rightReviewed != null && rightReviewed.isAfter(leftReviewed) ? right : left;
    }

    private List<VocabList> resolveActiveListScope(Long vocabListId) {
        VocabList root = ensureActiveListHierarchy(vocabListId);
        List<VocabList> scope = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        ArrayDeque<VocabList> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            VocabList current = queue.removeFirst();
            if (!visited.add(current.getId())) {
                continue;
            }
            scope.add(current);
            List<VocabList> children = vocabListMapper.selectList(new LambdaQueryWrapper<VocabList>()
                    .eq(VocabList::getParentId, current.getId())
                    .eq(VocabList::getStatus, "active")
                    .orderByAsc(VocabList::getSortOrder)
                    .orderByAsc(VocabList::getId));
            queue.addAll(children);
        }
        return scope;
    }

    private List<VocabItem> slice(List<VocabItem> items, long page, long pageSize) {
        int from = Math.toIntExact(Math.min((page - 1) * pageSize, items.size()));
        int to = Math.toIntExact(Math.min(from + pageSize, items.size()));
        return items.subList(from, to);
    }

    private <T> List<T> sliceRecords(List<T> records, long page, long pageSize) {
        int from = Math.toIntExact(Math.min((page - 1) * pageSize, records.size()));
        int to = Math.toIntExact(Math.min(from + pageSize, records.size()));
        return records.subList(from, to);
    }

    private void recordVocabStudyEvent(Long userId, Long vocabItemId, Integer durationSeconds) {
        learningStatsRecorder.recordEvent(userId, "vocab", vocabItemId, "completed", durationSeconds, OffsetDateTime.now());
    }
}
