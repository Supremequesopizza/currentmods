#version 120

uniform float iGlobalTime;
uniform vec2 iResolution;

void main(void)
{
    vec2 uv = (gl_FragCoord.xy * 2.0 - iResolution.xy) / min(iResolution.x, iResolution.y);
    
    float dist = length(uv);
    
    float alpha = 1.0 - smoothstep(0.9, 1.0, dist);

    gl_FragColor = vec4(1.0, 0.0, 1.0, alpha);
}