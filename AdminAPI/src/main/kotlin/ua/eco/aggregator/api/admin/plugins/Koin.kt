package ua.eco.aggregator.api.admin.plugins

import ua.eco.aggregator.backend.BackendKoinModule
import ua.eco.aggregator.backend.RecordService
import io.ktor.server.application.*
import ua.eco.aggregator.base.model.AggregatedRecordClasses
import org.koin.core.context.GlobalContext
import org.koin.core.qualifier.named
import org.koin.java.KoinJavaComponent
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(BackendKoinModule)
        GlobalContext.startKoin(this)
    }
}

fun injectRecordServices(): List<RecordService<*, *>> {
    val recordServices = mutableListOf<RecordService<*, *>>()

    for (record in AggregatedRecordClasses) {
        recordServices.add(
            KoinJavaComponent.inject<RecordService<*, *>>(
                clazz = RecordService::class.java,
                qualifier = named(record.recordClass.simpleName!!)
            ).value
        )
    }

    return recordServices
}