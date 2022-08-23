package com.example.openglvideoplaying

object ShaderSourceCode {

    const val mVertexShader =
        ("uniform mat4 mvpMatrix;\n" +
                "uniform mat4 uTextureMatrix;\n" +
                "attribute vec4 a_Position;\n" +
                "attribute vec4 a_TexCoordinate;\n" +
                "varying vec2 v_TexCoord;\n" +
                "void main() {\n" +
                "gl_Position = mvpMatrix * a_Position * vec4(1, -1, 1, 1);\n" +
                "v_TexCoord = (uTextureMatrix * a_TexCoordinate).xy;\n" +
                "}")

    const val mFragmentShader =
        ("#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + "varying vec2 v_TexCoord;\n"
                + "uniform samplerExternalOES u_Texture;\n"
                + "void main() {\n"
                + "vec4 color = texture2D(u_Texture, v_TexCoord);\n "
                + " gl_FragColor = color;\n"
                + "}\n")
}


