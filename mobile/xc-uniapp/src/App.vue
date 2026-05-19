<script lang="ts">
import { applyTabBarLocale } from './i18n'

export default {
  onLaunch() {
    applyTabBarLocale()
  }
}
</script>

<style>
@import url("./assets/fonts/source-han-serif-sc/source-han-serif-sc.css");

page {
  --xc-hanzi-font-family: "XCSourceHanSerif", "Source Han Serif SC", "Source Han Serif CN", "Source Han Serif", "Noto Serif SC", "Noto Serif CJK SC", serif;
  --xc-kai-font-family: var(--xc-hanzi-font-family);
  background: #eef5f7;
  color: #102033;
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

</style>
