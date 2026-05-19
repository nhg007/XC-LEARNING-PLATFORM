# 第三方字体

## LXGW WenKai Screen

- 用途：学生端 Web 和 UniApp H5 的汉字/句子展示，在未接入授权方正楷体文件时作为开源楷体兜底。
- 来源：`lxgw-wenkai-screen-webfont` 1.7.0。
- 许可证：字体文件遵循 SIL Open Font License，随字体目录保留 `OFL.txt`。
- 加载方式：保留官方 `unicode-range` 分包 CSS，浏览器只会按页面实际字符加载对应 `woff2` 子集。

## 方正楷体

- 用途：汉字学习卡片的首选楷体，字形更接近教材和字典里的规范楷书。
- 当前接入方式：优先使用用户设备已安装的 `FZKai-Z03S`、`FZKaiTi`、`方正楷体` 等本地字体名。
- 后续内置方式：如需所有设备完全一致，需采购/确认授权后放入项目字体目录，再通过 `@font-face` 指向授权字体文件。
