<script lang="ts">
import { applyTabBarLocale } from './i18n'

export default {
  onLaunch() {
    applyTabBarLocale()
  }
}
</script>

<style>
page {
  background: #eef5f7;
  color: #102033;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}

button::after {
  border: none;
}

button {
  align-items: center;
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
