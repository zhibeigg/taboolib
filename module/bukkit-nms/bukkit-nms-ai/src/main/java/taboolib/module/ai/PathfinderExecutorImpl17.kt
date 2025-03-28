package taboolib.module.ai

import net.minecraft.world.entity.EntityInsentient
import net.minecraft.world.entity.ai.attributes.GenericAttributes
import net.minecraft.world.entity.ai.control.ControllerJump
import net.minecraft.world.entity.ai.control.ControllerLook
import net.minecraft.world.entity.ai.goal.PathfinderGoal
import net.minecraft.world.entity.ai.goal.PathfinderGoalSelector
import net.minecraft.world.entity.ai.navigation.NavigationAbstract
import net.minecraft.world.level.pathfinder.PathEntity
import org.bukkit.Location
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftEntity
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.tabooproject.reflex.Reflex.Companion.getProperty
import org.tabooproject.reflex.Reflex.Companion.setProperty
import org.tabooproject.reflex.UnsafeAccess.get
import org.tabooproject.reflex.UnsafeAccess.put
import taboolib.module.nms.MinecraftVersion
import java.lang.reflect.Field

/**
 * 该类仅用作生成 ASM 代码，无任何意义
 *
 * @author sky
 * @since 2018-09-20 20:57
 */
class PathfinderExecutorImpl17 : PathfinderExecutor() {

    private var pathEntity: Field? = null
    private val pathfinderGoalSelectorSet: Field
    private var controllerJumpCurrent: Field

    init {
        pathfinderGoalSelectorSet = PathfinderGoalSelector::class.java.getDeclaredField(
            // Paper 1.20.5+ using mojmap
            if (MinecraftVersion.isUniversalCraftBukkit) "availableGoals" else if (MinecraftVersion.majorLegacy >= 12005) "c" else "d"
        )
        pathfinderGoalSelectorSet.isAccessible = true
        controllerJumpCurrent = ControllerJump::class.java.getDeclaredField(
            // Paper 1.20.5+ using mojmap
            if (MinecraftVersion.isUniversalCraftBukkit) "jump" else "a"
        )
        controllerJumpCurrent.isAccessible = true
        for (field in NavigationAbstract::class.java.declaredFields) {
            if (field.type == PathEntity::class.java) {
                pathEntity = field
                break
            }
        }
    }

    override fun getEntityInsentient(entity: LivingEntity): Any {
        return (entity as CraftEntity).handle
    }

    override fun getNavigation(entity: LivingEntity): Any {
        return (getEntityInsentient(entity) as EntityInsentient).navigation
    }

    override fun getControllerJump(entity: LivingEntity): Any {
        val e = getEntityInsentient(entity)
        if (MinecraftVersion.isUniversal) {
            return (e as net.minecraft.world.entity.EntityInsentient).jumpControl
        }
        return (e as EntityInsentient).jumpControl
    }

    override fun getControllerMove(entity: LivingEntity): Any {
        return (getEntityInsentient(entity) as EntityInsentient).moveControl
    }

    override fun getControllerLook(entity: LivingEntity): Any {
        val e = getEntityInsentient(entity)
        if (MinecraftVersion.isUniversal) {
            return (e as net.minecraft.world.entity.EntityInsentient).lookControl
        }
        return (e as EntityInsentient).lookControl
    }

    override fun getGoalSelector(entity: LivingEntity): Any {
        return (getEntityInsentient(entity) as EntityInsentient).goalSelector
    }

    override fun getTargetSelector(entity: LivingEntity): Any {
        return (getEntityInsentient(entity) as EntityInsentient).targetSelector
    }

    override fun getPathEntity(entity: LivingEntity): Any {
        return get(getNavigation(entity), pathEntity!!)!!
    }

    override fun setPathEntity(entity: LivingEntity, pathEntity: Any) {
        put(getNavigation(entity), this.pathEntity!!, pathEntity)
    }

    override fun addGoalAi(entity: LivingEntity, ai: SimpleAi, priority: Int) {
        (getEntityInsentient(entity) as EntityInsentient).goalSelector.addGoal(priority, pathfinderCreator.createPathfinderGoal(ai) as PathfinderGoal)
    }

    override fun addTargetAi(entity: LivingEntity, ai: SimpleAi, priority: Int) {
        (getEntityInsentient(entity) as EntityInsentient).targetSelector.addGoal(priority, pathfinderCreator.createPathfinderGoal(ai) as PathfinderGoal)
    }

    override fun replaceGoalAi(entity: LivingEntity, ai: SimpleAi, priority: Int) {
        replaceGoalAi(entity, ai, priority, null)
    }

    override fun replaceTargetAi(entity: LivingEntity, ai: SimpleAi, priority: Int) {
        replaceTargetAi(entity, ai, priority, null)
    }

    override fun replaceGoalAi(entity: LivingEntity, ai: SimpleAi, priority: Int, name: String?) {
        if (name == null) {
            removeGoal(priority, (getEntityInsentient(entity) as EntityInsentient).goalSelector)
        } else {
            removeGoal(name, (getEntityInsentient(entity) as EntityInsentient).goalSelector)
        }
        addGoalAi(entity, ai, priority)
    }

