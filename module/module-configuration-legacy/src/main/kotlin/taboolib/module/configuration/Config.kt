package taboolib.module.configuration

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Config(val value: String = "config.yml", val migrate: Boolean = false, val autoReload: Boolean = false)