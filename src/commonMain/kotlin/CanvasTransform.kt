import de.fabmax.kool.InputManager
import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.*
import de.fabmax.kool.scene.Group
import de.fabmax.kool.scene.Scene

fun Scene.canvasTransform(inputMan: InputManager, name: String? = null, block: CanvasTransform.() -> Unit): CanvasTransform {
    val sit = CanvasTransform(inputMan, this, name)
    sit.block()
    return sit
}

class CanvasTransform(inputMan: InputManager, scene: Scene, name: String? = null) : Group(name), Scene.DragHandler {

    var minZoom = 1e-30
    var maxZoom = 100.0

    var panStep = 1.0

    val translation = MutableVec2d()
    var zoom = 10.0
        set(value) {
            field = value.clamp(minZoom, maxZoom)
        }

    private val mouseTransform = Mat4d()
    val reset: () -> Unit

    override fun handleDrag(dragPtrs: List<InputManager.Pointer>, scene: Scene, ctx: KoolContext) {

    }

    init {
        scene.registerDragHandler(this)
        onUpdate += {
            updateTransform()
        }
        scene.onProcessInput += {


        }

        inputMan.registerKeyListener(InputManager.KEY_CURSOR_LEFT, "left") { translation.add(Vec2d(-panStep, 0.0)) }
        inputMan.registerKeyListener(InputManager.KEY_CURSOR_RIGHT, "right") { translation.add(Vec2d(panStep, 0.0)) }
        inputMan.registerKeyListener(InputManager.KEY_CURSOR_UP, "up") { translation.add(Vec2d(0.0, panStep)) }
        inputMan.registerKeyListener(InputManager.KEY_CURSOR_DOWN, "down") { translation.add(Vec2d(0.0, -panStep)) }

        inputMan.registerKeyListener(InputManager.KEY_NP_PLUS, "zoom in") { zoom *= 1.1 }
        inputMan.registerKeyListener(InputManager.KEY_NP_MINUS, "zoom out") { zoom /= 1.1 }


        val initialZoom = zoom
        val initialTranslation = translation.toVec2f()
        reset = {
            zoom = initialZoom
            translation.set(initialTranslation.toVec2d())
        }
    }

    private fun updateTransform() {
        val z = zoom
        mouseTransform.setIdentity()
        mouseTransform.translate(translation.x, translation.y, 0.0)
        mouseTransform.scale(z, z, 1.0)
        set(mouseTransform)
    }
}