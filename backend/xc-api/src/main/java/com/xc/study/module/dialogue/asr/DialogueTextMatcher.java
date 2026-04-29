package com.xc.study.module.dialogue.asr;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import org.springframework.util.StringUtils;

public final class DialogueTextMatcher {

    private DialogueTextMatcher() {
    }

    public static MatchResult match(String submitted, String standard) {
        String normalizedSubmitted = normalize(submitted);
        String normalizedStandard = normalize(standard);
        boolean correct = normalizedSubmitted.equals(normalizedStandard);
        Integer firstMismatchIndex = correct ? null : firstMismatchIndex(normalizedSubmitted, normalizedStandard);
        BigDecimal score = score(normalizedSubmitted, normalizedStandard);
        return new MatchResult(correct, firstMismatchIndex, score);
    }

    public static String normalize(String answer) {
        if (answer == null) {
            return "";
        }
        return Normalizer.normalize(answer, Normalizer.Form.NFKC)
                .replaceAll("[\\s\\p{Punct}，。！？、；：“”‘’（）《》【】]+", "")
                .toLowerCase();
    }

    private static Integer firstMismatchIndex(String submitted, String standard) {
        int length = Math.min(submitted.length(), standard.length());
        for (int i = 0; i < length; i++) {
            if (submitted.charAt(i) != standard.charAt(i)) {
                return i;
            }
        }
        return length;
    }

    private static BigDecimal score(String submitted, String standard) {
        int maxLength = Math.max(submitted.length(), standard.length());
        if (maxLength == 0) {
            return BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
        }
        int distance = levenshteinDistance(submitted, standard);
        double ratio = Math.max(0D, 1D - (double) distance / maxLength);
        return BigDecimal.valueOf(ratio * 100D).setScale(2, RoundingMode.HALF_UP);
    }

    private static int levenshteinDistance(String left, String right) {
        if (!StringUtils.hasText(left)) {
            return right.length();
        }
        if (!StringUtils.hasText(right)) {
            return left.length();
        }
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost
                );
            }
            int[] temp = previous;
            previous = current;
            current = temp;
        }
        return previous[right.length()];
    }

    public record MatchResult(boolean correct, Integer firstMismatchIndex, BigDecimal score) {
    }
}
