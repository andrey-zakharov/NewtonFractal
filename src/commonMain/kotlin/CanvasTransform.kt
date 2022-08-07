import de.fabmax.kool.InputManager
import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.*
import de.fabmax.kool.pipeline.RenderPass
import de.fabmax.kool.scene.Group
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.scene.animation.SpringDamperDouble
import de.fabmax.kool.util.Viewport

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
    private val panAnimator = Pair(SpringDamperDouble(0.0).apply {
        stiffness = 50.0
    }, SpringDamperDouble(0.0).apply {
        stiffness = 50.0
    })

    private val dragPoints = CircularFifoQueue<Vec2d>(5)
    private val dragPointsTimes = CircularFifoQueue<Double>(5)
    private var lastTimeDrag = .0

    fun unprojectToCanvas(viewport: Viewport, screenPos: Vec2d): Vec2d {
        return Vec2d(
            viewport.aspectRatio * zoom * (screenPos.x / viewport.width),
            zoom * (screenPos.y / viewport.height)
        )
    }

    fun KoolContext.anyBtn(pointers: List<InputManager.Pointer> = this.inputMgr.pointerState.pointers.toList() ) =
        pointers.any { it.buttonMask > 0 }

    override fun handleDrag(dragPtrs: List<InputManager.Pointer>, scene: Scene, ctx: KoolContext) {

        if (dragPtrs.none { !it.isConsumed() }) return
        if (dragPtrs.first().deltaScroll != .0) {
            dragPtrs.first().consume()
        }
        val anyBtn = ctx.anyBtn(dragPtrs)

        // raycast to xy
        // move by dragDelta
        mouseDrag += dragPtrs.fold(MutableVec2d(.0, .0)) { v, ptr ->
            v.add(Vec2d(-ptr.deltaX, ptr.deltaY)) }
        dragPoints.addLast(Vec2d(translation))
        dragPointsTimes.addLast(ctx.time)

        if ( !anyBtn && lastTimeDrag > 0 ) {
            handleDragEnd(dragPtrs, scene, ctx)
            lastTimeDrag = .0
        }

        if ( anyBtn ) {
            if (lastTimeDrag == .0 ) {
                handleDragStart(dragPtrs, scene, ctx)
            }
            lastTimeDrag = ctx.time
        }
    }
    fun handleDragStart(dragPtrs: List<InputManager.Pointer>, scene: Scene, ctx: KoolContext) {


        println("drag start")

    }
    fun handleDragEnd(dragPtrs: List<InputManager.Pointer>, scene: Scene, ctx: KoolContext) {

        println("drag end")
        val lastDrag = dragPtrs.fold(MutableVec2d(.0, .0)) { v, ptr ->
            v.subtract(Vec2d(ptr.deltaX, -ptr.deltaY)) }

        lastDrag.divAssign(ctx.time - lastTimeDrag)

        val lastDrags = MutableVec2d()
        dragPoints.last().subtract(dragPoints.first(), lastDrags)
        lastDrags.divAssign(dragPointsTimes.last() - dragPointsTimes.first())


        val animTime = if ( ctx.inputMgr.isShiftDown || ctx.inputMgr.pointerState.pointers.first().isRightButtonDown ) 5.0 else 1.0 // sec

        lastDrag.scale(animTime)
        lastDrags.plusAssign(translation)
        //debug = lastDrag.toString()

        val (x,y) = panAnimator
println("current: $translation desired: $lastDrags mouseDrag=${mouseDrag} with first: \n" +
        "${dragPoints.first()}/${dragPointsTimes.first()} last \n" +
        "${dragPoints.last()}/${dragPointsTimes.last()}")
        x.desired = lastDrags.x
        y.desired = lastDrags.y
        mouseDrag.x = .0
        mouseDrag.y = .0
    }

    init {

        scene.registerDragHandler(this)
        onUpdate += {
            panAnimator.first.animate(it.deltaT)
            panAnimator.second.animate(it.deltaT)

            updateDrags(it.renderPass)
            updateTransform()

            //debug = "${x.actual} -> ${x.desired}, ${y.actual} -> ${y.desired}"
        }

        inputMan.registerKeyListener(InputManager.KEY_CURSOR_LEFT, "left", { it.isPressed }) {
            panAnimator.first.desired = translation.x - panStep * (if (it.isShiftDown) 10.0 else 1.0) * zoom
        }
        inputMan.registerKeyListener(InputManager.KEY_CURSOR_RIGHT, "right", { it.isPressed }) {
            panAnimator.first.desired = translation.x + panStep * (if (it.isShiftDown) 10.0 else 1.0) * zoom
        }
        inputMan.registerKeyListener(InputManager.KEY_CURSOR_UP, "up", { it.isPressed }) {
            // y = -y, so - - gives +
            panAnimator.second.desired = translation.y + panStep * (if (it.isShiftDown) 10.0 else 1.0) * zoom
        }
        inputMan.registerKeyListener(InputManager.KEY_CURSOR_DOWN, "down", { it.isPressed }) {
            panAnimator.second.desired = translation.y - panStep * (if (it.isShiftDown) 10.0 else 1.0) * zoom
        }

        inputMan.registerKeyListener(InputManager.KEY_NP_PLUS, "zoom in", { it.isPressed }) { zoom *= 1.1 }
        inputMan.registerKeyListener(InputManager.KEY_NP_MINUS, "zoom out", { it.isPressed }) { zoom /= 1.1 }


        val initialZoom = zoom
        val initialTranslation = Vec2d(translation)
        reset = {
            zoom = initialZoom
            val (x, y) = panAnimator
            x.set(initialTranslation.x)
            y.set(initialTranslation.y)
        }
    }

    private fun updateDrags(rp: RenderPass) {
        if ( rp.viewport.width == 0 || rp.viewport.height == 0 ) return

        //from manual user's drag
        if ( mouseDrag.length() > 0 ) {
            val xydrag = unprojectToCanvas(rp.viewport, mouseDrag)
            val (x,y) = panAnimator

            x.set(translation.x + xydrag.x)
            y.set(translation.y + xydrag.y)

            mouseDrag.x = .0
            mouseDrag.y = .0
        }
    }

    private fun updateTransform() {
        val z = zoom
        mouseTransform.setIdentity()

        val (x,y) = panAnimator
        translation.x = x.actual
        translation.y = y.actual
        mouseTransform.translate(translation.x, translation.y, 0.0)
        mouseTransform.scale(z, z, 1.0)
        set(mouseTransform)
    }
}