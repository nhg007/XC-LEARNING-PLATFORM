<script lang="ts">
import { applyTabBarLocale } from './i18n'

export default {
  onLaunch() {
    applyTabBarLocale()
  }
}
</script>

<style>
@import url("./assets/fonts/lxgw-wenkai-screen/lxgwwenkaiscreen.css");

@font-face {
  font-family: "XCFangZhengKai";
  font-style: normal;
  font-weight: 400;
  src:
    local("FZKai-Z03S"),
    local("FZKaiTi"),
    local("FZKaiS-Extended"),
    local("FZKaiS-Extended(SIP)"),
    local("方正楷体"),
    local("方正楷体简体");
}

@font-face {
  font-family: "XCKaiLocal";
  font-style: normal;
  font-weight: 400;
  src:
    local("Kaiti SC"),
    local("KaitiSC-Regular"),
    local("STKaiti"),
    local("KaiTi"),
    local("KaiTi_GB2312"),
    local("楷体"),
    local("楷体_GB2312"),
    local("BiauKai"),
    local("DFKai-SB");
}

page {
  --xc-kai-font-family: "XCFangZhengKai", "FZKai-Z03S", "FZKaiTi", "方正楷体", "方正楷体简体", "XCKaiLocal", "Kaiti SC", "STKaiti", "KaiTi", "KaiTi_GB2312", "楷体", "楷体_GB2312", "BiauKai", "DFKai-SB", "LXGW WenKai Screen", serif;
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
