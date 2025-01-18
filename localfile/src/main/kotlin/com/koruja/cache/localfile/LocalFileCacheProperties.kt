package com.koruja.cache.localfile

import com.koruja.cache.core.CacheProperties

class LocalFileCacheProperties(
    override val isCacheDebugEnabled: Boolean,
    val baseDir: List<String>,
    val deleteExpiredCache: Boolean = true,
    val enableInMemoryCacheSupport: Boolean = false,
    val fileType: FileType = FileType.TXT,
) : CacheProperties(isCacheDebugEnabled = isCacheDebugEnabled)
