# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-04-13

### Added
- Initial release of aw-store
- Kotlin property delegate API based on MMKV
- 11 base data types: String, Int, Long, Float, Double, Boolean, ByteArray, Set\<String\>, Parcelable, Serializable, JSON
- 8 nullable delegates: nullableString, nullableInt, nullableLong, nullableFloat, nullableDouble, nullableBoolean, nullableBytes, nullableStringSet
- `nullableString()` delegate for null-aware string storage
- Multi-instance isolation via `mmapId`
- AES-CFB encryption via `cryptKey` and `CryptKey`
- `SpMigration.migrate()` with `MigrationResult` for SharedPreferences migration
- `registerContentChange()` / `unregisterContentChange()` for data change listening
- `sync()` / `async()` for write strategy control
- `AwStoreLogger` with optional debug logging
- `AwStore.rootDir` property for querying storage root directory
- JUnit-based unit tests for AwStore and MigrationResult
- Instrumented tests for MmkvDelegate and SpMigration
- Consumer ProGuard rules for public API and Parcelable
