package com.mustafashakir.peek.domain.usecase

import com.mustafashakir.peek.domain.model.PreparedMedia
import com.mustafashakir.peek.domain.model.RemoteMedia
import com.mustafashakir.peek.domain.repository.MediaRepository

class PrepareMediaForSharingUseCase(
    private val mediaRepository: MediaRepository,
) {
    suspend operator fun invoke(media: List<RemoteMedia>): Result<List<PreparedMedia>> =
        mediaRepository.prepareForSharing(media)
}

class DownloadMediaUseCase(
    private val mediaRepository: MediaRepository,
) {
    suspend operator fun invoke(media: List<RemoteMedia>): Int =
        mediaRepository.download(media)
}
