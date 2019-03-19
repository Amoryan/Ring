precision mediump float;

uniform vec3 u_LightInEyeSpace;

uniform sampler2D u_Texture;

uniform vec4 u_Color;

varying vec2 v_TexCoord;
varying vec3 v_PosInEyeSpace;
varying vec3 v_NormalInEyeSpace;

void main(){
    vec4 lightColor = vec4(1.0, 1.0, 1.0, 1.0);

    float ambientStrength = 0.3;
    vec4 ambientColor = ambientStrength * lightColor;

    float diffuseStrength = 0.7;
    vec3 lightDirection = normalize(u_LightInEyeSpace - v_PosInEyeSpace);
    float diffuse = max(dot(lightDirection, v_NormalInEyeSpace), 0.0);
    vec4 diffuseColor = diffuseStrength * diffuse * lightColor;

    float specularStrength = 0.7;
    vec3 lightRefDirection = reflect(-lightDirection, v_NormalInEyeSpace);;
    vec3 posEyeNormal = normalize(v_PosInEyeSpace);
    float specular = pow(max(dot(posEyeNormal, lightRefDirection), 0.0), 32.0);
    vec4 specularColor = specularStrength * specular * lightColor;

    gl_FragColor = (ambientColor + diffuseColor + specularColor) * u_Color;
    //    gl_FragColor = texture2D(u_Texture, v_TexCoord);
}
