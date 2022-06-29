import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.modules.ksl.KslBlinnPhongShader
import de.fabmax.kool.modules.ksl.KslShader
import de.fabmax.kool.modules.ksl.blocks.TexCoordAttributeBlock
import de.fabmax.kool.modules.ksl.blocks.texCoordAttributeBlock
import de.fabmax.kool.modules.ksl.lang.KslProgram
import de.fabmax.kool.modules.ksl.lang.x
import de.fabmax.kool.modules.ksl.lang.y
import de.fabmax.kool.pipeline.Attribute
import de.fabmax.kool.pipeline.GlslType
import de.fabmax.kool.pipeline.Shader
import de.fabmax.kool.pipeline.shadermodel.ShaderModel
import de.fabmax.kool.pipeline.shadermodel.fragmentStage
import de.fabmax.kool.pipeline.shadermodel.vertexStage
import de.fabmax.kool.pipeline.shading.ModeledShader
import de.fabmax.kool.scene.*
import de.fabmax.kool.util.Viewport

fun newtonFractalShader(cfgBlock: NFShader.Config.() -> Unit): NFShader {
    val cfg = NFShader.Config().apply(cfgBlock)
    return NFShader(cfg)
}

class NFShader(cfg: Config, model: KslProgram = NewtonFractal(cfg)) : KslShader(model, PipelineConfig()) {
    //var maxItem: Uniform1i = model.uniformInt1("maxIter")
    class Config {
        val maxIter = 5
    }

    class NewtonFractal(cfg: Config) : KslProgram( "Newton Fractal Drawer" ) {
        //var maxIter by uniform("maxIter")
        init {
            val texCoordBlock: TexCoordAttributeBlock
            vertexStage {
                main {
                    texCoordBlock = texCoordAttributeBlock()
                    outPosition set constFloat4(vertexAttribFloat3(Attribute.POSITIONS.name), 1f)
                }
            }
            fragmentStage {
                main {

                    val uv = texCoordBlock.getAttributeCoords(Attribute.TEXTURE_COORDS)
                    colorOutput(constFloat4(uv.x, uv.y, 0f.const, 1f.const))
                }
            }
        }

    }
}


class OldNewtonShader : ModeledShader(shaderModel()) {


    companion object {
        private fun shaderModel() = ShaderModel("drawer").apply {
            dumpCode = true

            vertexStage {
                positionOutput = simpleVertexPositionNode().outVec4
            }
            // colorOut =
            fragmentStage {
                val maxIter = namedVariable("maxIter", constInt(5))
                val coords = screenCoordNode()
                val res = combineNode(GlslType.VEC_4F)

                res.inX = coords.outScreenCoord
                colorOutput(            splitNode(coords.outScreenCoord, "xy").output)

                //colorOutput(coords.outScreenCoord)
            }
        }
    }

}
//fun makeShader(cfgBlock: Config.() -> Unit): Shader {
//    val cfg = Config()
//    cfg.cfgBlock()
//
//    return ModeledShader(model)
//}

class App(val ctx: KoolContext) {

    init {
        ctx.scenes += scene {

            fullScreenQuad(newtonFractalShader {})

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
            left = vp.x.toDouble(), right = (vp.width + vp.x).toDouble(),
            top = vp.y.toDouble(), bottom = (vp.height + vp.y).toDouble(),
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