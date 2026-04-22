# 贡献指南

感谢你对 aw-store 的关注！欢迎提交 Issue 和 Pull Request。

## 提交 Issue

- Bug 报告：请包含复现步骤、预期行为和实际行为
- 功能请求：请描述使用场景和期望的 API 设计
- 提问：请先搜索已有 Issue，避免重复

## 提交 Pull Request

1. Fork 本仓库
2. 创建功能分支：`git checkout -b feature/my-feature`
3. 提交变更：`git commit -m 'Add some feature'`
4. 推送分支：`git push origin feature/my-feature`
5. 创建 Pull Request

## 代码规范

- 遵循 [Kotlin 编码规范](https://kotlinlang.org/docs/coding-conventions.html)
- 公共 API 必须有 KDoc 注释
- 新功能请在 `demo` 中提供可运行入口，或在 PR 中写明手测步骤
- 提交信息使用英文，格式参考 [Conventional Commits](https://www.conventionalcommits.org/)

## 开发环境

- JDK 17+
- Android SDK 35
- Kotlin 2.0+

## 构建 & 测试

```bash
./gradlew :aw-store:assembleRelease        # 构建库
./gradlew :aw-store:ktlintCheck
./gradlew :aw-store:lintRelease
./gradlew :demo:assembleRelease            # 构建 Demo
```

## 发版前补充检查（R8）

MMKV native 与 Parcelable/JSON 反射边界需 release 冒烟；升级 MMKV 主版本时同步 README 与 demo 多进程用例。
