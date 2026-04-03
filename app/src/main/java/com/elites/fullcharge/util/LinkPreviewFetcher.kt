package com.elites.fullcharge.util

import com.elites.fullcharge.data.LinkPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.util.regex.Pattern

object LinkPreviewFetcher {

    private val URL_PATTERN = Pattern.compile(
        "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)",
        Pattern.CASE_INSENSITIVE
    )

    fun extractUrl(text: String): String? {
        val matcher = URL_PATTERN.matcher(text)
        return if (matcher.find()) matcher.group(1) else null
    }

    fun containsUrl(text: String): Boolean {
        return URL_PATTERN.matcher(text).find()
    }

    suspend fun fetchPreview(url: String): LinkPreview? = withContext(Dispatchers.IO) {
        try {
            val document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .timeout(5000)
                .get()

            val title = document.select("meta[property=og:title]").attr("content")
                .ifEmpty { document.select("meta[name=twitter:title]").attr("content") }
                .ifEmpty { document.title() }

            val description = document.select("meta[property=og:description]").attr("content")
                .ifEmpty { document.select("meta[name=twitter:description]").attr("content") }
                .ifEmpty { document.select("meta[name=description]").attr("content") }

            val imageUrl = document.select("meta[property=og:image]").attr("content")
                .ifEmpty { document.select("meta[name=twitter:image]").attr("content") }
                .ifEmpty { document.select("link[rel=image_src]").attr("href") }

            val siteName = document.select("meta[property=og:site_name]").attr("content")
                .ifEmpty {
                    try {
                        java.net.URI(url).host?.removePrefix("www.") ?: ""
                    } catch (e: Exception) {
                        ""
                    }
                }

            // 상대 경로 이미지 URL을 절대 경로로 변환
            val absoluteImageUrl = if (imageUrl.isNotEmpty() && !imageUrl.startsWith("http")) {
                try {
                    val uri = java.net.URI(url)
                    "${uri.scheme}://${uri.host}$imageUrl"
                } catch (e: Exception) {
                    imageUrl
                }
            } else {
                imageUrl
            }

            LinkPreview(
                url = url,
                title = title.ifEmpty { null },
                description = description.ifEmpty { null }?.take(150),
                imageUrl = absoluteImageUrl.ifEmpty { null },
                siteName = siteName.ifEmpty { null }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // 실패해도 기본 프리뷰 반환
            LinkPreview(
                url = url,
                siteName = try {
                    java.net.URI(url).host?.removePrefix("www.")
                } catch (e: Exception) {
                    null
                }
            )
        }
    }
}
