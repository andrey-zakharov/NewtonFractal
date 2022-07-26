import de.fabmax.kool.InputManager
import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.*
import de.fabmax.kool.pipeline.Attribute
import de.fabmax.kool.pipeline.Shader
import de.fabmax.kool.scene.*
import de.fabmax.kool.scene.ui.dps
import de.fabmax.kool.scene.ui.full
import de.fabmax.kool.scene.ui.uiScene
import de.fabmax.kool.scene.ui.zero



class App(val ctx: KoolContext): Scene.DragHandler {

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
                +camera

            }

            +inputTransform
            +fullScreenQuad(newtonFractalShader {


            }).apply {
                (this.shader as? NewtonFractalShader)?.run {

                }
                onUpdate += {
                    (this.shader as? NewtonFractalShader)?.uInvertedMvp?.run {
                        // translate from 0..1 uv coords to -0.5 .. 0.5 coords + ratio
                        this.setIdentity().setColVec(2,
                            Vec3f(-0.5f * it.viewport.aspectRatio, -0.5f, -0f)
                        //    Vec3f(inputTransform.translation.x.toFloat(), inputTransform.translation.y.toFloat(), 0f)
                        ) // translate
                        this.scale(Vec3f(it.viewport.aspectRatio, 1f, 1f))

                        val tmpMat = Mat3f()
                        tmpMat
                            .setIdentity()
                            .setColVec(2,
                                Vec3f(inputTransform.translation.x.toFloat(), inputTransform.translation.y.toFloat(), 0f)
                            ) // translate
                        tmpMat.scale(Vec3f(inputTransform.zoom.toFloat()))
                        println("before. this:")
                        this.dump()
                        println("before. tmpMat:")
                        tmpMat.dump()

                        this.set(tmpMat.mul(this))
                        //this.set(tmpMat)
                        this.invert()
                        println("after. this:")
                        this.dump()

                    }
                }
            }
        }

        ctx.scenes += uiScene {scene ->

            +label("cam") {
                layoutSpec.setOrigin(zero(), zero(), zero())
                layoutSpec.setSize(full(), dps(50f), full() )
                onUpdate += {
                    text = ctx.scenes[0].camera.globalPos.toString()
                }
            }
            +button("inputTransform.reset") {
                layoutSpec.setOrigin(zero(), dps(50f), zero())
                layoutSpec.setSize(full(), dps(50f), full() )
                onClick += { pointer: InputManager.Pointer, rayTest: RayTest, koolContext: KoolContext ->

                    //// HACK
                    (koolContext.scenes.first().children.first { it.name == "canvasTransform" } as? CanvasTransform)?.run {
                        reset()
                    }
                }
            }
        }
        ctx.run()
    }

    override fun handleDrag(dragPtrs: List<InputManager.Pointer>, scene: Scene, ctx: KoolContext) {
        println(dragPtrs)
    }

//    private val tmpMat = Mat3f()
}

private fun Group.fullScreenQuad(quadShader: Shader): Mesh {

    return mesh(listOf(Attribute.POSITIONS, Attribute.TEXTURE_COORDS, Attribute.COLORS, Attribute.NORMALS), "canvas") {

        isFrustumChecked = false
        generate {
            rect {
                size.set(2f, 2f)
                translate(-1f, -1f, 0f)
                mirrorTexCoordsY()
            }
        }
        shader = quadShader
    }
}