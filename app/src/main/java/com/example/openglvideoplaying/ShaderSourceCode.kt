package com.example.openglvideoplaying

object ShaderSourceCode {

    const val mVertexShader =
        "attribute vec4 a_Position;" +
                "attribute vec2 a_TexCoordinate;" +
                "varying vec2 v_TexCoord;" +

                "void main() {" +
                "v_TexCoord = a_TexCoordinate;" +
                "gl_Position = a_Position;" +
                "}"


    const val mFragmentShader =
    "#extension GL_OES_EGL_image_external : require" +
    "precision mediump float;" +

    "uniform samplerExternalOES u_Texture;" +
    "varying vec2 v_TexCoord;" +

    "void main() {"+
        "gl_FragColor = texture2D(u_Texture, v_TexCoord);" +
    "}"
}