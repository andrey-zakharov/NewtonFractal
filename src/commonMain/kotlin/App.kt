import de.fabmax.kool.InputManager
import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.Mat3f
import de.fabmax.kool.math.RayTest
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.scene.Camera
import de.fabmax.kool.scene.OrthographicCamera
import de.fabmax.kool.scene.scene
import de.fabmax.kool.scene.ui.*
import de.fabmax.kool.toString
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.pow

fun Mat3f.translate(o: Vec2f) = translate(o.x, o.y)
fun Mat3f.translate(x: Float, y: Float): Mat3f {
    this[0, 2] = x
    this[1, 2] = y
    //this[2, 2] = 1f //
    return this
}

class App(val ctx: KoolContext) {

    init {
        ctx.scenes += scene {

            camera = OrthographicCamera("plain").apply {
                projCorrectionMode = Camera.ProjCorrectionMode.ONSCREEN
                isClipToViewport = false
                isKeepAspectRatio = true
                setCentered(2f, 0.1f, 10f)
            }

            val inputTransform = canvasTransform(ctx.inputMgr, "canvasTransform") {
                //leftDragMethod = OrbitInputTransform.DragMethod.PAN
                panStep = 0.1
                // float precise
                minZoom = 10.0.pow(-1.5)
                maxZoom = 10.0.pow(7.5)
                +camera
            }

            +inputTransform
            +fullScreenQuad(newtonFractalShader {


            }).apply {
                onUpdate += {ev ->
                    val z = inputTransform.zoom.toFloat()
                    val displayMove = Mat3f().apply { translate(-0.5f,-0.5f) }

                    val matUv2xy = Mat3f().apply {
                        translate(inputTransform.translation.x.toFloat(), inputTransform.translation.y.toFloat())
                        scale(ev.viewport.aspectRatio * z, z, 1f)
                        mul(displayMove)
                    }

                    (this.shader as? NewtonFractalShader)?.run {
                        uv2xy.run {
                            //uv2xy.mul(displayMove, this)
                            set(matUv2xy)
                        }
                        scale = Vec2f(z, z)
                        viewport = Vec2f(ev.viewport.width.toFloat(), ev.viewport.height.toFloat())
                        gridScale = z / 10f.pow(ceil(log10(z)))

                    }
                }
            }
        }

        ctx.scenes += uiScene {scene ->
            +drawerMenu("hello") {
                +button("inputTransform.reset") {
                    layoutSpec.setOrigin(pcs(35f), dps(-50f), zero())
                    layoutSpec.setSize(full(), dps(50f), full() )
                    onClick += { pointer: InputManager.Pointer, rayTest: RayTest, koolContext: KoolContext ->
                        //// HACK
                        (koolContext.scenes.first().children.first { it.name == "canvasTransform" } as? CanvasTransform)?.run {
                            reset()
                            pointer.consume()
                        }
                    }
                }
            }

            +label("cam") {
                layoutSpec.setOrigin(zero(), zero(), zero())
                layoutSpec.setSize(full(), dps(100f), full() )
                onUpdate += { ev ->
                    with(ctx.scenes[0]) {
                        text = "pos: ${camera.globalPos}"
                        (children.first { it.name == "canvasTransform" } as? CanvasTransform)?.run {
                            text += "\nscale: $zoom (10^${log10(zoom).toString(4)})"
                        }
                    }
                }
            }
            +label("drags") {
                layoutSpec.setOrigin(zero(), dps(100f), zero())
                layoutSpec.setSize(full(), dps(100f), full() )
                onUpdate += { ev ->
                    with(ctx.scenes[0]) {
                        (children.first { it.name == "canvasTransform" } as? CanvasTransform)?.run {
                            debug?.run { if ( this.isNotEmpty() ) text = this }
                            debug = ""
                        }
                    }
                }
            }

        }
        ctx.run()

        tests()
    }

    fun tests() {
        val a = CircularFifoQueue<Int>(3)
        // expect exception NoSuchElementException
        //expectException { a.first() }
        //expectException { a.last() }
        a.addLast(1)
        expect( a.first() == 1 )
        expect( a.last() == 1 )
        a.addLast(2)
        expect( a.first() == 1 )
        expect( a.last() == 2 )
        a.addLast(3)
        a.addLast(4)
        expect( a.first() == 2 ) { "was ${a.first()} expect 2"}
        expect( a.last() == 4 )
        a.addLast(5)
        expect( a.first() == 3 ) { "was ${a.first()} expect 2"}
        expect( a.last() == 5 )
    }

    private fun expect(cond: Boolean, msg: () -> String = { "assert error" }) {
        if ( !cond ) throw AssertionError(msg())
    }

//    private val tmpMat = Mat3f()
}
