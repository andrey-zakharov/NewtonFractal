import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.Vec4f
import de.fabmax.kool.modules.ksl.KslShader
import de.fabmax.kool.modules.ksl.blocks.TexCoordAttributeBlock
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
    val roots by uniform4fv(NewtonFractal.UNIFORM_ROOTS, arraySize = 3)
    val colors by uniform3fv(NewtonFractal.UNIFORM_COLORS, arraySize = roots.size)

    val uv2xy by uniformMat3f(NewtonFractal.UNIFORM_MVP)
    var scale by uniform2f(NewtonFractal.UNIFORM_SCALE)
    var viewport by uniform2f(NewtonFractal.UNIFORM_VIEWPORT)
    var gridScale by uniform1f(NewtonFractal.UNIFORM_GRIDSCALE)


    override fun onPipelineSetup(builder: Pipeline.Builder, mesh: Mesh, ctx: KoolContext) {
        super.onPipelineSetup(builder, mesh, ctx)
        for (i in 0 until cfg.roots.size) {
            roots[i].set(Vec4f(cfg.roots[i].x, cfg.roots[i].y, 0f, 0f))
            colors[i].set(cfg.colors[i].r, cfg.colors[i].g, cfg.colors[i].b)
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
//        val colors = listOf(Color.RED, Color.GREEN, Color.BLUE)
        val colors = listOf(Color.fromHex("AF3F42"), Color.fromHex("276C69"), Color.fromHex("83A63B"))
        val maxIter = 100
        var tolerance = 0.00001f
        val a = Vec2f(1f, 0f)
    }

    class NewtonFractal(cfg: Config) : KslProgram( "Newton Fractal Drawer" ) {

        companion object {
            const val UNIFORM_VIEWPORT = "viewport"
            const val UNIFORM_SCALE = "scale"
            const val UNIFORM_GRIDSCALE = "gridScale"
            const val UNIFORM_MVP = "uMvp"
            const val UNIFORM_COLORS = "uColors"
            const val UNIFORM_ROOTS = "roots"

        }
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

                val roots = uniformFloat4Array(UNIFORM_ROOTS, 3)
                val colors = uniformFloat3Array(UNIFORM_COLORS, 3)
                val uv2xy = uniformMat3(UNIFORM_MVP)
                val viewport = uniformFloat2(UNIFORM_VIEWPORT)
                val scale = uniformFloat2(UNIFORM_SCALE)
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
                    val xyz = float3Var(uv2xy * float3Value(uv.x, uv.y, 1f.const))
                    val xy = xyz.float2("xy")
                    var z = float2Var(xy)
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
                        fori(0.const, cfg.roots.size.const) {
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
                    val color = float4Var(
                        float4Value(0f.const, 0f.const, 0f.const, 1f.const)
                    )
                    fori(0.const, roots.arraySize.const) {
                        val l = length(z - roots[it].float2("xy"))
                        val mixValue = m * pow(a, l)
                        val mixColor = colors[it]
                        color.x set mix(color.x, mixColor.x, mixValue)
                        color.y set mix(color.y, mixColor.y, mixValue)
                        color.z set mix(color.z, mixColor.z, mixValue)
                    }

                    //grid https://github.com/ogxd/grid-shader-unity/blob/master/Assets/Plugins/GridShader/Grid%20Shader.shader
                    val gridThickness = float2Var(2f.const / viewport)
                    gridThickness.x set gridThickness.x * (viewport.x / viewport.y)
                    val gridScale = uniformFloat1(UNIFORM_GRIDSCALE)
                    val fadeSpeed = 2f.const // Range(0.1, 4)) = 0.5
                    val localScale = floatVar(1f.const / gridScale)

                    val uvAnalog = float2Var(xy / scale)

                    val gridPos = int2Var(
                        floor(fract((uvAnalog - 0.5f.const * gridThickness) * localScale) + gridThickness * localScale).toInt2(),
                    )
                    val gridColor = Color.GRAY

                    val fade = floatVar(pow(1f.const - map(localScale, 0.1f.const, 1f.const, 0.00001f.const, 0.99999f.const), fadeSpeed))

                    `if`( gridPos.x eq 1.const or (gridPos.y eq 1.const)
                    ) {
                        val mixValue = max((1f.const - fade), fade)
                        color.x set mix(color.x, gridColor.x.const, mixValue)
                        color.y set mix(color.y, gridColor.y.const, mixValue)
                        color.z set mix(color.z, gridColor.z.const, mixValue)

                    }.`else` {
                        val tensScale = 10f.const * localScale
                        gridPos set floor(fract((uvAnalog - 0.5f.const * gridThickness) * tensScale) + gridThickness * tensScale).toInt2()

                        `if`( gridPos.x eq 1.const or (gridPos.y eq 1.const) ) {
                            // NO MIX FOR vec3?
                            val mixValue = fade // (1f.const - fade)
                            color.x set mix(color.x, gridColor.x.const, mixValue)
                            color.y set mix(color.y, gridColor.y.const, mixValue)
                            color.z set mix(color.z, gridColor.z.const, mixValue)
                        }
                    }

                    colorOutput(color)
                }
            }
        }

    }
}

//log10(x) = log(x) / log(10) = (1 / log(10)) * log(x)
fun KslScopeBuilder.log10(x: KslExprFloat1): KslExprFloat1 =
    KslInvokeFunctionScalar(
        parentStage.getOrCreateFunction(Log10.FUNC_NAME) { Log10(this) },
        this, KslTypeFloat1, x
    )

class Log10(parentScope: KslScopeBuilder) :
    KslFunction<KslTypeFloat1>(FUNC_NAME, KslTypeFloat1, parentScope.parentStage) {
    companion object {
        const val FUNC_NAME = "log10"
    }
    val d = 1.0 / kotlin.math.ln(10.0)
    init {
        val x = paramFloat1("x")
        body.apply {
            `return` (
                d.const * log(x)
            )
        }
    }
}

fun KslScopeBuilder.map(value: KslExprFloat1, min1: KslExprFloat1, max1: KslExprFloat1, min2: KslExprFloat1, max2: KslExprFloat1): KslExprFloat1 =
    KslInvokeFunctionScalar(
        parentStage.getOrCreateFunction(SlMap.FUNC_NAME) { SlMap(this) },
        this, KslTypeFloat1, value, min1, max1, min2, max2
    )

class SlMap(parentScope: KslScopeBuilder):
    KslFunction<KslTypeFloat1>(FUNC_NAME, KslTypeFloat1, parentScope.parentStage){
    companion object {
        const val FUNC_NAME = "map"
    }

    init {
        val v = paramFloat1("value")
        val min1 = paramFloat1("min1")
        val max1 = paramFloat1("max1")
        val min2 = paramFloat1("min2")
        val max2 = paramFloat1("max2")
        body.apply {
            `return`(min2 + (v - min1) * ( max2 - min2) / (max1 - min1))
        }
    }
}