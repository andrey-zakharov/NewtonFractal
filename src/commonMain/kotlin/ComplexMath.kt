import de.fabmax.kool.modules.ksl.lang.*

// based on https://github.com/rust-num/num-complex/blob/master/src/lib.rs
// Copyright 2013 The Rust Project Developers. MIT license
// Ported to GLSL by Andrei Kashcha (github.com/anvaka), available under MIT license as well.
// Ported to Ksl by Andrey Zakharov (andrey-zakharov@github.com), MIT license.


// issues: kool lib sealed classes, could not define new KslType
// object KslComplexNumber : KslFloatType("float2"), KslVector2<KslTypeFloat1>

fun KslScopeBuilder.complexFromPolar(
    r: KslExprFloat1,
    theta: KslExprFloat1,
): KslExprFloat2 {
    val func = parentStage.getOrCreateFunction(ComplexFromPolar.FUNC_NAME) { ComplexFromPolar(this) }
    return KslInvokeFunctionVector(func, this, KslTypeFloat2, r, theta)
}

// Returns a complex number z = 1 + i * 0.
class ComplexOne(parentScope: KslScopeBuilder) :
    KslFunction<KslTypeFloat2>("c_one", KslTypeFloat2, parentScope.parentStage) {
    init {
        body.apply {
            `return`(float2Value(1f, 0f))
        }
    }
}

// Returns a complex number z = 0 + i * 1.
class ComplexI(parentScope: KslScopeBuilder) :
    KslFunction<KslTypeFloat2>("c_i", KslTypeFloat2, parentScope.parentStage) {
    init {
        body.apply {
            `return`(float2Value(0f, 1f))
        }
    }
}

// Returns conjugate of a complex number.
class ComplexConjugate(parentScope: KslScopeBuilder) :
    KslFunction<KslTypeFloat2>("c_conj", KslTypeFloat2, parentScope.parentStage) {
    init {
        val c = paramFloat2("c")
        body.apply {
            `return`(float2Value(c.x, -c.y))
        }
    }
}

class ComplexFromPolar(parentScope: KslScopeBuilder) :
    KslFunction<KslTypeFloat2>(FUNC_NAME, KslTypeFloat2, parentScope.parentStage) {
    init {
        val r = paramFloat1("r")
        val theta = paramFloat1("theta")
        body.apply {
            `return`(float2Value(r * cos(theta), r * sin(theta)))
        }
    }
    companion object {
        const val FUNC_NAME = "c_from_polar"
    }
}

fun KslScopeBuilder.complexToPolar(c: KslExprFloat2): KslExprFloat2 =
    KslInvokeFunctionVector(
        parentStage.getOrCreateFunction(ComplexToPolar.FUNC_NAME) { ComplexToPolar(this) },
        this, KslTypeFloat2, c)

class ComplexToPolar(parentScope: KslScopeBuilder) :
    KslFunction<KslTypeFloat2>(FUNC_NAME, KslTypeFloat2, parentScope.parentStage) {
    init {
        val c = paramFloat2("c")
        body.apply {
            `return`(float2Value(length(c), atan2(c.y, c.x)))
        }
    }
    companion object {
        const val FUNC_NAME = "c_to_polar"
    }
}

// Computes `e^(c)`, where `e` is the base of the natural logarithm.
fun KslScopeBuilder.complexExp(c: KslExprFloat2): KslExprFloat2 =
    KslInvokeFunctionVector(
        parentStage.getOrCreateFunction(ComplexExp.FUNC_NAME) { ComplexExp(this) },
        this, KslTypeFloat2, c)

class ComplexExp(parentScope: KslScopeBuilder) :
    KslFunction<KslTypeFloat2>(FUNC_NAME, KslTypeFloat2, parentScope.parentStage) {
    init {
        val c = paramFloat2("c")
        body.apply {
            `return`(complexFromPolar(exp(c.x), c.y))
        }
    }
    companion object {
        const val FUNC_NAME = "c_exp"
    }
}

// Raises a floating point number to the complex power `c`.
fun KslScopeBuilder.complexExp(base: KslExprFloat1, c: KslExprFloat2): KslExprFloat2 =
    KslInvokeFunctionVector(
        parentStage.getOrCreateFunction(ComplexExp.FUNC_NAME) { ComplexExp2(this) },
        this, KslTypeFloat2, c)

