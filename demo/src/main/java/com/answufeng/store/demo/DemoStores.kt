package com.answufeng.store.demo

import android.os.Parcelable
import com.answufeng.store.CryptKey
import com.answufeng.store.MmkvDelegate
import kotlinx.parcelize.Parcelize

object UserStore : MmkvDelegate() {
    var token by string()
    var userId by long()
    var isLoggedIn by boolean()
    var score by float()
    var tags by stringSet()
    var nickname by nullableString()
    var age by nullableInt()
    var ratio by double()
    var nullableTimestamp by nullableLong()
    var nullableScore by nullableFloat()
    var nullableRatio by nullableDouble()
    var nullableEnabled by nullableBoolean()
}

object SecureStore : MmkvDelegate(
    mmapId = "secure_demo",
    secureCryptKey = CryptKey.fromString("aw-store-demo-secret-do-not-use-in-prod"),
) {
    var password by string()
}

object IsolatedStore : MmkvDelegate(mmapId = "isolated") {
    var data by string()
}

object MultiProcessStore : MmkvDelegate(mmapId = "shared", multiProcess = true) {
    var counter by int()
}

object ParcelableStore : MmkvDelegate(mmapId = "parcelable_demo") {
    var profile by parcelable<UserProfile>()
}

object BytesStore : MmkvDelegate(mmapId = "bytes_demo") {
    var binaryData by bytes()
    var nullableBinaryData by nullableBytes()
}

object NullableSetStore : MmkvDelegate(mmapId = "nullable_set_demo") {
    var tags by nullableStringSet()
}

object NullableDefaultStore : MmkvDelegate(mmapId = "nullable_default_demo") {
    var label by nullableString(default = "（缺省：从未写入）")
}

object JsonStore : MmkvDelegate(mmapId = "json_demo") {
    var user by json<UserInfo>()
}

@Parcelize
data class UserProfile(val name: String, val age: Int) : Parcelable

data class UserInfo(val name: String, val email: String, val level: Int)
