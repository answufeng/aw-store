# aw-store Demo 功能矩阵

主界面按 **5 个分类、12 个场景** 组织（取代原先 30+ 零散按钮），底部固定 **运行日志** 面板。

| 分类 | 场景 | 覆盖能力 |
|------|------|----------|
| 入门 | 基本读写 | 属性委托、7 种基础类型 |
| 入门 | Nullable 语义 | 赋 null 删键、default 占位 |
| 数据类型 | Parcelable / JSON / 字节 | 复杂类型与命令式 JSON |
| 实例与进程 | 加密 / 隔离 / 多进程 / SP 迁移 | mmapId、CryptKey、effectiveMmapId、SpMigration |
| 进阶 API | 监听 / 命令式 / getOrPut | registerOnKeyChanged、registerContentChange、edit、TTL、getOrPutJson |
| 调试 | 导出与管理 / 清空 | exportToMap、allKeys、mmkvInstance |

菜单：**使用说明**、**注销监听**、**复制/分享日志**、**清空存储**。

集成前请阅读根目录 README 与 MMKV 官方文档。
