package taboolib.module.nms

import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.chat.ComponentSerializer
import org.bukkit.boss.BossBar
import org.bukkit.craftbukkit.v1_21_R3.util.CraftChatMessage
import org.bukkit.entity.Player
import org.tabooproject.reflex.Reflex.Companion.getProperty
import org.tabooproject.reflex.Reflex.Companion.setProperty
import taboolib.common.UnsupportedVersionException
import taboolib.common.util.unsafeLazy

/**
 * 将 Json 信息设置到 [BossBar] 的标题栏
 */
fun BossBar.setRawTitle(title: String) {
    NMSMessage.instance.setRawTitle(this, title)
}

/**
 * 发送 Json 信息到玩家的标题栏
 */
fun Player.sendRawTitle(title: String?, subtitle: String?, fadein: Int = 0, stay: Int = 20, fadeout: Int = 0) {
    NMSMessage.instance.sendRawTitle(this, title, subtitle, fadein, stay, fadeout)
}

/**
 * 发送 Json 信息到玩家的动作栏
 */
fun Player.sendRawActionBar(message: String) {
    NMSMessage.instance.sendRawActionBar(this, message)
}

/**
 * TabooLib
 * taboolib.module.nms.NMSMessage
 *
 * @author 坏黑
 * @since 2023/8/5 03:47
 */
abstract class NMSMessage {

    abstract fun fromJson(json: String): Any

    abstract fun setRawTitle(bossBar: BossBar, title: String)

    abstract fun sendRawTitle(player: Player, title: String?, subtitle: String?, fadein: Int, stay: Int, fadeout: Int)

    abstract fun sendRawActionBar(player: Player, action: String)

    companion object {

        val instance by unsafeLazy { nmsProxy<NMSMessage>() }
    }
}

// region NMSMessageImpl
class NMSMessageImpl : NMSMessage() {

    override fun fromJson(json: String): Any {
        return CraftChatMessage.fromJSON(json)
    }

    override fun setRawTitle(bossBar: BossBar, title: String) {
        // 1.20.5+
        if (MinecraftVersion.versionId >= 12005) {
            bossBar as CraftBossBar21
            bossBar.handle.setName(CraftChatMessage.fromJSON(title))
        }
        // 1.16+
        // ChatSerializer.a 的返回值由 IChatBaseComponent 变为 IChatMutableComponent
        else if (MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_16)) {
            bossBar as CraftBossBar16
            bossBar.handle.a(NMSChatSerializer16.a(title))
        } else {
            bossBar as CraftBossBar12
            bossBar.getProperty<NMSBossBattleServer12>("handle")!!.a(NMSChatSerializer12.a(title))
        }
    }

    override fun sendRawTitle(player: Player, title: String?, subtitle: String?, fadein: Int, stay: Int, fadeout: Int) {
        if (MinecraftVersion.isLower(MinecraftVersion.V1_9)) {
            throw UnsupportedVersionException()
        }
        if (MinecraftVersion.isUniversal) {
            // 时间
            player.sendPacket(NMSClientboundSetTitlesAnimationPacket(fadein, stay, fadeout))
            // 大标题
            if (title != null) {
                player.sendPacket(NMSClientboundSetTitleTextPacket(CraftChatMessage.fromJSON(title)))
            }
            // 小标题
            if (subtitle != null) {
                player.sendPacket(NMSClientboundSetSubtitleTextPacket(CraftChatMessage.fromJSON(subtitle)))
            }
        } else {
            // 时间
            player.sendPacket(NMSPacketPlayOutTitle16(fadein, stay, fadeout))
            // 大标题
            if (title != null) {
                // 1.16+
                // ChatSerializer.a 的返回值由 IChatBaseComponent 变为 IChatMutableComponent
                if (MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_16)) {
                    player.sendPacket(NMSPacketPlayOutTitle16(NMSEnumTitleAction16.TITLE, NMSChatSerializer16.a(title)))
                } else {
                    player.sendPacket(NMSPacketPlayOutTitle8(NMSEnumTitleAction8.TITLE, NMSChatSerializer8.a(title)))
                }
            }
            // 小标题
            if (subtitle != null) {
                if (MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_16)) {
                    player.sendPacket(NMSPacketPlayOutTitle16(NMSEnumTitleAction16.SUBTITLE, NMSChatSerializer16.a(subtitle)))
                } else {
                    player.sendPacket(NMSPacketPlayOutTitle8(NMSEnumTitleAction8.SUBTITLE, NMSChatSerializer8.a(subtitle)))
                }
            }
        }
    }

    override fun sendRawActionBar(player: Player, action: String) {
        try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, *ComponentSerializer.parse(action))
        } catch (ex: NoSuchMethodError) {
            player.sendPacket(NMSPacketPlayOutChat16().also {
                it.setProperty("b", 2.toByte())
                it.setProperty("components", ComponentSerializer.parse(action))
            })
        }
    }
}
// endregion

// region Typealias
private typealias NMSChatSerializer = net.minecraft.network.chat.IChatBaseComponent.ChatSerializer
// title
private typealias NMSClientboundSetTitlesAnimationPacket = net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket
private typealias NMSClientboundSetTitleTextPacket = net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
private typealias NMSClientboundSetSubtitleTextPacket = net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
private typealias CraftBossBar21 = org.bukkit.craftbukkit.v1_21_R3.boss.CraftBossBar

private typealias NMSChatSerializer16 = net.minecraft.server.v1_16_R3.IChatBaseComponent.ChatSerializer
// title
private typealias NMSEnumTitleAction16 = net.minecraft.server.v1_16_R3.PacketPlayOutTitle.EnumTitleAction
private typealias NMSPacketPlayOutTitle16 = net.minecraft.server.v1_16_R3.PacketPlayOutTitle
// action bar
private typealias NMSPacketPlayOutChat16 = net.minecraft.server.v1_16_R3.PacketPlayOutChat
private typealias CraftBossBar16 = org.bukkit.craftbukkit.v1_16_R3.boss.CraftBossBar

private typealias NMSChatSerializer12 = net.minecraft.server.v1_12_R1.IChatBaseComponent.ChatSerializer
private typealias NMSBossBattleServer12 = net.minecraft.server.v1_12_R1.BossBattleServer
private typealias CraftBossBar12 = org.bukkit.craftbukkit.v1_12_R1.boss.CraftBossBar

private typealias NMSChatSerializer8 = net.minecraft.server.v1_8_R3.IChatBaseComponent.ChatSerializer
private typealias NMSEnumTitleAction8 = net.minecraft.server.v1_8_R3.PacketPlayOutTitle.EnumTitleAction
private typealias NMSPacketPlayOutTitle8 = net.minecraft.server.v1_8_R3.PacketPlayOutTitle
// endregion