class ComplexExp2(parentScope: KslScopeBuilder) :
    KslFunction<KslTypeFloat2>(FUNC_NAME, KslTypeFloat2, parentScope.parentStage) {
    init {
        val base = paramFloat1("base")
        val c = paramFloat2("c")
        body.apply {
            `return`(complexFromPolar(pow(base, c.x), c.y * log(base)))
        }
    }
    companion object {
        const val FUNC_NAME = "c_exp"
    }
}

// Raises `c` to a floating point power `e`.
fun KslScopeBuilder.complexPow(c: KslExprFloat2, e: KslExprFloat1): KslExprFloat2 =
    KslInvokeFunctionVector(
        parentStage.getOrCreateFunction(ComplexPow.FUNC_NAME) { ComplexPow(this) },
        this, KslTypeFloat2, c, e)

class ComplexPow(parentScope: KslScopeBuilder) :
    KslFunction<KslTypeFloat2>(FUNC_NAME, KslTypeFloat2, parentScope.parentStage) {
    init {
        val c = paramFloat2("c")
        val e = paramFloat1("e")
        body.apply {
            val p = float2Var(complexToPolar(c))
            `return`(complexFromPolar(pow(p.x, e), p.y * e))
        }
    }
    companion object {
        const val FUNC_NAME = "c_pow"
    }
}

fun KslScopeBuilder.complexDivide(self: KslExprFloat2, other: KslExprFloat2): KslExprFloat2 =
    KslInvokeFunctionVector(
        parentStage.getOrCreateFunction(ComplexDiv.FUNC_NAME) { ComplexDiv(this) },
        this, KslTypeFloat2, self, other)

class ComplexDiv(parentScope: KslScopeBuilder) :
    KslFunction<KslTypeFloat2>(FUNC_NAME, KslTypeFloat2, parentScope.parentStage) {
    init {
        val self = paramFloat2("self")
        val other = paramFloat2("other")
        body.apply {
            val norm = length(other)
            `return` (
                float2Value(self.x * other.x + self.y * other.y, self.y * other.x - self.x * other.y) /
                    (norm * norm)
            )
        }
    }
    companion object {
        const val FUNC_NAME = "c_div"
    }
}

// Computes the complex product of `self * other`.
fun KslScopeBuilder.complexMultiply(self: KslExprFloat2, other: KslExprFloat2): KslExprFloat2 =
    KslInvokeFunctionVector(
        parentStage.getOrCreateFunction(ComplexMultiply.FUNC_NAME) { ComplexMultiply(this) },
        this, KslTypeFloat2, self, other)

class ComplexMultiply(parentScope: KslScopeBuilder) :
    KslFunction<KslTypeFloat2>(FUNC_NAME, KslTypeFloat2, parentScope.parentStage) {
    companion object {
        const val FUNC_NAME = "c_mul"
    }
    init {
        val self = paramFloat2("self")
        val other = paramFloat2("other")
        body.apply {
            `return` (
                float2Value(self.x * other.x - self.y * other.y, self.x * other.y + self.y * other.x)
            )
        }
    }
}


