package dev.mr3n.survivalonigo

import dev.moru3.minepie.Executor.Companion.runTaskTimer
import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.Vector
import kotlin.math.abs
import kotlin.math.pow

class SurvivalOnigo: JavaPlugin() {
    private var target: Player? = null
    private val targetLocation = mutableMapOf<World,Location>()

    private var task: BukkitTask? = null

    private var notify = false

    override fun onEnable() {
        this.dataFolder.mkdirs()
        this.runTaskTimer(1L,1L) {
            Bukkit.getOnlinePlayers().forEach { player ->
                val targetLocation = targetLocation[player.world]?:return@forEach
                val playerLocation = player.location
                val targetVector = targetLocation.toVector()
                val playerVector = playerLocation.toVector()
                val direction = targetVector.subtract(playerVector)
                val lookAtTarget = playerLocation.clone().setDirection(direction)
                var yaw = lookAtTarget.yaw.toInt()
                if(yaw > 180) { yaw -= 360 }
                if(yaw < -180) { yaw += 360 }
                var result = yaw-playerLocation.yaw.toInt()
                if(result > 180) { result -= 360 }
                if(result < -180) { result += 360 }
                var range = result..result
                if(abs(result) < 10) {
                    val f = abs(((10-abs(result))/2) * 2)
                    range = -f..f
                }
                var actionBar = ""
                (-50..50).forEach { count ->
                    actionBar += if(count in range) { "${ChatColor.RED}|${ChatColor.WHITE}" } else { "|" }
                }
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,*TextComponent.fromLegacyText(actionBar))
            }
        }
        this.runTaskTimer(20,20) {
            if(this.target == null || this.task == null) { return@runTaskTimer }
            Bukkit.getOnlinePlayers().forEach { player ->
                if(player.gameMode == GameMode.SPECTATOR) { return@forEach }
                val location = player.location
                this.dataFolder.resolve("${player.name}.text").appendText("${location.blockX} ${location.blockY} ${location.blockZ} ${location.world?.name}${System.lineSeparator()}")
            }
        }
    }

    private fun refreshTask(rate: Int) {
        this.task?.cancel()
        this.task = this.runTaskTimer(rate * 20L, rate * 20L) {
            val target = target?:return@runTaskTimer
            targetLocation[target.world] = target.location
            if(this.notify) { target.sendMessage("${ChatColor.RED}位置情報が更新されました。") }
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        try {
            when(command.name) {
                "survivalonigo" -> {
                    when(args.getOrNull(0)) {
                        "target" -> {
                            when(args.getOrNull(1)) {
                                "set" -> {
                                    val player = Bukkit.getPlayer(args.getOrNull(2)?:throw IllegalArgumentException("/survivalonigo target set <プレイヤー> で指定する必要があります。"))
                                    this.target = player
                                    sender.sendMessage("${ChatColor.WHITE}ターゲットを${ChatColor.RED}${player?.name}${ChatColor.WHITE}に設定しました。")
                                    targetLocation.clear()
                                }
                                null -> {
                                    val target = target?.name?:throw IllegalArgumentException("ターゲットが設定されていません。")
                                    sender.sendMessage("${ChatColor.WHITE}現在のターゲットは${ChatColor.RED}${target}${ChatColor.WHITE}です。")
                                }
                            }
                        }
                        "refresh" -> {
                            when(args.getOrNull(1)) {
                                "set" -> {
                                    val rate = args.getOrNull(2)?.toIntOrNull()?:throw IllegalArgumentException("/survivalonigo refresh set <秒> で指定する必要があります。")
                                    refreshTask(rate)
                                    sender.sendMessage("${ChatColor.WHITE}コンパスのリフレッシュレートを${ChatColor.RED}${rate}${ChatColor.WHITE}に設定しました。")
                                }
                            }
                        }
                        "notify" -> {
                            when(args.getOrNull(1)) {
                                "set" -> {
                                    this.notify = args.getOrNull(2)?.toBooleanStrictOrNull()?:throw IllegalArgumentException("/survivalonigo notify set <true/false>")
                                }
                            }
                        }
                        "end" -> {
                            this.target = null
                            this.task?.cancel()
                            this.targetLocation.clear()

                        }
                    }
                }
            }
        } catch(e: IllegalArgumentException) {
            sender.sendMessage("${ChatColor.RED}${e.message}")
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): MutableList<String>? {
        when(args.size) {
            1 -> {
                return listOf("target","refresh","notify","end").filter { it.startsWith(args[0]) }.toMutableList()
            }
            2 -> {
                return listOf("set").filter { it.startsWith(args[1]) }.toMutableList()
            }
            3 -> {
                when(args.getOrNull(0)) {
                    "target" -> {
                        when(args.getOrNull(1)) {
                            "set" -> {
                                return Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1]) }.toMutableList()
                            }
                        }
                    }
                    "refresh" -> {
                        when(args.getOrNull(1)) {
                            "set" -> {
                                return mutableListOf()
                            }
                        }
                    }
                }
            }
        }
        return null
    }
}