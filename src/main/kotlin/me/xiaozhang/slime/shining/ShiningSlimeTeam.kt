package me.xiaozhang.slime.shining

import io.github.sunshinewzy.shining.api.addon.ShiningAddon
import io.github.sunshinewzy.shining.api.event.guide.ShiningGuideOpenEvent
import io.github.sunshinewzy.shining.api.event.guide.ShiningGuideTeamGetAsyncEvent
import io.github.sunshinewzy.shining.core.guide.team.GuideTeam
import io.github.sunshinewzy.shining.core.guide.team.GuideTeams
import io.github.sunshinewzy.shining.objects.ShiningDispatchers
import me.xiaozhangup.slimecargo.manager.IslandManager
import me.xiaozhangup.slimecargo.objects.Island
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object ShiningSlimeTeam : ShiningAddon() {

    private val namespacedKey: NamespacedKey = NamespacedKey("shining", "team")

    override fun onEnable() {
        addonManager.registerListener(ShiningGuideTeamGetAsyncEvent::class.java) {
            val player = it.player
            ShiningDispatchers.launchDB {
                newSuspendedTransaction {
                    player.island()?.let { island ->
                        player.getTeam()?.let { guideTeam ->
                            it.team = guideTeam
                        } ?: run {
                            val newTeam = GuideTeam.create(island.landId, island.landId.toString(), ItemStack(Material.GRASS_BLOCK))!!
                            player.world.setTeam(newTeam.id.value)
                            it.team = newTeam
                        }
                    } ?: run {
                        it.isCancelled = true
                    }
                }
            }
        }

        addonManager.registerListener(ShiningGuideOpenEvent::class.java) {
            val island = it.player.island()
            val uuid = it.player.uniqueId

            if (island == null || (island.owner != uuid && !island.teams.contains(uuid))) { it.isCancelled = true }
        }
    }

    private fun Player.island(): Island? {
        return IslandManager.getLand(world.name).orElse(null)
    }

    private fun World.setTeam(id: Int) {
        persistentDataContainer.set(namespacedKey, PersistentDataType.INTEGER, id)
    }

    private fun Player.getTeam(): GuideTeam? {
        val id = world.persistentDataContainer.getOrDefault(namespacedKey, PersistentDataType.INTEGER, -1)
        return if (id == -1) null
        else GuideTeam.findById(id)
    }
}