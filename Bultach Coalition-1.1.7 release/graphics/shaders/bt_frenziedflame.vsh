#version 120

uniform vec2 u_viewport_pos;    
uniform vec2 u_viewport_size;   
void main(void)
{


    vec2 relative_pos = gl_Vertex.xy - u_viewport_pos;

    vec2 normalized_pos = relative_pos / u_viewport_size;

    vec2 clip_space_pos = (normalized_pos * 2.0) - 1.0;

    gl_Position = vec4(clip_space_pos, 0.0, 1.0);

    gl_TexCoord[0] = gl_MultiTexCoord0;
}