package com.thingspath.domain.usecase

import com.thingspath.data.local.repository.ItemRepository
import com.thingspath.data.model.Item
import javax.inject.Inject

class GetItemByIdUseCase @Inject constructor(
    private val repository: ItemRepository
) {
    /** 同步读内存缓存，用于 UI 即时展示 */
    fun getCached(itemId: Long): Item? = repository.getCachedItemById(itemId)

    /** 挂起函数：缓存优先，未命中时回退 D1 */
    suspend operator fun invoke(itemId: Long): Item? {
        return repository.getItemById(itemId)
    }
}
