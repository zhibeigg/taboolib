package taboolib.module.kether

import org.tabooproject.reflex.ClassMethod
import org.tabooproject.reflex.ReflexClass
import taboolib.common.Inject
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.io.taboolibPath
import taboolib.common.platform.Awake
import taboolib.common.platform.function.getOpenContainers
import taboolib.common.platform.function.pluginId
import taboolib.common.util.asList
import java.util.function.Supplier

/**
 * TabooLibKotlin
 * taboolib.module.kether.KetherLoader
 *
 * @author sky
 * @since 2021/2/6 3:33 下午
 */
@Awake
@Inject
class KetherLoader : ClassVisitor(0) {

    override fun visit(method: ClassMethod, owner: ReflexClass) {
        if (method.isAnnotationPresent(KetherParser::class.java) && method.returnType == ScriptActionParser::class.java) {
            val instance = findInstance(owner)
            val parser = (if (instance == null) method.invokeStatic() else method.invoke(instance)) as ScriptActionParser<*>
            val annotation = method.getAnnotation(KetherParser::class.java)
            val value = annotation.property<Any>("value")?.asList()?.toTypedArray() ?: arrayOf()
            val namespace = annotation.property("namespace", "kether")
            registerParser(parser, value, namespace, annotation.property("shared", false))
        } else if (method.isAnnotationPresent(KetherProperty::class.java) && method.returnType == ScriptProperty::class.java) {
            val instance = findInstance(owner)
            val property = (if (instance == null) method.invokeStatic() else method.invoke(instance)) as ScriptProperty<*>
            val annotation = method.getAnnotation(KetherProperty::class.java)
            val bind = annotation.property<Class<*>>("bind") ?: error("KetherProperty bind is null")
            registerProperty(property, bind, annotation.property("shared", false))
        }
    }

    override fun getLifeCycle(): LifeCycle {
        return LifeCycle.LOAD
    }

    @Inject
    companion object {

        val sharedParser = ArrayList<Pair<Array<String>, String>>()
        val sharedScriptProperty = ArrayList<Pair<String, String>>()

        @Awake(LifeCycle.DISABLE)
        private fun cancel() {
            getOpenContainers().forEach { remote ->
                sharedParser.forEach {
                    remote.call(StandardChannel.REMOTE_REMOVE_ACTION, arrayOf(it.first, it.second))
                }
                sharedScriptProperty.forEach {
                    remote.call(StandardChannel.REMOTE_REMOVE_PROPERTY, arrayOf(it.first, it.second))
                }
            }
        }

        /** 注册 Parser */
        fun registerParser(parser: ScriptActionParser<*>, name: Array<String>, namespace: String = "kether", shared: Boolean = false) {
            // 共享 Parser 到所有 TabooLib 插件
            if (shared) {
                sharedParser += name to namespace
                getOpenContainers().forEach { it.call(StandardChannel.REMOTE_ADD_ACTION, arrayOf(pluginId, name, namespace)) }
            }
            // 注册到自己
            name.forEach { Kether.addAction(it, parser, namespace) }
        }

        /** 注册 Property */
        fun registerProperty(property: ScriptProperty<*>, bind: Class<*>, shared: Boolean = false) {
            if (shared) {
                var name = bind.name
                name = if (name.startsWith(taboolibPath)) "@${name.substring(taboolibPath.length)}" else name
                sharedScriptProperty += name to property.id
                getOpenContainers().forEach { it.call(StandardChannel.REMOTE_ADD_PROPERTY, arrayOf(pluginId, name, property)) }
            }
            Kether.addScriptProperty(bind, property)
        }
    }
}