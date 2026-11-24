package com.dayflow.app.domain.usecase

/** Lowercase, strip '#', no spaces, de-duplicated. */
fun normalizeTags(tags: List<String>): List<String> =
    tags.map { it.trim().removePrefix("#").lowercase().replace(Regex("\\s+"), "-") }
        .filter { it.isNotEmpty() }.distinct()
