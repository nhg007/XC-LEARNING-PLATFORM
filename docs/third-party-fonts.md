# 第三方字体

## Source Han Serif SC / Noto Serif SC

- 用途：学生端 Web 和 UniApp H5 的汉字/句子展示，保证手机和桌面端使用统一的宋体字形。
- 来源：`@fontsource/noto-serif-sc` 5.2.9，Noto Serif SC 为 Source Han Serif 的 Google/Noto 发行名。
- 许可证：字体文件遵循 SIL Open Font License 1.1，随字体目录保留 `OFL.txt`。
- 加载方式：仅内置常规 400 字重，保留 `unicode-range` 分包 CSS，浏览器按页面实际字符加载对应 `woff2` 子集。
- 内部字体名：项目统一注册为 `XCSourceHanSerif`，业务样式通过 `--xc-hanzi-font-family` 使用；旧的 `--xc-kai-font-family` 保留为兼容别名。
