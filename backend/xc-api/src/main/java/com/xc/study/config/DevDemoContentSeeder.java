package com.xc.study.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xc.study.module.dialogue.entity.DialogueLine;
import com.xc.study.module.dialogue.entity.DialogueLineVocab;
import com.xc.study.module.dialogue.entity.VideoMaterial;
import com.xc.study.module.dialogue.mapper.DialogueLineMapper;
import com.xc.study.module.dialogue.mapper.DialogueLineVocabMapper;
import com.xc.study.module.dialogue.mapper.VideoMaterialMapper;
import com.xc.study.module.exercise.entity.ExerciseSet;
import com.xc.study.module.exercise.entity.ExerciseSetItem;
import com.xc.study.module.exercise.entity.SentenceExercise;
import com.xc.study.module.exercise.entity.SentenceWordOption;
import com.xc.study.module.exercise.mapper.ExerciseSetItemMapper;
import com.xc.study.module.exercise.mapper.ExerciseSetMapper;
import com.xc.study.module.exercise.mapper.SentenceExerciseMapper;
import com.xc.study.module.exercise.mapper.SentenceWordOptionMapper;
import com.xc.study.module.vocab.entity.VocabItem;
import com.xc.study.module.vocab.entity.VocabList;
import com.xc.study.module.vocab.mapper.VocabItemMapper;
import com.xc.study.module.vocab.mapper.VocabListItemMapper;
import com.xc.study.module.vocab.mapper.VocabListMapper;
import com.xc.study.module.vocab.entity.VocabListItem;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DevDemoContentSeeder {

    private final VocabListMapper vocabListMapper;
    private final VocabItemMapper vocabItemMapper;
    private final VocabListItemMapper vocabListItemMapper;
    private final ExerciseSetMapper exerciseSetMapper;
    private final ExerciseSetItemMapper exerciseSetItemMapper;
    private final SentenceExerciseMapper sentenceExerciseMapper;
    private final SentenceWordOptionMapper sentenceWordOptionMapper;
    private final VideoMaterialMapper videoMaterialMapper;
    private final DialogueLineMapper dialogueLineMapper;
    private final DialogueLineVocabMapper dialogueLineVocabMapper;

    public DevDemoContentSeeder(
            VocabListMapper vocabListMapper,
            VocabItemMapper vocabItemMapper,
            VocabListItemMapper vocabListItemMapper,
            ExerciseSetMapper exerciseSetMapper,
            ExerciseSetItemMapper exerciseSetItemMapper,
            SentenceExerciseMapper sentenceExerciseMapper,
            SentenceWordOptionMapper sentenceWordOptionMapper,
            VideoMaterialMapper videoMaterialMapper,
            DialogueLineMapper dialogueLineMapper,
            DialogueLineVocabMapper dialogueLineVocabMapper
    ) {
        this.vocabListMapper = vocabListMapper;
        this.vocabItemMapper = vocabItemMapper;
        this.vocabListItemMapper = vocabListItemMapper;
        this.exerciseSetMapper = exerciseSetMapper;
        this.exerciseSetItemMapper = exerciseSetItemMapper;
        this.sentenceExerciseMapper = sentenceExerciseMapper;
        this.sentenceWordOptionMapper = sentenceWordOptionMapper;
        this.videoMaterialMapper = videoMaterialMapper;
        this.dialogueLineMapper = dialogueLineMapper;
        this.dialogueLineVocabMapper = dialogueLineVocabMapper;
    }

    @Transactional
    public DemoContent ensureContent() {
        VocabList vocabList = ensureVocabList();
        List<VocabItem> vocabItems = ensureVocabItems(vocabList.getId());
        ExerciseSet exerciseSet = ensureExerciseSet();
        List<SentenceExercise> exercises = ensureExercises(exerciseSet.getId());
        List<DialogueLine> dialogueLines = ensureDialogueMaterial(vocabItems);
        return new DemoContent(vocabList, vocabItems, exerciseSet, exercises, dialogueLines);
    }

    private VocabList ensureVocabList() {
        VocabList list = vocabListMapper.selectOne(new LambdaQueryWrapper<VocabList>()
                .eq(VocabList::getName, "Demo HSK1 基础词汇")
                .eq(VocabList::getListType, "HSK")
                .eq(VocabList::getLevel, "HSK1")
                .last("limit 1"));
        if (list != null) {
            return list;
        }
        list = new VocabList();
        list.setName("Demo HSK1 基础词汇");
        list.setListType("HSK");
        list.setLevel("HSK1");
        list.setDescription("开发环境演示词表：问候、人物、日常表达。");
        list.setSortOrder(1);
        list.setStatus("active");
        vocabListMapper.insert(list);
        return list;
    }

    private List<VocabItem> ensureVocabItems(Long vocabListId) {
        return List.of(
                ensureVocabItem(vocabListId, "你好", "nǐ hǎo", "hello", "привет", "你好，我是安娜。", 1),
                ensureVocabItem(vocabListId, "谢谢", "xiè xie", "thank you", "спасибо", "谢谢你的帮助。", 2),
                ensureVocabItem(vocabListId, "学生", "xué sheng", "student", "студент", "我是汉语学生。", 3),
                ensureVocabItem(vocabListId, "老师", "lǎo shī", "teacher", "учитель", "王老师很好。", 4),
                ensureVocabItem(vocabListId, "学习", "xué xí", "to study", "учиться", "我每天学习汉语。", 5),
                ensureVocabItem(vocabListId, "朋友", "péng you", "friend", "друг", "他是我的朋友。", 6),
                ensureVocabItem(vocabListId, "中国", "zhōng guó", "China", "Китай", "我想去中国。", 7),
                ensureVocabItem(vocabListId, "今天", "jīn tiān", "today", "сегодня", "今天我学习汉语。", 8),
                ensureVocabItem(vocabListId, "名字", "míng zi", "name", "имя", "你的名字是什么？", 9),
                ensureVocabItem(vocabListId, "再见", "zài jiàn", "goodbye", "до свидания", "老师，再见。", 10)
        );
    }

    private VocabItem ensureVocabItem(
            Long vocabListId,
            String hanzi,
            String pinyin,
            String meaningEn,
            String meaningRu,
            String exampleSentence,
            int sortOrder
    ) {
        VocabItem item = vocabItemMapper.selectOne(new LambdaQueryWrapper<VocabItem>()
                .eq(VocabItem::getVocabListId, vocabListId)
                .eq(VocabItem::getHanzi, hanzi)
                .last("limit 1"));
        if (item != null) {
            ensureVocabListItem(vocabListId, item.getId(), sortOrder);
            return item;
        }
        item = new VocabItem();
        item.setVocabListId(vocabListId);
        item.setHanzi(hanzi);
        item.setPinyin(pinyin);
        item.setMeaningEn(meaningEn);
        item.setMeaningRu(meaningRu);
        item.setExampleSentence(exampleSentence);
        item.setSortOrder(sortOrder);
        item.setStatus("active");
        vocabItemMapper.insert(item);
        ensureVocabListItem(vocabListId, item.getId(), sortOrder);
        return item;
    }

    private void ensureVocabListItem(Long vocabListId, Long vocabItemId, int sortOrder) {
        VocabListItem link = vocabListItemMapper.selectOne(new LambdaQueryWrapper<VocabListItem>()
                .eq(VocabListItem::getVocabListId, vocabListId)
                .eq(VocabListItem::getVocabItemId, vocabItemId)
                .last("limit 1"));
        if (link != null) {
            return;
        }
        link = new VocabListItem();
        link.setVocabListId(vocabListId);
        link.setVocabItemId(vocabItemId);
        link.setSortOrder(sortOrder);
        link.setStatus("active");
        vocabListItemMapper.insert(link);
    }

    private ExerciseSet ensureExerciseSet() {
        ExerciseSet set = exerciseSetMapper.selectOne(new LambdaQueryWrapper<ExerciseSet>()
                .eq(ExerciseSet::getTitle, "Demo HSK1 句子排序")
                .eq(ExerciseSet::getExerciseType, "translation_order")
                .eq(ExerciseSet::getLevel, "HSK1")
                .last("limit 1"));
        if (set != null) {
            return set;
        }
        set = new ExerciseSet();
        set.setTitle("Demo HSK1 句子排序");
        set.setExerciseType("translation_order");
        set.setLevel("HSK1");
        set.setStatus("active");
        exerciseSetMapper.insert(set);
        return set;
    }

    private List<SentenceExercise> ensureExercises(Long exerciseSetId) {
        return List.of(
                ensureExercise(exerciseSetId, "我是学生", "wǒ shì xué sheng", "I am a student.", "Я студент.", 1, List.of("我", "是", "学生")),
                ensureExercise(exerciseSetId, "我学习汉语", "wǒ xué xí hàn yǔ", "I study Chinese.", "Я изучаю китайский.", 2, List.of("我", "学习", "汉语")),
                ensureExercise(exerciseSetId, "老师很好", "lǎo shī hěn hǎo", "The teacher is very good.", "Учитель очень хороший.", 3, List.of("老师", "很", "好"))
        );
    }

    private SentenceExercise ensureExercise(
            Long exerciseSetId,
            String answer,
            String pinyin,
            String translationEn,
            String translationRu,
            int sortOrder,
            List<String> words
    ) {
        SentenceExercise exercise = sentenceExerciseMapper.selectOne(new LambdaQueryWrapper<SentenceExercise>()
                .eq(SentenceExercise::getExerciseSetId, exerciseSetId)
                .eq(SentenceExercise::getHanziAnswer, answer)
                .last("limit 1"));
        if (exercise == null) {
            exercise = new SentenceExercise();
            exercise.setExerciseSetId(exerciseSetId);
            exercise.setExerciseType("translation_order");
            exercise.setHanziAnswer(answer);
            exercise.setPinyinPrompt(pinyin);
            exercise.setTranslationEn(translationEn);
            exercise.setTranslationRu(translationRu);
            exercise.setExplanation("按正确语序排列词块。");
            exercise.setSortOrder(sortOrder);
            exercise.setStatus("active");
            sentenceExerciseMapper.insert(exercise);
        }
        ensureExerciseSetItem(exerciseSetId, exercise.getId(), sortOrder);
        ensureWordOptions(exercise.getId(), words);
        return exercise;
    }

    private void ensureExerciseSetItem(Long exerciseSetId, Long exerciseId, int sortOrder) {
        ExerciseSetItem link = exerciseSetItemMapper.selectOne(new LambdaQueryWrapper<ExerciseSetItem>()
                .eq(ExerciseSetItem::getExerciseSetId, exerciseSetId)
                .eq(ExerciseSetItem::getSentenceExerciseId, exerciseId)
                .last("limit 1"));
        if (link != null) {
            return;
        }
        link = new ExerciseSetItem();
        link.setExerciseSetId(exerciseSetId);
        link.setSentenceExerciseId(exerciseId);
        link.setSortOrder(sortOrder);
        link.setStatus("active");
        exerciseSetItemMapper.insert(link);
    }

    private void ensureWordOptions(Long exerciseId, List<String> words) {
        Long count = sentenceWordOptionMapper.selectCount(new LambdaQueryWrapper<SentenceWordOption>()
                .eq(SentenceWordOption::getExerciseId, exerciseId));
        if (count > 0) {
            return;
        }
        for (int i = 0; i < words.size(); i++) {
            SentenceWordOption option = new SentenceWordOption();
            option.setExerciseId(exerciseId);
            option.setWordText(words.get(i));
            option.setCorrectOrder(i + 1);
            sentenceWordOptionMapper.insert(option);
        }
    }

    private List<DialogueLine> ensureDialogueMaterial(List<VocabItem> vocabItems) {
        VideoMaterial material = videoMaterialMapper.selectOne(new LambdaQueryWrapper<VideoMaterial>()
                .eq(VideoMaterial::getTitle, "Demo HSK1 日常对话")
                .eq(VideoMaterial::getMaterialType, "short_video")
                .last("limit 1"));
        if (material == null) {
            material = new VideoMaterial();
            material.setTitle("Demo HSK1 日常对话");
            material.setMaterialType("short_video");
            material.setDescription("开发环境演示台词材料：问候、身份介绍和告别。");
            material.setStatus("active");
            videoMaterialMapper.insert(material);
        }
        DialogueLine line1 = ensureDialogueLine(material.getId(), 1, "你好，我是学生", "nǐ hǎo, wǒ shì xué sheng", "Hello, I am a student.", "Привет, я студент.");
        DialogueLine line2 = ensureDialogueLine(material.getId(), 2, "我每天学习汉语", "wǒ měi tiān xué xí hàn yǔ", "I study Chinese every day.", "Я каждый день изучаю китайский.");
        DialogueLine line3 = ensureDialogueLine(material.getId(), 3, "老师，再见", "lǎo shī, zài jiàn", "Teacher, goodbye.", "Учитель, до свидания.");

        ensureDialogueVocab(line1.getId(), vocabItems, "你好", "nǐ hǎo", "hello", "привет");
        ensureDialogueVocab(line1.getId(), vocabItems, "学生", "xué sheng", "student", "студент");
        ensureDialogueVocab(line2.getId(), vocabItems, "学习", "xué xí", "to study", "учиться");
        ensureDialogueVocab(line3.getId(), vocabItems, "老师", "lǎo shī", "teacher", "учитель");
        ensureDialogueVocab(line3.getId(), vocabItems, "再见", "zài jiàn", "goodbye", "до свидания");
        return List.of(line1, line2, line3);
    }

    private DialogueLine ensureDialogueLine(
            Long materialId,
            int lineNo,
            String hanzi,
            String pinyin,
            String translationEn,
            String translationRu
    ) {
        DialogueLine line = dialogueLineMapper.selectOne(new LambdaQueryWrapper<DialogueLine>()
                .eq(DialogueLine::getMaterialId, materialId)
                .eq(DialogueLine::getLineNo, lineNo)
                .last("limit 1"));
        if (line == null) {
            line = new DialogueLine();
            line.setMaterialId(materialId);
            line.setLineNo(lineNo);
            line.setHanziText(hanzi);
            line.setPinyinText(pinyin);
            line.setTranslationEn(translationEn);
            line.setTranslationRu(translationRu);
            dialogueLineMapper.insert(line);
        }
        return line;
    }

    private void ensureDialogueVocab(
            Long lineId,
            List<VocabItem> vocabItems,
            String wordText,
            String pinyin,
            String meaningEn,
            String meaningRu
    ) {
        DialogueLineVocab vocab = dialogueLineVocabMapper.selectOne(new LambdaQueryWrapper<DialogueLineVocab>()
                .eq(DialogueLineVocab::getDialogueLineId, lineId)
                .eq(DialogueLineVocab::getWordText, wordText)
                .last("limit 1"));
        if (vocab != null) {
            return;
        }
        vocab = new DialogueLineVocab();
        vocab.setDialogueLineId(lineId);
        vocab.setVocabItemId(vocabItems.stream()
                .filter(item -> wordText.equals(item.getHanzi()))
                .map(VocabItem::getId)
                .findFirst()
                .orElse(null));
        vocab.setWordText(wordText);
        vocab.setPinyin(pinyin);
        vocab.setMeaningEn(meaningEn);
        vocab.setMeaningRu(meaningRu);
        vocab.setExplanation("台词中的常用 HSK1 表达。");
        dialogueLineVocabMapper.insert(vocab);
    }

    public record DemoContent(
            VocabList vocabList,
            List<VocabItem> vocabItems,
            ExerciseSet exerciseSet,
            List<SentenceExercise> exercises,
            List<DialogueLine> dialogueLines
    ) {
    }
}
