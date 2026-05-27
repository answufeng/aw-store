package com.answufeng.store.demo

import androidx.annotation.StringRes

data class DemoSection(
    @StringRes val titleRes: Int,
    val items: List<DemoItem>,
)

data class DemoItem(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val destructive: Boolean = false,
)

object DemoCatalog {
    val sections: List<DemoSection> = listOf(
        DemoSection(
            R.string.section_basics,
            listOf(
                DemoItem("basics_rw", R.string.demo_basics_rw_title, R.string.demo_basics_rw_sub),
                DemoItem("nullable", R.string.demo_nullable_title, R.string.demo_nullable_sub),
            ),
        ),
        DemoSection(
            R.string.section_types,
            listOf(
                DemoItem("complex_types", R.string.demo_types_title, R.string.demo_types_sub),
            ),
        ),
        DemoSection(
            R.string.section_instances,
            listOf(
                DemoItem("secure", R.string.demo_secure_title, R.string.demo_secure_sub),
                DemoItem("isolation", R.string.demo_isolation_title, R.string.demo_isolation_sub),
                DemoItem("multi_process", R.string.demo_multi_process_title, R.string.demo_multi_process_sub),
                DemoItem("sp_migration", R.string.demo_migration_title, R.string.demo_migration_sub),
            ),
        ),
        DemoSection(
            R.string.section_advanced,
            listOf(
                DemoItem("listeners", R.string.demo_listeners_title, R.string.demo_listeners_sub),
                DemoItem("imperative", R.string.demo_imperative_title, R.string.demo_imperative_sub),
                DemoItem("get_or_put", R.string.demo_get_or_put_title, R.string.demo_get_or_put_sub),
            ),
        ),
        DemoSection(
            R.string.section_debug,
            listOf(
                DemoItem("debug_tools", R.string.demo_debug_title, R.string.demo_debug_sub),
                DemoItem("clear_all", R.string.demo_clear_title, R.string.demo_clear_sub, destructive = true),
            ),
        ),
    )
}
