import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.Vec4f
import de.fabmax.kool.modules.ksl.KslShader
import de.fabmax.kool.modules.ksl.blocks.TexCoordAttributeBlock
import de.fabmax.kool.modules.ksl.blocks.mvpMatrix
import de.fabmax.kool.modules.ksl.blocks.texCoordAttributeBlock
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.pipeline.Attribute
import de.fabmax.kool.pipeline.Pipeline
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.util.Color
import kotlin.math.sqrt

fun newtonFractalShader(cfgBlock: NewtonFractalShader.Config.() -> Unit): NewtonFractalShader {
    val cfg = NewtonFractalShader.Config().apply(cfgBlock)
    return NewtonFractalShader(cfg)
}

class NewtonFractalShader(val cfg: Config, model: KslProgram = NewtonFractal(cfg)) : KslShader(model, PipelineConfig()) {
    val roots by uniform4fv("roots", arraySize = 3)
    override fun onPipelineSetup(builder: Pipeline.Builder, mesh: Mesh, ctx: KoolContext) {
        super.onPipelineSetup(builder, mesh, ctx)
        for (i in 0 until cfg.roots.size) {
            roots[i].set(Vec4f(cfg.roots[i].x, cfg.roots[i].y, 0f, 0f))
        }
    }
    //var maxItem by uniform1i("maxIter")
    class Config {
        val roots = listOf(
            Vec2f(1f, 0f),
            Vec2f(-.5f, sqrt(3f)/2f),
            Vec2f(-.5f, -sqrt(3f)/2f),
        )
        val colors = listOf(Color.RED, Color.GREEN, Color.BLUE)
        val maxIter = 50
        var tolerance = 0.0001f
        val a = Vec2f(1f, 0f)
    }

    class NewtonFractal(cfg: Config) : KslProgram( "Newton Fractal Drawer" ) {

        init {
            dumpCode = true
            val texCoordBlock: TexCoordAttributeBlock
            vertexStage {
                val uMvp = mvpMatrix()
                main {

                    val mvp = mat4Var(uMvp.matrix)
                    texCoordBlock = texCoordAttributeBlock()
                    val localPos = float4Value(vertexAttribFloat3(Attribute.POSITIONS.name), 1f)

                    outPosition set mvp * localPos
                }
            }
            fragmentStage {
                val roots = uniformFloat4Array("roots", 3)
                val f = getOrCreateFunction("f") {
                    object : KslFunction<KslTypeFloat2>("f", KslTypeFloat2, this) {
                        init {
                            val z = paramFloat2("z")
                            body.apply {

                                `return`(complexPow(z, 3f.const) - float2Value(1f, 0f))

                            }
                        }
                    }
                }
                val df = getOrCreateFunction("df") {
                    object: KslFunction<KslTypeFloat2>("df", KslTypeFloat2, this) {
                        init {
                            val z = paramFloat2("z")
                            body.apply {

                                `return`(complexPow(z, 2f.const) * 3f.const)

                            }
                        }
                    }
                }
                main {

                    val uv = texCoordBlock.getAttributeCoords(Attribute.TEXTURE_COORDS)
                    var z = float2Var(uv * 5f.const, "z")
                    val res = floatArray(cfg.roots.size, 0f.const)
                    val steps = intVar(0.const)
                    val rootReached = boolVar(false.const)

                    `for`(steps, steps lt cfg.maxIter.const and rootReached.not(), 1.const) {
                        z -= complexMultiply(cfg.a.const,
                            complexDivide(
                                KslInvokeFunctionVector(f, this, KslTypeFloat2, z),
                                KslInvokeFunctionVector(df, this, KslTypeFloat2, z)
                            )
                        )

                        // check
                        `fori`(0.const, cfg.roots.size.const) {
                            val diff = z - roots[it].float2("xy")
                            `if`( length(diff) lt cfg.tolerance.const ) {
                                rootReached set true.const
                                `break`()
                            }
                        }

                        //
                    }

                    val a = 0.5f.const
                    val m = steps.toFloat1() / cfg.maxIter.const.toFloat1()
                    val r = length(z - roots[0].float2("xy"))
                    val g = length(z - roots[1].float2("xy"))
                    val b = length(z - roots[2].float2("xy"))

                    //colorOutput(float4Value(res.x, res.y, 0f.const, 1f.const))
                    colorOutput( float4Value(
                        m * pow(a, r),
                        m * pow(a, g),
                        m * pow(a, b), 1f.const) )
                }
            }
        }

    }
}