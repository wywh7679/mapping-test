attribute vec4 a_Position;
attribute vec2 a_TexCoord;
attribute vec4 a_Color;
uniform mat4 u_ModelViewProjectionMatrix;
varying vec2 v_TexCoord;
varying vec4 v_Color;

void main() {
    v_TexCoord = a_TexCoord;
    v_Color = a_Color;
    gl_Position = u_ModelViewProjectionMatrix * a_Position;
}
