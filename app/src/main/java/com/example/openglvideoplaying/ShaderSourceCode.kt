package com.example.openglvideoplaying

object ShaderSourceCode {
//    const val mVertexShader = "uniform mat4 uMVPMatrix;\n" +
//            "uniform mat4 uSTMatrix;\n" +
//            "attribute vec4 aPosition;\n" +
//            "attribute vec4 aTextureCoord;\n" +
//            "varying vec2 vTextureCoord;\n" +
//            "void main() {\n" +
//            "  gl_Position = uMVPMatrix * aPosition;\n" +
//            "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
//            "}\n"
//
//    const val mFragmentShader = "#extension GL_OES_EGL_image_external : require\n" +
//            "precision mediump float;" +
//            "varying vec2 vTextureCoord;" +
//            "uniform samplerExternalOES sTexture;" +
//            "void main() {\n" +
//            "  gl_FragColor = texture2D(sTexture, vTextureCoord);" +
//            "}\n"

    const val mVertexShader =
        "uniform mat4 uMVPMatrix;" +
                "uniform mat4 uSTMatrix;" +
                "attribute vec4 aPosition;" +
                "attribute vec4 aTextureCoord;" +
                "varying vec2 vTextureCoord;" +
                "void main() {" +
                "  gl_Position = uMVPMatrix * aPosition;" +
                "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;" +
                "}"

//    const val mVertexShader =
//        "attribute vec4 a_Position;"+
//    "attribute vec2 a_TexCoordinate;" +
//    "varying vec2 v_TexCoord;" +
//
//    "void main() {" +
//        "v_TexCoord = a_TexCoordinate;"+
//        "gl_Position = a_Position;" +
//    "}"

    const val mFragmentShader =
        "#extension GL_OES_EGL_image_external : require" +
                "precision mediump float;" +
                "varying vec2 vTextureCoord;" +
                "uniform samplerExternalOES sTexture;" +
                "void main() {" +
                "  gl_FragColor = texture2D(sTexture, vTextureCoord);" +
                "}"
}