    override fun replaceTargetAi(entity: LivingEntity, ai: SimpleAi, priority: Int, name: String?) {
        if (name == null) {
            removeGoal(priority, (getEntityInsentient(entity) as EntityInsentient).targetSelector)
        } else {
            removeGoal(name, (getEntityInsentient(entity) as EntityInsentient).targetSelector)
        }
        addTargetAi(entity, ai, priority)
    }

    override fun removeGoalAi(entity: LivingEntity, priority: Int) {
        removeGoal(priority, (getEntityInsentient(entity) as EntityInsentient).goalSelector)
    }

    override fun removeTargetAi(entity: LivingEntity, priority: Int) {
        removeGoal(priority, (getEntityInsentient(entity) as EntityInsentient).targetSelector)
    }

    override fun removeGoalAi(entity: LivingEntity, name: String) {
        removeGoal(name, (getEntityInsentient(entity) as EntityInsentient).goalSelector)
    }

    override fun removeTargetAi(entity: LivingEntity, name: String) {
        removeGoal(name, (getEntityInsentient(entity) as EntityInsentient).targetSelector)
    }

    private fun removeGoal(name: String, targetSelector: Any) {
        val collection = getGoal(targetSelector)
        collection.toList().forEach {
            val a = it!!.getProperty<Any>("goal", remap = true)!!
            if (a.javaClass.name.contains(name)) {
                if (collection is MutableList) {
                    collection.remove(it)
                } else if (collection is MutableSet) {
                    collection.remove(it)
                }
            }
            if (a.javaClass.simpleName == "PathfinderCreatorImpl17" && a.getProperty<Any>("simpleAI")!!.javaClass.name.contains(name)) {
                if (collection is MutableList) {
                    collection.remove(it)
                } else if (collection is MutableSet) {
                    collection.remove(it)
                }
            }
        }
    }

    private fun removeGoal(priority: Int, targetSelector: Any) {
        val collection = getGoal(targetSelector)
        collection.toList().forEach {
            if (it!!.getProperty<Int>("priority") == priority) {
                if (collection is MutableList) {
                    collection.remove(it)
                } else if (collection is MutableSet) {
                    collection.remove(it)
                }
            }
        }
    }

    private fun getGoal(targetSelector: Any): Collection<*> {
        return targetSelector.getProperty<Set<*>>("availableGoals", remap = true)!!
    }

    override fun clearGoalAi(entity: LivingEntity) {
        get<MutableCollection<*>>((getEntityInsentient(entity) as EntityInsentient).goalSelector, pathfinderGoalSelectorSet)?.clear()
    }

    override fun clearTargetAi(entity: LivingEntity) {
        get<MutableCollection<*>>((getEntityInsentient(entity) as EntityInsentient).targetSelector, pathfinderGoalSelectorSet)?.clear()
    }

    override fun getGoalAi(entity: LivingEntity): Iterable<*>? {
        return get<Collection<*>>((getEntityInsentient(entity) as EntityInsentient).goalSelector, pathfinderGoalSelectorSet)
    }

    override fun getTargetAi(entity: LivingEntity): Iterable<*>? {
        return get<Collection<*>>((getEntityInsentient(entity) as EntityInsentient).targetSelector, pathfinderGoalSelectorSet)
    }

    override fun setGoalAi(entity: LivingEntity, ai: Iterable<*>?) {
        put((getEntityInsentient(entity) as EntityInsentient).goalSelector, pathfinderGoalSelectorSet, ai)
    }

    override fun setTargetAi(entity: LivingEntity, ai: Iterable<*>?) {
        put((getEntityInsentient(entity) as EntityInsentient).targetSelector, pathfinderGoalSelectorSet, ai)
    }

    override fun navigationMove(entity: LivingEntity, location: Location): Boolean {
        return navigationMove(entity, location, 0.6)
    }

    override fun navigationMove(entity: LivingEntity, location: Location, speed: Double): Boolean {
        return (getNavigation(entity) as NavigationAbstract).moveTo(location.x, location.y, location.z, speed)
    }

    override fun navigationMove(entity: LivingEntity, target: LivingEntity): Boolean {
        return navigationMove(entity, target, 0.6)
    }

    override fun navigationMove(entity: LivingEntity, target: LivingEntity, speed: Double): Boolean {
        return (getNavigation(entity) as NavigationAbstract).moveTo((target as CraftEntity).handle, speed)
    }

    override fun navigationReach(entity: LivingEntity): Boolean {
        return (getPathEntity(entity) as PathEntity).canReach()
    }

    override fun controllerLookAt(entity: LivingEntity, target: Location) {
        (getControllerLook(entity) as ControllerLook).setLookAt(target.x, target.y, target.z, 10f, 40f)
    }

    override fun controllerLookAt(entity: LivingEntity, target: Entity) {
        (getControllerLook(entity) as ControllerLook).setLookAt((target as CraftEntity).handle, 10f, 40f)
    }

    override fun controllerJumpReady(entity: LivingEntity) {
        (getControllerJump(entity) as ControllerJump).setProperty("jump", true, remap = true)
    }

    override fun controllerJumpCurrent(entity: LivingEntity): Boolean {
        return controllerJumpCurrent.getBoolean(getControllerJump(entity))
    }

    override fun setFollowRange(entity: LivingEntity, value: Double) {
        (getEntityInsentient(entity) as EntityInsentient).getAttribute(GenericAttributes.FOLLOW_RANGE)!!.baseValue = value
    }
}
