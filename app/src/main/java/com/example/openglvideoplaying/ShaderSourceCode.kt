package com.example.openglvideoplaying

object ShaderSourceCode {

    const val mVertexShader =
        "uniform mat4 u_MVP;" +
                "attribute vec4 a_Position;" +
                "attribute vec2 a_TexCoordinate;" +
                "varying vec2 v_TexCoord;" +

                "void main() {" +
                "v_TexCoord = a_TexCoordinate;" +
                "gl_Position = u_MVP * a_Position;" +
                "}"

    const val mFragmentShader =
        ("#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + "varying vec2 v_TexCoord;\n"
                + "uniform samplerExternalOES u_Texture;\n"
                + "void main() {\n"
                + "  gl_FragColor = texture2D(u_Texture, v_TexCoord);\n"
                + "}\n")
}


