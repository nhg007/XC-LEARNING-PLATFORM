<script lang="ts">
import { applyTabBarLocale } from './i18n'

export default {
  onLaunch() {
    applyTabBarLocale()
  }
}
</script>

<style>
@import url("./assets/fonts/maoken-yingbi-kaishu/maoken-yingbi-kaishu.css");

page {
  --xc-hanzi-font-family: "XCMaokenYingBiKai", "STKaiti", "KaiTi", "Kaiti SC", serif;
  --xc-kai-font-family: var(--xc-hanzi-font-family);
  --xc-bg: #f6f8fb;
  --xc-bg-grid: transparent;
  --xc-surface: #ffffff;
  --xc-surface-raised: #ffffff;
  --xc-surface-soft: #f2f6f7;
  --xc-border: #e4e8ef;
  --xc-border-strong: #c9d6df;
  --xc-ink: #111827;
  --xc-muted: #667085;
  --xc-muted-soft: #98a2b3;
  --xc-primary: #128475;
  --xc-primary-strong: #0b665d;
  --xc-primary-soft: #e7f5f2;
  --xc-button-primary-top: #27c0a3;
  --xc-button-primary-mid: #128475;
  --xc-button-primary-bottom: #0b665d;
  --xc-button-secondary-top: #ffffff;
  --xc-button-secondary-bottom: #f1f6f7;
  --xc-button-secondary-edge: #d5e2e6;
  --xc-accent: #2f6fe4;
  --xc-accent-soft: #eef4ff;
  --xc-blue: #2f6fe4;
  --xc-blue-soft: #eef4ff;
  --xc-yellow: #f2b84b;
  --xc-yellow-soft: #fff7e6;
  --xc-warning-bg: #fff7e6;
  --xc-warning-border: #f1d08a;
  --xc-warning-text: #8a5b00;
  --xc-danger-bg: #fff1ed;
  --xc-danger-border: #f7b8a7;
  --xc-danger-text: #9f2f1c;
  --xc-hero-bg: #101828;
  --xc-hero-bg-2: #173b45;
  --xc-hero-muted: #d0d9e4;
  --xc-shadow-card: 0 10rpx 30rpx rgba(16, 24, 40, 0.07);
  --xc-shadow-raised: 0 18rpx 48rpx rgba(16, 24, 40, 0.12);
  background-color: var(--xc-bg);
  color: var(--xc-ink);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}

button::after {
  border: none;
}

button {
  align-items: center;
  border-width: 0.5px !important;
  box-sizing: border-box;
  display: flex;
  justify-content: center;
  line-height: 1.2;
}

button[size='mini'] {
  min-height: 58rpx;
  padding: 0 18rpx;
}

page .page {
  background: transparent !important;
  color: var(--xc-ink) !important;
}

page .hero {
  background: linear-gradient(142deg, var(--xc-hero-bg) 0%, var(--xc-hero-bg-2) 100%) !important;
  border-bottom-left-radius: 30rpx !important;
  border-bottom-right-radius: 30rpx !important;
  box-shadow: 0 16rpx 40rpx rgba(16, 24, 40, 0.16);
  color: #ffffff !important;
  overflow: hidden;
  position: relative;
}

page .hero .eyebrow,
page .eyebrow {
  color: #8ff0df !important;
  font-weight: 900 !important;
  letter-spacing: 0 !important;
}

page .hero .subtitle {
  color: var(--xc-hero-muted) !important;
}

page .title,
page .section-title,
page .card-title,
page .list-title,
page .lesson-title,
page .set-title,
page .entry-title,
page .locked-title,
page .overview-title,
page .panel-title,
page .summary-title,
page .group-title,
page .feature-title,
page .access-title,
page .continue-title,
page .spotlight-title,
page .room-title,
page .member-name,
page .prompt,
page .meaning,
page .hanzi,
page .answer-title,
page .event-title {
  color: var(--xc-ink) !important;
}

page .title {
  font-size: 44rpx !important;
  font-weight: 800 !important;
  letter-spacing: 0 !important;
  line-height: 1.16 !important;
}

page .hero .title {
  color: #ffffff !important;
}

page .section-title,
page .overview-title,
page .panel-title {
  font-size: 32rpx !important;
  font-weight: 850 !important;
  letter-spacing: 0 !important;
}

page .card-title,
page .list-title,
page .lesson-title,
page .set-title,
page .entry-title,
page .item-title,
page .material-title,
page .summary-title,
page .group-title,
page .feature-title,
page .access-title,
page .continue-title,
page .spotlight-title {
  font-weight: 820 !important;
  letter-spacing: 0 !important;
}

page .muted,
page .card-desc,
page .entry-desc,
page .feature-desc,
page .access-desc,
page .continue-desc,
page .spotlight-desc,
page .summary-desc,
page .subtitle,
page .label,
page .meta-value,
page .example,
page .list-copy text:last-child {
  color: var(--xc-muted) !important;
}

