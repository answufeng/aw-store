package com.answufeng.store

/**
 * SharedPreferences 迁移到 MMKV 的结果。
 *
 * @property totalKeys SP 中的总键数
 * @property successCount 成功迁移的键数
 * @property failedCount 迁移失败的键数
 * @property skippedKeys 跳过的键列表（null 值或未知类型）
 */
data class MigrationResult(
    val totalKeys: Int,
    val successCount: Int,
    val failedCount: Int,
    val skippedKeys: List<String>
) {
    /** 是否全部迁移成功（无失败、无跳过） */
    val isSuccess: Boolean get() = failedCount == 0 && skippedKeys.isEmpty()

    override fun toString(): String {
        return "MigrationResult(total=$totalKeys, success=$successCount, failed=$failedCount, skipped=${skippedKeys.size})"
    }
}