/*
float arg(vec2 c) {
  return atan(c.y, c.x);
}

// Computes the principal value of natural logarithm of `c`.
vec2 c_ln(vec2 c) {
  vec2 polar = c_to_polar(c);
  return vec2(log(polar.x), polar.y);
}

// Returns the logarithm of `c` with respect to an arbitrary base.
vec2 c_log(vec2 c, float base) {
  vec2 polar = c_to_polar(c);
  return vec2(log(polar.r), polar.y) / log(base);
}

// Computes the square root of complex number `c`.
vec2 c_sqrt(vec2 c) {
  vec2 p = c_to_polar(c);
  return c_from_polar(sqrt(p.x), p.y/2.);
}

// Raises `c` to a floating point power `e`.
vec2 c_pow(vec2 c, float e) {
  vec2 p = c_to_polar(c);
  return c_from_polar(pow(p.x, e), p.y*e);
}

// Raises `c` to a complex power `e`.
vec2 c_pow(vec2 c, vec2 e) {
  vec2 polar = c_to_polar(c);
  return c_from_polar(
     pow(polar.x, e.x) * exp(-e.y * polar.y),
     e.x * polar.y + e.y * log(polar.x)
  );
}



vec2 c_sin(vec2 c) {
  return vec2(sin(c.x) * cosh(c.y), cos(c.x) * sinh(c.y));
}

vec2 c_cos(vec2 c) {
  // formula: cos(a + bi) = cos(a)cosh(b) - i*sin(a)sinh(b)
  return vec2(cos(c.x) * cosh(c.y), -sin(c.x) * sinh(c.y));
}

vec2 c_tan(vec2 c) {
  vec2 c2 = 2. * c;
  return vec2(sin(c2.x), sinh(c2.y))/(cos(c2.x) + cosh(c2.y));
}

vec2 c_atan(vec2 c) {
  // formula: arctan(z) = (ln(1+iz) - ln(1-iz))/(2i)
  vec2 i = c_i();
  vec2 one = c_one();
  vec2 two = one + one;
  if (c == i) {
    return vec2(0., 1./1e-10);
  } else if (c == -i) {
    return vec2(0., -1./1e-10);
  }

  return c_div(
    c_ln(one + c_mul(i, c)) - c_ln(one - c_mul(i, c)),
    c_mul(two, i)
  );
}

vec2 c_asin(vec2 c) {
 // formula: arcsin(z) = -i ln(sqrt(1-z^2) + iz)
  vec2 i = c_i(); vec2 one = c_one();
  return c_mul(-i, c_ln(
    c_sqrt(c_one() - c_mul(c, c)) + c_mul(i, c)
  ));
}

vec2 c_acos(vec2 c) {
  // formula: arccos(z) = -i ln(i sqrt(1-z^2) + z)
  vec2 i = c_i();

  return c_mul(-i, c_ln(
    c_mul(i, c_sqrt(c_one() - c_mul(c, c))) + c
  ));
}

vec2 c_sinh(vec2 c) {
  return vec2(sinh(c.x) * cos(c.y), cosh(c.x) * sin(c.y));
}

vec2 c_cosh(vec2 c) {
  return vec2(cosh(c.x) * cos(c.y), sinh(c.x) * sin(c.y));
}

vec2 c_tanh(vec2 c) {
  vec2 c2 = 2. * c;
  return vec2(sinh(c2.x), sin(c2.y))/(cosh(c2.x) + cos(c2.y));
}

vec2 c_asinh(vec2 c) {
  // formula: arcsinh(z) = ln(z + sqrt(1+z^2))
  vec2 one = c_one();
  return c_ln(c + c_sqrt(one + c_mul(c, c)));
}

vec2 c_acosh(vec2 c) {
  // formula: arccosh(z) = 2 ln(sqrt((z+1)/2) + sqrt((z-1)/2))
  vec2 one = c_one();
  vec2 two = one + one;
  return c_mul(two,
      c_ln(
        c_sqrt(c_div((c + one), two)) + c_sqrt(c_div((c - one), two))
      ));
}

vec2 c_atanh(vec2 c) {
  // formula: arctanh(z) = (ln(1+z) - ln(1-z))/2
  vec2 one = c_one();
  vec2 two = one + one;
  if (c == one) {
      return vec2(1./1e-10, vec2(0.));
  } else if (c == -one) {
      return vec2(-1./1e-10, vec2(0.));
  }
  return c_div(c_ln(one + c) - c_ln(one - c), two);
}

// Attempts to identify the gaussian integer whose product with `modulus`
// is closest to `c`
vec2 c_rem(vec2 c, vec2 modulus) {
  vec2 c0 = c_div(c, modulus);
  // This is the gaussian integer corresponding to the true ratio
  // rounded towards zero.
  vec2 c1 = vec2(c0.x - mod(c0.x, 1.), c0.y - mod(c0.y, 1.));
  return c - c_mul(modulus, c1);
}

vec2 c_inv(vec2 c) {
  float norm = length(c);
	return vec2(c.x, -c.y) / (norm * norm);
}
 */