package com.example.network

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class GoogleBooksResponse(
    val items: List<GoogleBookItem>?
)

@JsonClass(generateAdapter = true)
data class GoogleBookItem(
    val volumeInfo: VolumeInfo?
)

@JsonClass(generateAdapter = true)
data class VolumeInfo(
    val title: String?,
    val authors: List<String>?,
    val description: String?,
    val imageLinks: ImageLinks?,
    val publishedDate: String?,
    val categories: List<String>?,
    val publisher: String?
)

@JsonClass(generateAdapter = true)
data class ImageLinks(
    val thumbnail: String?,
    val smallThumbnail: String?
)

interface GoogleBooksApi {
    @GET("books/v1/volumes")
    suspend fun searchBooks(@Query("q") query: String): retrofit2.Response<GoogleBooksResponse>
}
