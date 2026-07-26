package com.mustafashakir.peek.domain.usecase

import java.net.URI

class ExtractUrlFromTextUseCase {
    operator fun invoke(text: CharSequence?): String? {
        val candidate = URL_PATTERN.find(text?.toString().orEmpty())
            ?.value
            ?.trimEnd(*TRAILING_PUNCTUATION)
            ?: return null

        return runCatching { URI(candidate) }
            .getOrNull()
            ?.takeIf { uri ->
                uri.scheme.equals("http", ignoreCase = true) ||
                    uri.scheme.equals("https", ignoreCase = true)
            }
            ?.takeIf { it.host?.isNotBlank() == true }
            ?.toASCIIString()
    }

    private companion object {
        val URL_PATTERN = Regex("https?://\\S+", RegexOption.IGNORE_CASE)
        val TRAILING_PUNCTUATION = charArrayOf('.', ',', ';', ':', '!', '?', ')', ']', '}', '\'', '"')
    }
}
