uniform mat4 u_MVMatrix;
uniform mat4 u_MVPMatrix;

attribute vec4 a_Position;
attribute vec3 a_Normal;
attribute vec2 a_TexCoord;

varying vec2 v_TexCoord;
varying vec3 v_PosInEyeSpace;
varying vec3 v_NormalInEyeSpace;

void main(){
    gl_Position = u_MVPMatrix * a_Position;

    v_TexCoord = a_TexCoord;

    v_PosInEyeSpace = (u_MVMatrix * a_Position).xyz;

    v_NormalInEyeSpace = normalize(mat3(u_MVMatrix) * a_Normal);
}
