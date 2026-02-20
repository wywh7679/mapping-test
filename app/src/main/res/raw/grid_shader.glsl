precision mediump float;
uniform vec3 u_Color; // Uniform variable for color
varying vec3 v_Color;

void main() {
    gl_FragColor = v_Color;
}