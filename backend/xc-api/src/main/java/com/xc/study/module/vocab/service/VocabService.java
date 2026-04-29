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
import com.xc.study.module.vocab.entity.UserVocabProgress;
import com.xc.study.module.vocab.entity.VocabItem;
import com.xc.study.module.vocab.entity.VocabList;
import com.xc.study.module.vocab.mapper.UserVocabFavoriteMapper;
import com.xc.study.module.vocab.mapper.UserVocabProgressMapper;
import com.xc.study.module.vocab.mapper.VocabItemMapper;
import com.xc.study.module.vocab.mapper.VocabListMapper;
import com.xc.study.module.vocab.vo.FavoriteStatusVO;
import com.xc.study.module.vocab.vo.VocabItemVO;
import com.xc.study.module.vocab.vo.VocabListVO;
import com.xc.study.module.vocab.vo.VocabProgressVO;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VocabService {

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
    private final UserVocabFavoriteMapper userVocabFavoriteMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final LearningStatsRecorder learningStatsRecorder;
    private final MasterDataCache masterDataCache;

    public VocabService(
            VocabListMapper vocabListMapper,
            VocabItemMapper vocabItemMapper,
            UserVocabProgressMapper userVocabProgressMapper,
            UserVocabFavoriteMapper userVocabFavoriteMapper,
            MediaAssetMapper mediaAssetMapper,
            LearningStatsRecorder learningStatsRecorder,
            MasterDataCache masterDataCache
    ) {
        this.vocabListMapper = vocabListMapper;
        this.vocabItemMapper = vocabItemMapper;
        this.userVocabProgressMapper = userVocabProgressMapper;
        this.userVocabFavoriteMapper = userVocabFavoriteMapper;
        this.mediaAssetMapper = mediaAssetMapper;
        this.learningStatsRecorder = learningStatsRecorder;
        this.masterDataCache = masterDataCache;
    }

    public PageResult<VocabListVO> listLists(long page, long pageSize, String listType, String level) {
        PageParams params = PageParams.normalize(page, pageSize);
        return masterDataCache.get(
                listCacheKey(params.page(), params.pageSize(), listType, level),
                VOCAB_LIST_PAGE_TYPE,
                () -> loadLists(params.page(), params.pageSize(), listType, level)
        );
    }

    private PageResult<VocabListVO> loadLists(long page, long pageSize, String listType, String level) {
        Page<VocabList> result = vocabListMapper.selectPage(Page.of(page, pageSize), new LambdaQueryWrapper<VocabList>()
                .eq(VocabList::getStatus, "active")
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
        VocabList list = vocabListMapper.selectById(vocabListId);
        if (list == null || !"active".equals(list.getStatus())) {
            throw BusinessException.notFound("词汇表不存在");
        }
        return toListVO(list);
    }

    public PageResult<VocabItemVO> listItems(Long userId, Long vocabListId, long page, long pageSize) {
        PageParams params = PageParams.normalize(page, pageSize);
        PageResult<VocabItemVO> itemPage = masterDataCache.get(
                itemCacheKey(vocabListId, params.page(), params.pageSize()),
                VOCAB_ITEM_PAGE_TYPE,
                () -> loadItems(vocabListId, params.page(), params.pageSize())
        );
        return withFavoriteStatus(userId, itemPage);
    }

    private PageResult<VocabItemVO> loadItems(Long vocabListId, long page, long pageSize) {
        ensureListExists(vocabListId);
        Page<VocabItem> result = vocabItemMapper.selectPage(Page.of(page, pageSize), new LambdaQueryWrapper<VocabItem>()
                .eq(VocabItem::getVocabListId, vocabListId)
                .eq(VocabItem::getStatus, "active")
                .orderByAsc(VocabItem::getSortOrder)
                .orderByAsc(VocabItem::getId));
        Map<Long, MediaAsset> audioAssets = loadMediaAssets(result.getRecords().stream().map(VocabItem::getAudioAssetId).toList());
        return PageResult.of(result.getRecords().stream()
                .map(item -> toItemVO(item, false, audioAssets.get(item.getAudioAssetId())))
                .toList(), result.getTotal(), page, pageSize);
    }

    public VocabItemVO getItem(Long userId, Long vocabItemId) {
        VocabItemVO item = masterDataCache.get(
                itemDetailCacheKey(vocabItemId),
                VOCAB_ITEM_TYPE,
                () -> loadItem(vocabItemId)
        );
        return withFavorite(item, favoriteIds(userId, List.of(item.id())).contains(item.id()));
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
        ensureListExists(vocabListId);
        long totalCount = countActiveItems(vocabListId);
        UserVocabProgress progress = userVocabProgressMapper.selectOne(new LambdaQueryWrapper<UserVocabProgress>()
                .eq(UserVocabProgress::getUserId, userId)
                .eq(UserVocabProgress::getVocabListId, vocabListId)
                .last("limit 1"));
        if (progress == null) {
            return new VocabProgressVO(vocabListId, 0, null, 0, totalCount);
        }
        return new VocabProgressVO(
                vocabListId,
                progress.getCurrentIndex(),
                progress.getLastVocabItemId(),
                progress.getReviewedCount(),
                totalCount
        );
    }

    @Transactional
    public VocabProgressVO updateProgress(Long userId, Long vocabListId, UpdateVocabProgressRequest request) {
        ensureListExists(vocabListId);
        validateLastItem(vocabListId, request.lastVocabItemId());

        UserVocabProgress progress = userVocabProgressMapper.selectOne(new LambdaQueryWrapper<UserVocabProgress>()
                .eq(UserVocabProgress::getUserId, userId)
                .eq(UserVocabProgress::getVocabListId, vocabListId)
                .last("limit 1"));
        int reviewedCount = request.reviewedCount() == null ? request.currentIndex() : request.reviewedCount();
        int previousReviewedCount = progress == null || progress.getReviewedCount() == null ? 0 : progress.getReviewedCount();
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
            progress.setReviewedCount(Math.max(reviewedCount, previousReviewedCount));
            userVocabProgressMapper.updateById(progress);
        }
        if (progress.getReviewedCount() > previousReviewedCount) {
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
        Page<UserVocabFavorite> result = userVocabFavoriteMapper.selectPage(Page.of(page, pageSize), new LambdaQueryWrapper<UserVocabFavorite>()
                .eq(UserVocabFavorite::getUserId, userId)
                .orderByDesc(UserVocabFavorite::getCreatedAt)
                .orderByDesc(UserVocabFavorite::getId));
        List<Long> itemIds = result.getRecords().stream().map(UserVocabFavorite::getVocabItemId).toList();
        if (itemIds.isEmpty()) {
            return PageResult.of(List.of(), result.getTotal(), page, pageSize);
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
        return PageResult.of(records, result.getTotal(), page, pageSize);
    }

    private VocabListVO toListVO(VocabList list) {
        return new VocabListVO(list.getId(), list.getName(), list.getListType(), list.getLevel(), list.getDescription(), list.getSortOrder());
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
                favorite
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
        VocabItem item = vocabItemMapper.selectById(vocabItemId);
        if (item == null || !"active".equals(item.getStatus()) || !vocabListId.equals(item.getVocabListId())) {
            throw BusinessException.notFound("最近学习词汇不属于当前词汇表");
        }
    }

    private long countActiveItems(Long vocabListId) {
        return vocabItemMapper.selectCount(new LambdaQueryWrapper<VocabItem>()
                .eq(VocabItem::getVocabListId, vocabListId)
                .eq(VocabItem::getStatus, "active"));
    }

    private String listCacheKey(long page, long pageSize, String listType, String level) {
        return "vocab:lists:page:%d:size:%d:type:%s:level:%s".formatted(
                page,
                pageSize,
                cachePart(listType),
                cachePart(level)
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

    private PageResult<VocabItemVO> withFavoriteStatus(Long userId, PageResult<VocabItemVO> itemPage) {
        List<Long> itemIds = itemPage.records().stream().map(VocabItemVO::id).toList();
        Set<Long> favoriteIds = favoriteIds(userId, itemIds);
        return PageResult.of(
                itemPage.records().stream()
                        .map(item -> withFavorite(item, favoriteIds.contains(item.id())))
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
                favorite
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

    private void recordVocabStudyEvent(Long userId, Long vocabItemId, Integer durationSeconds) {
        learningStatsRecorder.recordEvent(userId, "vocab", vocabItemId, "completed", durationSeconds, OffsetDateTime.now());
    }
}
