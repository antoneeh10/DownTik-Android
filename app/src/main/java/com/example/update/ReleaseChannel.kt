package com.example.update

/**
 * Update distribution channels supported by DownTik.
 */
enum class ReleaseChannel(
    val id: String,
    val displayName: String,
    val description: String,
    val badgeLabel: String
) {
    STABLE(
        id = "stable",
        displayName = "Stable",
        description = "Hanya menerima rilis produksi/stabil resmi yang sudah teruji.",
        badgeLabel = "Stable"
    ),
    BETA(
        id = "beta",
        displayName = "Beta",
        description = "Menerima rilis pra-rilis beta untuk menguji fitur baru.",
        badgeLabel = "Beta"
    );

    companion object {
        fun fromId(id: String?): ReleaseChannel {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: STABLE
        }
    }
}
