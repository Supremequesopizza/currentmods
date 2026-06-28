#version 120

uniform sampler2D vortexTexture;
uniform float time;
uniform float alpha;
uniform float fadeInProgress;
uniform float fadeOutProgress;
uniform bool isFadingIn;
uniform bool isFadingOut;

//miracle that this fucking works

float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

float noise(vec2 st) {
    vec2 i = floor(st);
    vec2 f = fract(st);

    float a = random(i);
    float b = random(i + vec2(1.0, 0.0));
    float c = random(i + vec2(0.0, 1.0));
    float d = random(i + vec2(1.0, 1.0));

    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

void main() {
    vec2 uv = gl_TexCoord[0].st;
    vec2 center = vec2(0.5);

    float distortion1 = noise(uv * 5.0 + time * 0.2);
    float distortion2 = noise(uv * 3.0 - time * 0.1);
    float pulse = sin(time * 0.8) * 0.5 + 0.5;
    vec2 distortion_offset = vec2(distortion1, distortion2) * 0.03 * pulse;

    float strain_noise = noise(uv * 40.0 + time * 4.0) * 0.05;
    vec2 direction = normalize(uv - center);
    vec2 final_uv = uv + distortion_offset - (direction * strain_noise);

    vec4 vortexColor = texture2D(vortexTexture, final_uv);
    float baseAlpha = vortexColor.a;
    float finalAlpha = baseAlpha * alpha;

    if (isFadingIn) {
        float dist = distance(uv, center) * 1.414;
        
        float pulse_effect = sin(fadeInProgress * 25.0) * 0.05;
        float wipe_edge = 1.0 - (fadeInProgress + pulse_effect);

        float fadeInMask = smoothstep(wipe_edge, wipe_edge + 0.15, dist);
        finalAlpha *= fadeInMask;
    }

     if (isFadingOut) {
        vec2 center = vec2(0.5);
        float dist = distance(uv, center) * 1.414;
        
        float strain_noise = noise(uv * 40.0 + time * 4.0) * 0.1;
        dist -= strain_noise;
        
        float fadeOutMask = smoothstep(fadeOutProgress, fadeOutProgress + 0.1, dist);
        finalAlpha *= fadeOutMask;
    }

    gl_FragColor = vec4(vortexColor.rgb, finalAlpha);
}