import de.fabmax.kool.InputManager
import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.*
import de.fabmax.kool.pipeline.RenderPass
import de.fabmax.kool.scene.Group
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.scene.animation.SpringDamperDouble
import de.fabmax.kool.toString
import de.fabmax.kool.util.Viewport

fun Scene.canvasTransform(inputMan: InputManager, name: String? = null, block: CanvasTransform.() -> Unit): CanvasTransform {
    val sit = CanvasTransform(inputMan, this, name)
    sit.block()
    return sit
}

class CanvasTransform(inputMan: InputManager, scene: Scene, name: String? = null) : Group(name), Scene.DragHandler {

    val reset: () -> Unit

    var minZoom = 1e-30
    var maxZoom = 100.0

    var panStep = 1.0
    var scaleStep = 50.0 // in percent?
    val translation = MutableVec2d()

    var scale
        get() = scaleAnimator.actual
        set(value) {
            scaleAnimator.desired = value.clamp(minZoom, maxZoom)
        }

    private val scaleAnimator = SpringDamperDouble(1.0)

    var debug: String? = null
    var debugPivot: Vec2d? = null

    private val mouseTransform = Mat4d()
    private val mouseDrag = MutableVec2d()
    private val panAnimator = Pair(SpringDamperDouble(0.0).apply {
        stiffness = 50.0
    }, SpringDamperDouble(0.0).apply {
        stiffness = 50.0
    })


    private val dragPoints = CircularFifoQueue<Vec2d>(5)
    private val dragPointsTimes = CircularFifoQueue<Double>(5)
    private var lastTimeDrag = .0
    private val viewportCache = Viewport()

    private val displayMove = Vec2d(-0.5,-0.5)
    private val printPrecision = 3
    /// tbd matrix stuff
    fun unscaleToCanvas(viewport: Viewport, screenPos: Vec2d): Vec2d {
        return Vec2d(
            viewport.aspectRatio * scale * (screenPos.x / viewport.width),
            scale * (screenPos.y / viewport.height)
        )
    }
    fun unprojectToCanvas(viewport: Viewport, screenPos: Vec2d, scale: Double = this.scale, origin: Vec2d = translation): Vec2d {
        //return MutableVec2d(unscaleToCanvas(viewport, screenPos)).add(displayMove).add(translation)
        return MutableVec2d(
            (screenPos.x / viewport.width),
            (screenPos.y / viewport.height)
        ).add(displayMove).mul(Vec2d(viewport.aspectRatio*scale, -scale)).add(origin)
    }

    fun KoolContext.anyBtn(pointers: List<InputManager.Pointer> = this.inputMgr.pointerState.pointers.toList() ) =
        pointers.any { it.buttonMask > 0 }

    private fun calcDragPoints(screenPos: Vec2d, delta: Vec2d): Pair<Vec2d, Vec2d> {
        //val pos = unprojectToCanvas(viewportCache, screenPos)
        val oldpos = MutableVec2d(screenPos)
        oldpos.subtract(delta, oldpos)
        //oldpos.set(unprojectToCanvas(viewportCache, oldpos))
        return Pair(oldpos, screenPos)
    }

