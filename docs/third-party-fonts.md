# 第三方字体

## Maoken YingBi KaiShu

- 用途：学生端 Web 和 UniApp H5 的汉字/句子展示，保证手机和桌面端使用统一的硬笔楷书字形。
- 来源：用户提供的 `MaokenYingBiKaiShuJ_0.20.ttf`。
- 许可证：字体授权请以字体随附说明为准，提交或发布前需要确认允许在项目中内置分发。
- 加载方式：当前直接内置 TTF 文件，浏览器和 H5 可通过 `@font-face` 加载。
- 内部字体名：项目统一注册为 `XCMaokenYingBiKai`，业务样式通过 `--xc-hanzi-font-family` 使用；旧的 `--xc-kai-font-family` 保留为兼容别名。
