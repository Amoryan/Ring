uniform mat4 u_MVPMatrix;

attribute vec4 a_Position;
attribute vec3 a_Normal;

varying vec2 v_TexCoord;

void main(){
    gl_Position = u_MVPMatrix * a_Position;

    // [-1,1]映射到[0,1]
    v_TexCoord = ((mat3(u_MVPMatrix) * a_Normal + 1.0) / 2.0).xy;
}
