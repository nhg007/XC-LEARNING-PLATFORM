package com.xc.study.module.dialogue.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.common.PageResult;
import com.xc.study.module.dialogue.dto.CheckDialogueLineRequest;
import com.xc.study.module.dialogue.entity.DialogueLine;
import com.xc.study.module.dialogue.entity.DialogueLineVocab;
import com.xc.study.module.dialogue.entity.VideoMaterial;
import com.xc.study.module.dialogue.mapper.DialogueLineMapper;
import com.xc.study.module.dialogue.mapper.DialogueLineVocabMapper;
import com.xc.study.module.dialogue.mapper.VideoMaterialMapper;
import com.xc.study.module.dialogue.vo.DialogueLineAnalysisVO;
import com.xc.study.module.dialogue.vo.DialogueLineCheckResultVO;
import com.xc.study.module.dialogue.vo.DialogueLineVO;
import com.xc.study.module.dialogue.vo.DialogueLineVocabVO;
import com.xc.study.module.dialogue.vo.VideoMaterialVO;
import com.xc.study.module.media.entity.MediaAsset;
import com.xc.study.module.media.mapper.MediaAssetMapper;
import com.xc.study.module.stats.entity.StudyEvent;
import com.xc.study.module.stats.service.LearningStatsRecorder;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DialogueService {

    private final VideoMaterialMapper videoMaterialMapper;
    private final DialogueLineMapper dialogueLineMapper;
    private final DialogueLineVocabMapper dialogueLineVocabMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final LearningStatsRecorder learningStatsRecorder;

    public DialogueService(
            VideoMaterialMapper videoMaterialMapper,
            DialogueLineMapper dialogueLineMapper,
            DialogueLineVocabMapper dialogueLineVocabMapper,
            MediaAssetMapper mediaAssetMapper,
            LearningStatsRecorder learningStatsRecorder
    ) {
        this.videoMaterialMapper = videoMaterialMapper;
        this.dialogueLineMapper = dialogueLineMapper;
        this.dialogueLineVocabMapper = dialogueLineVocabMapper;
        this.mediaAssetMapper = mediaAssetMapper;
        this.learningStatsRecorder = learningStatsRecorder;
    }

    public PageResult<VideoMaterialVO> listMaterials(long page, long pageSize, String materialType) {
        Page<VideoMaterial> result = videoMaterialMapper.selectPage(Page.of(page, pageSize), new LambdaQueryWrapper<VideoMaterial>()
                .eq(VideoMaterial::getStatus, "active")
                .eq(StringUtils.hasText(materialType), VideoMaterial::getMaterialType, materialType)
                .orderByDesc(VideoMaterial::getUpdatedAt)
                .orderByDesc(VideoMaterial::getId));
        return PageResult.of(toMaterialVOs(result.getRecords()), result.getTotal(), page, pageSize);
    }

    public List<DialogueLineVO> listLines(Long materialId) {
        VideoMaterial material = requireActiveMaterial(materialId);
        List<DialogueLine> lines = dialogueLineMapper.selectList(new LambdaQueryWrapper<DialogueLine>()
                .eq(DialogueLine::getMaterialId, material.getId())
                .orderByAsc(DialogueLine::getLineNo)
                .orderByAsc(DialogueLine::getId));
        return toLineVOs(lines);
    }

    public DialogueLineAnalysisVO getAnalysis(Long lineId) {
        DialogueLine line = requireActiveLine(lineId);
        List<DialogueLineVocabVO> vocabItems = dialogueLineVocabMapper.selectList(new LambdaQueryWrapper<DialogueLineVocab>()
                        .eq(DialogueLineVocab::getDialogueLineId, line.getId())
                        .orderByAsc(DialogueLineVocab::getId))
                .stream()
                .map(vocab -> new DialogueLineVocabVO(
                        vocab.getId(),
                        vocab.getVocabItemId(),
                        vocab.getWordText(),
                        vocab.getPinyin(),
                        vocab.getMeaningEn(),
                        vocab.getMeaningRu(),
                        vocab.getExplanation()
                ))
                .toList();
        return new DialogueLineAnalysisVO(
                line.getId(),
                line.getHanziText(),
                line.getPinyinText(),
                line.getTranslationEn(),
                line.getTranslationRu(),
                vocabItems
        );
    }

    @Transactional
    public DialogueLineCheckResultVO checkLine(Long userId, Long lineId, CheckDialogueLineRequest request) {
        DialogueLine line = requireActiveLine(lineId);
        String submittedAnswer = resolveSubmittedAnswer(request);
        if (!StringUtils.hasText(submittedAnswer)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "答案不能为空");
        }
        String standardAnswer = line.getHanziText();
        String normalizedSubmitted = normalizeAnswer(submittedAnswer);
        String normalizedStandard = normalizeAnswer(standardAnswer);
        boolean correct = normalizedSubmitted.equals(normalizedStandard);
        Integer firstMismatchIndex = correct ? null : firstMismatchIndex(normalizedSubmitted, normalizedStandard);

        StudyEvent event = learningStatsRecorder.recordEvent(
                userId,
                "dialogue",
                line.getId(),
                correct ? "correct" : "wrong",
                request.durationSeconds(),
                OffsetDateTime.now()
        );
        return new DialogueLineCheckResultVO(
                event.getId(),
                line.getId(),
                correct,
                submittedAnswer,
                standardAnswer,
                firstMismatchIndex,
                correct ? "回答正确" : "回答有误，请继续修改"
        );
    }

    private List<VideoMaterialVO> toMaterialVOs(List<VideoMaterial> materials) {
        if (materials.isEmpty()) {
            return List.of();
        }
        Map<Long, MediaAsset> covers = loadMediaAssets(materials.stream().map(VideoMaterial::getCoverAssetId).toList());
        return materials.stream()
                .map(material -> {
                    MediaAsset cover = covers.get(material.getCoverAssetId());
                    return new VideoMaterialVO(
                            material.getId(),
                            material.getTitle(),
                            material.getMaterialType(),
                            material.getDescription(),
                            material.getCoverAssetId(),
                            cover == null ? null : cover.getUrl(),
                            countLines(material.getId())
                    );
                })
                .toList();
    }

    private List<DialogueLineVO> toLineVOs(List<DialogueLine> lines) {
        if (lines.isEmpty()) {
            return List.of();
        }
        Map<Long, MediaAsset> audioAssets = loadMediaAssets(lines.stream().map(DialogueLine::getAudioAssetId).toList());
        return lines.stream()
                .map(line -> {
                    MediaAsset audio = audioAssets.get(line.getAudioAssetId());
                    return new DialogueLineVO(
                            line.getId(),
                            line.getMaterialId(),
                            line.getLineNo(),
                            line.getHanziText(),
                            line.getPinyinText(),
                            line.getTranslationEn(),
                            line.getTranslationRu(),
                            line.getAudioAssetId(),
                            audio == null ? null : audio.getUrl(),
                            line.getStartMs(),
                            line.getEndMs(),
                            wordOptions(line.getHanziText())
                    );
                })
                .toList();
    }

    private Map<Long, MediaAsset> loadMediaAssets(List<Long> ids) {
        List<Long> assetIds = ids.stream().filter(id -> id != null).distinct().toList();
        if (assetIds.isEmpty()) {
            return Map.of();
        }
        return mediaAssetMapper.selectBatchIds(assetIds)
                .stream()
                .collect(Collectors.toMap(MediaAsset::getId, Function.identity()));
    }

    private long countLines(Long materialId) {
        return dialogueLineMapper.selectCount(new LambdaQueryWrapper<DialogueLine>()
                .eq(DialogueLine::getMaterialId, materialId));
    }

    private VideoMaterial requireActiveMaterial(Long materialId) {
        VideoMaterial material = videoMaterialMapper.selectById(materialId);
        if (material == null || !"active".equals(material.getStatus())) {
            throw BusinessException.notFound("台词材料不存在");
        }
        return material;
    }

    private DialogueLine requireActiveLine(Long lineId) {
        DialogueLine line = dialogueLineMapper.selectById(lineId);
        if (line == null) {
            throw BusinessException.notFound("台词不存在");
        }
        requireActiveMaterial(line.getMaterialId());
        return line;
    }

    private List<String> wordOptions(String answer) {
        if (!StringUtils.hasText(answer)) {
            return List.of();
        }
        return answer.codePoints()
                .mapToObj(Character::toString)
                .filter(StringUtils::hasText)
                .filter(text -> !text.matches("[\\s\\p{Punct}，。！？、；：“”‘’（）《》【】]+"))
                .toList();
    }

    private String resolveSubmittedAnswer(CheckDialogueLineRequest request) {
        if (request.orderedWords() != null && !request.orderedWords().isEmpty()) {
            return request.orderedWords().stream()
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining(""));
        }
        return request.answerText();
    }

    private String normalizeAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        return Normalizer.normalize(answer, Normalizer.Form.NFKC)
                .replaceAll("[\\s\\p{Punct}，。！？、；：“”‘’（）《》【】]+", "")
                .toLowerCase();
    }

    private Integer firstMismatchIndex(String submitted, String standard) {
        int length = Math.min(submitted.length(), standard.length());
        for (int i = 0; i < length; i++) {
            if (submitted.charAt(i) != standard.charAt(i)) {
                return i;
            }
        }
        return length;
    }
}
