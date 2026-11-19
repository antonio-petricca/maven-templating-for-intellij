package com.github.intellij.plugins.mt4ij

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service

/**
 * Parent disposable for the plugin message bus connections of a project.
 */
@Service(Service.Level.PROJECT)
class MessageBusParent : Disposable {
    override fun dispose() {
        // Nothing to release: child connections are disposed with this service.
    }
}
