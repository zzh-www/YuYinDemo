package com.yuyin.demo.models

import com.squareup.moshi.JsonClass

/***
 * saveMode 保存模式
 *         saveText：0
 *         saveTime：1
 *         saveVoice：2
 * modelMode 模型转写语言
 * model_dict 各语言模型保存路径
 */
@JsonClass(generateAdapter = true)
data class LocalSettings(
    var saveMode: Int,
    var model_dict: MutableMap<String, MutableList<String>>,
    var modelMode: String
) {
    override fun equals(other: Any?): Boolean =
        if (other is LocalSettings) {
            other.saveMode == saveMode && other.modelMode == modelMode && other.model_dict[other.modelMode] == model_dict[modelMode]
        } else {
            false
        }

    fun dictPath() = model_dict[modelMode]?.get(1) ?: ""
    fun modelPath() = model_dict[modelMode]?.get(0) ?: ""
    fun modelDict() = model_dict[modelMode]
    override fun hashCode(): Int {
        var result = saveMode
        result = 31 * result + model_dict.hashCode()
        result = 31 * result + modelMode.hashCode()
        return result
    }
}