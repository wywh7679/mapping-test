#version 330
#define BLUR_HORIZONTAL 1
#define BLUR_VERTICAL 2
#define KERNELSIZE 11
#define KERNELSIZEHALF 5
in vec2 texc;
uniform int blurDirection;
uniform float width;
uniform float height;
uniform sampler2D tex;

/* As per the convention */
out vec4 colorOut0;

const float gaussKernel[KERNELSIZE] = float[11] (
    0.042557, 0.056743, 0.075657, 0.100876, 0.134501,
    0.179335,
    0.134501, 0.100876, 0.075657, 0.056743, 0.042557
);

vec4 gaussBlurHorizontal()
{
    float x = gl_FragCoord.x;
    float reciWidth = 1.0 / width;
    vec4 result = vec4(0.0);
    for(int i = 0; i<KERNELSIZE; ++i){
        float tc = (float(i-KERNELSIZEHALF) + x) * reciWidth;
        /* Look up texel and apply weight */
        result += texture(tex, vec2(tc, texc.y)) * gaussKernel[i];
    }
    return result;
}

vec4 gaussBlurVertical()
{
    float y = gl_FragCoord.y;
    float reciHeight = 1.0 / height;
    vec4 result = vec4(0.0);
    for(int i = 0; i<KERNELSIZE; ++i){
        float tc = (float(i-KERNELSIZEHALF) + y) * reciHeight;
        result += texture(tex, vec2(texc.x, tc)) * gaussKernel[i];
    }
    return result;
}

void main()
{
    if(blurDirection == BLUR_HORIZONTAL)
        colorOut0 = gaussBlurHorizontal();
    else //blurDirection == BLUR_VERTICAL
        colorOut0 = gaussBlurVertical();
}