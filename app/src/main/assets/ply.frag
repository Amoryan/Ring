precision mediump float;

uniform sampler2D u_Texture;

varying vec4 v_Color;
varying vec2 v_TexCoord;

void main(){
    gl_FragColor = v_Color;
//    gl_FragColor = texture2D(u_Texture, v_TexCoord);
}