page .subtitle,
page .muted,
page .card-desc,
page .entry-desc,
page .example {
  line-height: 1.58 !important;
}

page .state-card,
page .today-card,
page .start-card,
page .quick-card,
page .list-item,
page .entry-card,
page .overview-card,
page .continue-card,
page .summary-card,
page .favorite-card,
page .study-card,
page .control-panel,
page .scope-card,
page .lesson-item,
page .panel,
page .benefits text,
page .set-item,
page .setup-card,
page .status-card,
page .completed-card,
page .locked-card,
page .translation-card,
page .shadow-card,
page .analysis-card,
page .speech-card,
page .result-card,
page .vocab-card,
page .access-card,
page .plan-card,
page .order-card,
page .empty-card,
page .join-card,
page .room-card,
page .owner-card,
page .invite-card,
page .member-card,
page .stats-card,
page .module-card,
page .week-card,
page .event-card,
page .player-card,
page .stage-card,
page .hint-card {
  background: var(--xc-surface) !important;
  border-color: var(--xc-border) !important;
  border-radius: 24rpx !important;
  box-shadow: var(--xc-shadow-card) !important;
  color: var(--xc-ink) !important;
}

page .state-card,
page .selected,
page .options,
page .answer-input,
page .audio-row,
page .audio-wave-player,
page .owner-card,
page .stat-box,
page .metric,
page .word,
page .tool-button,
page .plain-btn,
page .small-button,
page .locale-button,
page .segment,
page .difficulty-btn,
page .group-tab,
page .outline-btn,
page .text-btn,
page .remove-btn,
page .logout-button,
page .stage-group-trigger,
page .stage-btn,
page .sheet-close,
page .status-chip,
page .tag {
  border-color: var(--xc-border) !important;
}

page button {
  border-radius: 18rpx !important;
  font-weight: 800 !important;
  letter-spacing: 0 !important;
  transition: transform 0.12s ease, box-shadow 0.12s ease;
}

page button:active,
page .start-action:active,
page .continue-action:active,
page .spotlight-action:active {
  transform: translateY(3rpx);
}

page .primary-btn,
page .primary-button,
page .pay-button,
page .button,
page .access-button,
page .nav-button.primary,
page .logout-button.primary,
page .start-action,
page .continue-action,
page .spotlight-action {
  align-items: center !important;
  background: linear-gradient(180deg, var(--xc-button-primary-top) 0%, var(--xc-button-primary-mid) 58%, var(--xc-button-primary-bottom) 100%) !important;
  border: 1px solid #087064 !important;
  border-bottom: 7rpx solid #07564f !important;
  box-shadow:
    inset 0 2rpx 0 rgba(255, 255, 255, 0.34),
    0 10rpx 0 rgba(7, 86, 79, 0.1),
    0 14rpx 26rpx rgba(18, 132, 117, 0.2) !important;
  color: #ffffff !important;
  display: flex !important;
  font-size: 28rpx !important;
  justify-content: center !important;
  min-height: 84rpx !important;
}

page .plain-btn,
page .secondary-button,
page .tool-button,
page .filter-btn,
page .mode-btn,
page .level-btn,
page .icon-btn,
page .retry-button,
page .small-button,
page .word,
page .scope-tab,
page .nav-button.secondary,
page .segment,
page .difficulty-btn,
page .group-tab,
page .outline-btn,
page .text-btn,
page .remove-btn,
page .stage-group-trigger,
page .stage-btn,
page .sheet-close {
  align-items: center !important;
  background: linear-gradient(180deg, var(--xc-button-secondary-top) 0%, var(--xc-button-secondary-bottom) 100%) !important;
  border: 1px solid var(--xc-border) !important;
  border-bottom: 6rpx solid var(--xc-button-secondary-edge) !important;
  box-shadow:
    inset 0 2rpx 0 rgba(255, 255, 255, 0.9),
    0 8rpx 18rpx rgba(16, 24, 40, 0.06) !important;
  color: var(--xc-ink) !important;
  display: flex !important;
  font-size: 26rpx !important;
  justify-content: center !important;
  min-height: 76rpx !important;
}

page .filter-btn.active,
page .mode-btn.active,
page .level-btn.active,
page .small-button.active,
page .selected-word,
page .scope-tab.active,
page .panel-tab.active,
page .segment.active,
page .difficulty-btn.active,
page .group-tab.active,
page .stage-btn.active {
  background: linear-gradient(180deg, var(--xc-button-primary-top) 0%, var(--xc-button-primary-mid) 58%, var(--xc-button-primary-bottom) 100%) !important;
  border-color: #087064 !important;
  border-bottom-color: #07564f !important;
  color: #ffffff !important;
}