    override fun handleDrag(dragPtrs: List<InputManager.Pointer>, scene: Scene, ctx: KoolContext) {

        if (dragPtrs.none { !it.isConsumed() }) return
        ctx.getWindowViewport(viewportCache)

        if (dragPtrs.first().deltaScroll != .0) {
            with(dragPtrs.first()) {
                handleScaling(Vec2d(x, y), deltaScroll)
                consume()
            }
            return
        }
        if ( dragPtrs.size > 1 || (debugPivot != null && dragPtrs.isNotEmpty())) {
            val points = dragPtrs.map { pointer ->
                val screenPos = Vec2d(pointer.x, pointer.y)
                val delta = Vec2d(pointer.deltaX, pointer.deltaY)
                calcDragPoints(screenPos, delta)
            }.toMutableList()

            debugPivot?.run {
                points.add(calcDragPoints(this, Vec2d.ZERO))
            }

            // was
            val v0 = MutableVec2d(points[0].first).also { it.minusAssign(points[1].first) }.length()
            // comes
            val v1 = MutableVec2d(points[0].second).also { it.minusAssign(points[1].second) }.length()

            val newScale = scale * v0 / v1
            val delta = scaleToDelta(newScale)
            with(dragPtrs.first()) {
                handleScaling(Vec2d(x, y), delta)
                consume()
            }
            return
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


    init {

        scene.registerDragHandler(this)
        onUpdate += { ev ->
            viewportCache.set(ev.viewport.x, ev.viewport.ySigned, ev.viewport.width, ev.viewport.heightSigned)

            listOf(panAnimator.first, panAnimator.second, scaleAnimator).forEach {
                it.animate(ev.deltaT)
            }

            updateDrags(ev.renderPass)
            updateTransform()

            //debug = "${x.actual} -> ${x.desired}, ${y.actual} -> ${y.desired}"
        }
        scene.onProcessInput += {
            debug = it.inputMgr.pointerState.pointers.filter { it.isValid && !it.isConsumed() }.joinToString("\n") {
                val xy = unprojectToCanvas(viewportCache, Vec2d(it.x, it.y))
                "pointer #${it.id} @${xy.toString(3)}"
            }
        }

        inputMan.registerKeyListener(InputManager.KEY_CURSOR_LEFT, "left", { it.isPressed }) {
            animateTo(translation.x - it.getPanStep(), panAnimator.second.desired)
        }
        inputMan.registerKeyListener(InputManager.KEY_CURSOR_RIGHT, "right", { it.isPressed }) {
            animateTo(translation.x + it.getPanStep(), panAnimator.second.desired)
        }
        inputMan.registerKeyListener(InputManager.KEY_CURSOR_UP, "up", { it.isPressed }) {
            // y = -y, so - - gives +
            animateTo(panAnimator.first.desired, translation.y + it.getPanStep())
        }
        inputMan.registerKeyListener(InputManager.KEY_CURSOR_DOWN, "down", { it.isPressed }) {
            animateTo(panAnimator.first.desired, translation.y - it.getPanStep())
        }

        inputMan.registerKeyListener(InputManager.KEY_NP_PLUS, "zoom in", { it.isPressed }) {
            with(inputMan.pointerState.primaryPointer) {
                handleScaling ( Vec2d(x, y), -1.0 )
            }
        }
        inputMan.registerKeyListener(InputManager.KEY_NP_MINUS, "zoom out", { it.isPressed }) {
            with(inputMan.pointerState.primaryPointer) {
                handleScaling ( Vec2d(x, y), +1.0 )
            }
        }
        // set pivot for debug zoom by two pointers
        inputMan.registerKeyListener(InputManager.KEY_INSERT, "set pivot 1", { it.isPressed }) {
            with(inputMan.pointerState.primaryPointer) {
                debugPivot = Vec2d(x, y)//unprojectToCanvas(viewportCache, Vec2d(x, y))
                println("debug pivot = ${debugPivot?.toString(3)}")
            }
        }
        inputMan.registerKeyListener(InputManager.KEY_DEL, "pivot reset", { it.isPressed }) {
            debugPivot = null
        }

        val initialZoom = scale
        val initialTranslation = Vec2d(translation)
        reset = {
            println("reseting to zoom=${initialZoom.toString(printPrecision)} tra = ${initialTranslation.toString(printPrecision)}")
//            zoom = initialZoom
            scale = initialZoom
            animateTo(initialTranslation.x, initialTranslation.y)
        }
    }

    private fun InputManager.KeyEvent.getPanStep() =
        panStep * (if (isShiftDown) 10.0 else 1.0) * scaleAnimator.actual

    private fun handleDragStart(dragPtrs: List<InputManager.Pointer>, scene: Scene, ctx: KoolContext) {


        println("drag start")

    }
    private fun handleDragEnd(dragPtrs: List<InputManager.Pointer>, scene: Scene, ctx: KoolContext) {

        println("drag end")
        // expect dragPoints.isNotEmpty()
        val lastDrags = MutableVec2d()
        val animTime = if ( ctx.inputMgr.isShiftDown || ctx.inputMgr.pointerState.pointers.first().isRightButtonDown ) 5.0 else 1.0 // sec

        dragPoints.last().subtract(dragPoints.first(), lastDrags)
        lastDrags /= dragPointsTimes.last() - dragPointsTimes.first()
        lastDrags.scale(animTime)
        lastDrags += translation
        //debug = lastDrag.toString()

//        val (x,y) = panAnimator
        //println("current: $translation desired: $lastDrags mouseDrag=${mouseDrag} with first: \n" +
        //        "${dragPoints.first()}/${dragPointsTimes.first()} last \n" +
        //        "${dragPoints.last()}/${dragPointsTimes.last()}")
        animateTo(lastDrags.x, lastDrags.y)
        mouseDrag.x = .0
        mouseDrag.y = .0
    }


    private fun deltaToScale(deltaScroll: Double) =
        scale + scale * deltaScroll * (scaleStep / 100.0)
    private fun scaleToDelta(targetScale: Double) =
        100.0 * ( targetScale / scale - 1) / scaleStep

    /// pivot in screen coords
    private fun handleScaling(pivot: Vec2d, deltaScroll: Double) {

        val newScale = deltaToScale(deltaScroll)
        val oldXy = unprojectToCanvas(viewportCache, pivot)
        val newXy = unprojectToCanvas(viewportCache, pivot, newScale)

        // translation.x - newScale * (newXy.x - oldXy.x) / zoom
        val newTr = MutableVec2d(newXy).apply {
            minusAssign(oldXy)
            scale(-newScale/scale)
            plusAssign(translation)
        }
//        debug = "deltaScroll=$deltaScroll pivot=$pivot ${(newScale/zoom).toString(printPrecision)} oldxy = ${oldXy.toString(printPrecision)} newxy = ${newXy.toString(printPrecision)} " +
//                "trsl = ${translation.toString(printPrecision)} newtr = ${newTr.toString(printPrecision)}"
        animateTo(newTr.x, newTr.y)
        scale = newScale
    }

    private fun updateDrags(rp: RenderPass) {
        if ( rp.viewport.width == 0 || rp.viewport.height == 0 ) return

        //from manual user's drag
        // tbd move to handleDrag as we know viewport there
        if ( mouseDrag.length() > 0 ) {
            val xydrag = unscaleToCanvas(rp.viewport, mouseDrag)
            val (x,y) = panAnimator

            x.set(translation.x + xydrag.x)
            y.set(translation.y + xydrag.y)

            mouseDrag.x = .0
            mouseDrag.y = .0
        }
    }

    private fun updateTransform() {
        val z = scale
        mouseTransform.setIdentity()

        val (x,y) = panAnimator
        translation.x = x.actual
        translation.y = y.actual
        mouseTransform.translate(translation.x, translation.y, 0.0)
        mouseTransform.scale(z, z, 1.0)
        set(mouseTransform)
    }

    private fun animateTo(x: Number, y: Number) = animateTo(x.toDouble(), y.toDouble())
    private fun animateTo(x: Double, y: Double) {
        panAnimator.first.desired = x
        panAnimator.second.desired = y
    }
}
