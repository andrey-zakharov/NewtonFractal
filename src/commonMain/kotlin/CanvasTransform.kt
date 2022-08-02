import de.fabmax.kool.InputManager
import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.*
import de.fabmax.kool.pipeline.RenderPass
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
    private val mouseDrag = MutableVec2d()
    val reset: () -> Unit

    var debug: String? = null

    override fun handleDrag(dragPtrs: List<InputManager.Pointer>, scene: Scene, ctx: KoolContext) {
        //simple
        debug = dragPtrs.joinToString("\n") { "x=${it.x} y=${it.y} ${it.deltaX} ${it.deltaY} dragDelta=${it.dragDeltaX} ${it.dragDeltaY}" }
        // raycast to xy
        // move by dragDelta
        mouseDrag += dragPtrs.fold(MutableVec2d(.0, .0)) { v, ptr -> v.add(Vec2d(-ptr.deltaX, ptr.deltaY)) }
        // totalDragDelta /
    }

    init {
        scene.registerDragHandler(this)
        onUpdate += {
            updateDrags(it.renderPass)
            updateTransform()
        }
        scene.onProcessInput += {


        }

        inputMan.registerKeyListener(InputManager.KEY_CURSOR_LEFT, "left", { it.isPressed }) { translation.add(Vec2d(-panStep * zoom, 0.0)) }
        inputMan.registerKeyListener(InputManager.KEY_CURSOR_RIGHT, "right", { it.isPressed }) { translation.add(Vec2d(panStep * zoom, 0.0)) }
        inputMan.registerKeyListener(InputManager.KEY_CURSOR_UP, "up", { it.isPressed }) { translation.add(Vec2d(0.0, panStep * zoom)) }
        inputMan.registerKeyListener(InputManager.KEY_CURSOR_DOWN, "down", { it.isPressed }) { translation.add(Vec2d(0.0, -panStep * zoom)) }

        inputMan.registerKeyListener(InputManager.KEY_NP_PLUS, "zoom in", { it.isPressed }) { zoom *= 1.1 }
        inputMan.registerKeyListener(InputManager.KEY_NP_MINUS, "zoom out", { it.isPressed }) { zoom /= 1.1 }


        val initialZoom = zoom
        val initialTranslation = translation.toVec2f()
        reset = {
            zoom = initialZoom
            translation.set(initialTranslation.toVec2d())
        }
    }

    private fun updateDrags(rp: RenderPass) {
        if ( rp.viewport.width == 0 || rp.viewport.height == 0 ) return
        translation.x += zoom * ( mouseDrag.x / rp.viewport.width )
        translation.y += zoom * ( mouseDrag.y / rp.viewport.height )
        mouseDrag.x = .0
        mouseDrag.y = .0
    }

    private fun updateTransform() {
        val z = zoom
        mouseTransform.setIdentity()
        mouseTransform.translate(translation.x, translation.y, 0.0)
        mouseTransform.scale(z, z, 1.0)
        set(mouseTransform)
    }
}