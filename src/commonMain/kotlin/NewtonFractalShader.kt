import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.Mat4d
import de.fabmax.kool.math.Mat4f
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
    var a by uniform2f("a", Vec2f(1f, 0f) ) // a complex number coefficient in general Newton's iteration
    var d by uniform1f("uD", 3f)
    val roots by uniform4fv("roots", arraySize = 3)
    val uInvertedMvp by uniformMat3f("uInvertedMvp")

    override fun onPipelineSetup(builder: Pipeline.Builder, mesh: Mesh, ctx: KoolContext) {
        super.onPipelineSetup(builder, mesh, ctx)
        for (i in 0 until cfg.roots.size) {
            roots[i].set(Vec4f(cfg.roots[i].x, cfg.roots[i].y, 0f, 0f))
        }

        // newton iterations vars
        a = Vec2f(1f, 0f)
        d = 3f
    }

    //var maxItem by uniform1i("maxIter")
    class Config {
        val roots = listOf(
            Vec2f(1f, 0f),
            Vec2f(-.5f, sqrt(3f)/2f),
            Vec2f(-.5f, -sqrt(3f)/2f),
        )
        val colors = listOf(Color.RED, Color.GREEN, Color.BLUE)
        val maxIter = 100
        var tolerance = 0.00001f
        val a = Vec2f(1f, 0f)
    }

    class NewtonFractal(cfg: Config) : KslProgram( "Newton Fractal Drawer" ) {

        init {
            dumpCode = true
            val texCoordBlock: TexCoordAttributeBlock
            vertexStage {
//                val uMvp = mvpMatrix()
                main {

                    //val mvp = mat4Var(uMvp.matrix)
                    texCoordBlock = texCoordAttributeBlock()
                    val localPos = float4Value(vertexAttribFloat3(Attribute.POSITIONS.name), 1f)

                    outPosition set localPos
//                    outPosition set mvp * localPos
                }
            }
            fragmentStage {

                val roots = uniformFloat4Array("roots", 3)
                val uInvertedMvp = uniformMat3("uInvertedMvp")
                val d = uniformFloat1("uD")

                val f = getOrCreateFunction("f") {
                    object : KslFunction<KslTypeFloat2>("f", KslTypeFloat2, this) {
                        init {
                            val d = paramFloat1("d")
                            val z = paramFloat2("z")
                            body.apply {

                                `return`(complexPow(z, d) - float2Value(1f, 0f))

                            }
                        }
                    }
                }
                val df = getOrCreateFunction("df") {
                    object: KslFunction<KslTypeFloat2>("df", KslTypeFloat2, this) {
                        init {
                            val d = paramFloat1("d")
                            val z = paramFloat2("z")
                            body.apply {

                                `return`(complexPow(z, d - 1f.const) * d)

                            }
                        }
                    }
                }
                main {

                    val uv = texCoordBlock.getAttributeCoords(Attribute.TEXTURE_COORDS)
//                    val mvp = mat4Var(mvpMatrix().matrix)
                    //val mvp = mat4Var(uInvertedMvp)
                    val xy = float3Value(uv.x, uv.y, 1f.const)
                    var z = float2Var((uInvertedMvp * xy).float2("xy"))
                    val res = floatArray(cfg.roots.size, 0f.const)
                    val steps = intVar(0.const)
                    val rootReached = boolVar(false.const)

                    `for`(steps, steps lt cfg.maxIter.const and rootReached.not(), 1.const) {
                        z -= complexMultiply(cfg.a.const,
                            complexDivide(
                                KslInvokeFunctionVector(f, this, KslTypeFloat2, d, z),
                                KslInvokeFunctionVector(df, this, KslTypeFloat2, d, z)
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
                    val m = 3f.const * steps.toFloat1() / cfg.maxIter.const.toFloat1()
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