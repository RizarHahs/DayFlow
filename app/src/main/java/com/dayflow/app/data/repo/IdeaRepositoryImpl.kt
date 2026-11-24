package com.dayflow.app.data.repo

import com.dayflow.app.data.local.IdeaDao
import com.dayflow.app.data.local.IdeaEntity
import com.dayflow.app.domain.model.Idea
import com.dayflow.app.domain.model.IdeaDraft
import com.dayflow.app.domain.model.ItemKind
import com.dayflow.app.domain.repo.IdeaRepository
import com.dayflow.app.domain.usecase.normalizeTags
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdeaRepositoryImpl @Inject constructor(private val dao: IdeaDao) : IdeaRepository {

    override fun observeAll(): Flow<List<Idea>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun get(id: Long): Idea? = dao.get(id)?.toDomain()

    override suspend fun getDraft(id: Long): IdeaDraft? =
        dao.get(id)?.let { IdeaDraft(it.id, it.text, it.description, parseTags(it.tags)) }

    override suspend fun save(draft: IdeaDraft): Long {
        val now = System.currentTimeMillis()
        val tags = normalizeTags(draft.tags).joinToString(" ")
        val existing = if (draft.id != 0L) dao.get(draft.id) else null
        return if (existing == null) {
            dao.insert(
                IdeaEntity(
                    text = draft.text.trim(), description = draft.description.trim(),
                    createdAt = now, updatedAt = now, pinned = false, archived = false,
                    tags = tags, convertedToKind = null,
                )
            )
        } else {
            dao.update(existing.copy(text = draft.text.trim(), description = draft.description.trim(), tags = tags, updatedAt = now))
            existing.id
        }
    }

    override suspend fun togglePin(id: Long) {
        dao.get(id)?.let { dao.update(it.copy(pinned = !it.pinned)) }
    }

    override suspend fun setArchived(id: Long, archived: Boolean) {
        dao.get(id)?.let { dao.update(it.copy(archived = archived, pinned = if (archived) false else it.pinned)) }
    }

    override suspend fun markConverted(id: Long, kind: ItemKind) {
        dao.get(id)?.let { dao.update(it.copy(convertedToKind = kind)) }
    }

    override suspend fun delete(id: Long) = dao.delete(id)
}