page .scope-tab.active,
page .group-tab.active,
page .segment.active,
page .mode-btn.active,
page .level-btn.active,
page .small-button.active {
  box-shadow:
    inset 0 2rpx 0 rgba(255, 255, 255, 0.34),
    0 8rpx 0 rgba(7, 86, 79, 0.1),
    0 12rpx 22rpx rgba(18, 132, 117, 0.18) !important;
}

page button[disabled],
page .tool-button[disabled],
page .plain-btn[disabled],
page .primary-btn[disabled],
page .nav-button[disabled],
page .segment[disabled],
page .difficulty-btn[disabled],
page .stage-btn[disabled] {
  border-bottom-width: 3rpx !important;
  box-shadow: none !important;
  opacity: 0.48 !important;
}

page .error-card,
page .result-card.danger,
page .result.danger,
page .completed-card.failed,
page .pop-card.wrong {
  background: var(--xc-danger-bg) !important;
  border-color: var(--xc-danger-border) !important;
  color: var(--xc-danger-text) !important;
}

page .result-card.success,
page .result.success {
  background: #eaf8ef !important;
  border-color: #a6d9bb !important;
  color: #166534 !important;
}

page .notice-card,
page .order-card.status-pending {
  background: var(--xc-warning-bg) !important;
  border-color: var(--xc-warning-border) !important;
  color: var(--xc-warning-text) !important;
}

page .primary-btn,
page .nav-button.primary,
page .scope-tab.active,
page .small-button.active,
page .start-action,
page .player-pill,
page .active-option,
page .featured-pill {
  background: linear-gradient(180deg, var(--xc-button-primary-top) 0%, var(--xc-button-primary-mid) 58%, var(--xc-button-primary-bottom) 100%) !important;
  border-color: var(--xc-primary) !important;
  color: #ffffff !important;
}

page .pinyin,
page .side-label,
page .link-text,
page .overview-label,
page .summary-label,
page .continue-label,
page .spotlight-label,
page .progress-pill,
page .value,
page .summary-card.strong .label,
page .summary-card.strong .value {
  color: var(--xc-primary) !important;
}

page .side-label,
page .progress-pill,
page .status-chip.learned,
page .lesson-count,
page .overview-pill,
page .tag,
page .word {
  background: var(--xc-primary-soft) !important;
  border-color: var(--xc-border-strong) !important;
}

page .lesson-count,
page .overview-pill {
  color: var(--xc-blue) !important;
  background: var(--xc-blue-soft) !important;
}

page .card-mark,
page .mark,
page .avatar {
  box-shadow: none !important;
}

page .tool-button,
page .plain-btn,
page .retry-button,
page .small-button,
page .word,
page .scope-tab,
page .nav-button.secondary,
page .segment,
page .difficulty-btn,
page .group-tab,
page .outline-btn,
page .text-btn,
page .stage-group-trigger,
page .stage-btn,
page .sheet-close {
  background: linear-gradient(180deg, var(--xc-button-secondary-top) 0%, var(--xc-button-secondary-bottom) 100%) !important;
  color: var(--xc-ink) !important;
}

page .remove-btn {
  background: linear-gradient(180deg, #fff8f6 0%, var(--xc-danger-bg) 100%) !important;
  border-bottom-color: #efc0b4 !important;
  border-color: var(--xc-danger-border) !important;
  color: var(--xc-danger-text) !important;
}

page .selected-word {
  background: linear-gradient(180deg, var(--xc-button-primary-top) 0%, var(--xc-button-primary-mid) 58%, var(--xc-button-primary-bottom) 100%) !important;
  border-color: #087064 !important;
  border-bottom-color: #07564f !important;
  box-shadow:
    inset 0 2rpx 0 rgba(255, 255, 255, 0.34),
    0 8rpx 0 rgba(7, 86, 79, 0.1),
    0 12rpx 22rpx rgba(18, 132, 117, 0.18) !important;
  color: #ffffff !important;
}

page .progress-fill {
  background: linear-gradient(90deg, var(--xc-yellow) 0%, #8ff0df 100%) !important;
  border: 0 !important;
  box-shadow: none !important;
}

page .access-pill,
page .featured-pill {
  border-radius: 16rpx !important;
  font-weight: 900 !important;
}

page .access-pill {
  background: var(--xc-primary-soft) !important;
  border-color: var(--xc-border-strong) !important;
  color: var(--xc-primary) !important;
}

page .stroke-panel {
  background: var(--xc-surface) !important;
  border-color: var(--xc-border) !important;
  box-shadow: var(--xc-shadow-raised) !important;
}

page .arrow {
  color: var(--xc-muted-soft) !important;
  font-size: 34rpx !important;
  font-weight: 850 !important;
  line-height: 1 !important;
}

page input,
page textarea {
  color: var(--xc-ink) !important;
}

page .hanzi {
  font-size: 112rpx !important;
  font-weight: 400 !important;
}

page .pinyin {
  font-size: 34rpx !important;
  letter-spacing: 0 !important;
}

</style>
