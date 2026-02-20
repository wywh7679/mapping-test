attribute vec4 a_Position;
uniform mat4 u_ModelViewProjectionMatrix;
attribute vec4 a_Color;
varying vec4 v_Color;

void main() {
    v_Color = a_Color;
    gl_Position = u_ModelViewProjectionMatrix * a_Position;
    gl_PointSize = 1.0; // Set size for point rendering
}