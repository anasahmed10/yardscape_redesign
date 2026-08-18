package com.naslabs.yardscape.map

data class MapRenderReconciliation<T>(
    val added: List<T>,
    val retainedIds: Set<String>,
    val removedIds: Set<String>,
    val selectionChangedIds: Set<String>,
)

fun <T> reconcile(
    previous: Map<String, T>,
    current: Map<String, T>,
    previousSelectedId: String? = null,
    selectedId: String? = null,
): MapRenderReconciliation<T> {
    val retainedIds = previous.keys.intersect(current.keys)
    return MapRenderReconciliation(
        added = current.filterKeys { it !in previous }.values.toList(),
        retainedIds = retainedIds,
        removedIds = previous.keys - current.keys,
        selectionChangedIds = setOfNotNull(previousSelectedId, selectedId)
            .filter { it in previous || it in current }
            .toSet(),
    )
}
