import de.fabmax.kool.KoolContext
import de.fabmax.kool.pipeline.Attribute
import de.fabmax.kool.pipeline.Shader
import de.fabmax.kool.scene.*
import de.fabmax.kool.util.Viewport

class App(val ctx: KoolContext) {

    init {
        ctx.scenes += scene {

            fullScreenQuad(newtonFractalShader {


            })

            //clearColor = null

            camera = OrthographicCamera("plain").apply {
                projCorrectionMode = Camera.ProjCorrectionMode.ONSCREEN
                isKeepAspectRatio = true
                left = 0f
                right = 1f
                top = 1f
                bottom = 0f
            }

            onUpdate += {

            }
        }

        resize()

        ctx.run()


    }

    fun resize() {
        val vp = Viewport()
        ctx.getWindowViewport(vp)
        ctx.scenes[0].camera.proj.setOrthographic(
            left = 0.0, right = vp.width.toDouble(),
            top = 0.0, bottom = vp.height.toDouble(),
            far = 1000.0, near = -1000.0
        )
        println(vp)
    }
}

private fun Group.fullScreenQuad(quadShader: Shader) {
    isFrustumChecked = false
    +mesh(listOf(Attribute.POSITIONS, Attribute.TEXTURE_COORDS, Attribute.COLORS, Attribute.NORMALS), "canvas") {

        isFrustumChecked = false
        generate {
            rect {
                size.set(1f, 1f)
                mirrorTexCoordsY()
            }
        }
        shader = quadShader
    }
}