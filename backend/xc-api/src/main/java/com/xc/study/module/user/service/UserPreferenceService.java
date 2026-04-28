package com.xc.study.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xc.study.module.user.dto.UpdateUserPreferenceRequest;
import com.xc.study.module.user.entity.UserPreference;
import com.xc.study.module.user.mapper.UserPreferenceMapper;
import com.xc.study.module.user.vo.UserPreferenceVO;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserPreferenceService {

    private final UserPreferenceMapper userPreferenceMapper;

    public UserPreferenceService(UserPreferenceMapper userPreferenceMapper) {
        this.userPreferenceMapper = userPreferenceMapper;
    }

    public UserPreferenceVO getPreference(Long userId) {
        return toVO(ensurePreference(userId));
    }

    @Transactional
    public UserPreferenceVO updatePreference(Long userId, UpdateUserPreferenceRequest request) {
        UserPreference preference = ensurePreference(userId);
        if (StringUtils.hasText(request.uiLanguage())) {
            preference.setUiLanguage(request.uiLanguage());
        }
        if (StringUtils.hasText(request.translationLanguage())) {
            preference.setTranslationLanguage(request.translationLanguage());
        }
        if (StringUtils.hasText(request.vocabMeaningLanguage())) {
            preference.setVocabMeaningLanguage(request.vocabMeaningLanguage());
        }
        if (StringUtils.hasText(request.matchingMeaningLanguage())) {
            preference.setMatchingMeaningLanguage(request.matchingMeaningLanguage());
        }
        if (request.soundEnabled() != null) {
            preference.setSoundEnabled(request.soundEnabled());
        }
        preference.setUpdatedAt(OffsetDateTime.now());
        userPreferenceMapper.updateById(preference);
        return toVO(preference);
    }

    private UserPreference ensurePreference(Long userId) {
        UserPreference preference = userPreferenceMapper.selectOne(new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getUserId, userId)
                .last("limit 1"));
        if (preference != null) {
            return preference;
        }
        OffsetDateTime now = OffsetDateTime.now();
        preference = new UserPreference();
        preference.setUserId(userId);
        preference.setUiLanguage("zh");
        preference.setTranslationLanguage("ru");
        preference.setVocabMeaningLanguage("ru");
        preference.setMatchingMeaningLanguage("ru");
        preference.setSoundEnabled(true);
        preference.setCreatedAt(now);
        preference.setUpdatedAt(now);
        userPreferenceMapper.insert(preference);
        return preference;
    }

    private UserPreferenceVO toVO(UserPreference preference) {
        return new UserPreferenceVO(
                preference.getUiLanguage(),
                preference.getTranslationLanguage(),
                preference.getVocabMeaningLanguage(),
                preference.getMatchingMeaningLanguage(),
                preference.getSoundEnabled()
        );
    }
}
