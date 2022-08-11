import de.fabmax.kool.math.Vec2d
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.pipeline.Attribute
import de.fabmax.kool.pipeline.Shader
import de.fabmax.kool.scene.Group
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.mesh
import de.fabmax.kool.toString

expect fun epochMillis(): Long

internal fun Group.fullScreenQuad(quadShader: Shader): Mesh {

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

fun Vec2d.toString(precision: Int): String {
    return "(${this.x.toString(precision)} ${this.y.toString(precision)})"
}

fun Vec3f.toString(precision: Int): String {
    return "(${x.toString(precision)} ${y.toString(precision)} ${z.toString(precision)})"
}

