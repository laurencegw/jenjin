package com.binarymonks.jj.demo

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.binarymonks.jj.core.JJConfig
import com.binarymonks.jj.demo.d00.D00_Basic
import com.binarymonks.jj.demo.d01.D01_nested_transforms

object DesktopLauncher {
    @JvmStatic fun main(arg: Array<String>) {
        val lwjglConfig = LwjglApplicationConfiguration()
        lwjglConfig.height = 1000
        lwjglConfig.width = 1000

        LwjglApplication(D01_nested_transforms(), lwjglConfig)
    }
}
