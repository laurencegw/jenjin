package com.binarymonks.jj.core.components

import com.badlogic.gdx.utils.ObjectMap
import kotlin.reflect.KClass

class ComponentMaster {

    private var trackedComponents = ObjectMap<KClass<*>, Component>()
    private var addTrackedComponent = ObjectMap<KClass<*>, Component>()
    private var removeTrackedComponents = ObjectMap<KClass<*>, Component>()

    fun update() {
        clean()
        for (entry in trackedComponents.entries()) {
            if (entry.value.isDone()) {
                removeTrackedComponents.put(entry.key, entry.value)
            } else {
                entry.value.update()
            }
        }
    }

    fun clean() {
        for (entry in removeTrackedComponents.entries()) {
            entry.value.onRemoveFromSceneWrapper()
            trackedComponents.remove(entry.key)
        }
        removeTrackedComponents.clear()
        for (entry in addTrackedComponent.entries()) {
            trackedComponents.put(entry.key, entry.value)
        }
        addTrackedComponent.clear()
    }

    fun addComponent(component: Component) {
        if (!component.type().java.isAssignableFrom(component.javaClass)) {
            throw Exception("Your component ${component::class.simpleName} is not an instance of its type")
        }
        if (trackedComponents.containsKey(component.type()) || addTrackedComponent.containsKey(component.type())) {
            throw RuntimeException("Your are adding a tracked component that will clobber another component of type ${component.type().simpleName}")
        }
        addTrackedComponent.put(component.type(), component)
    }

    fun <T : Component> getComponent(type: KClass<T>): T? {
        if (trackedComponents.containsKey(type)) {
            @Suppress("UNCHECKED_CAST")
            return trackedComponents.get(type) as T
        }
        if (addTrackedComponent.containsKey(type)) {
            @Suppress("UNCHECKED_CAST")
            return addTrackedComponent.get(type) as T
        }
        return null
    }

    fun onAddToWorld() {
        addTrackedComponent.forEach { it.value.onAddToWorldWrapper() }
        trackedComponents.forEach { it.value.onAddToWorldWrapper() }
    }

    fun destroy() {
        clean()
        trackedComponents.forEach { it.value.onRemoveFromWorld() }
    }

    fun onScenePool() {
        addTrackedComponent.forEach { it.value.onScenePool() }
        trackedComponents.forEach { it.value.onScenePool() }
    }

}
