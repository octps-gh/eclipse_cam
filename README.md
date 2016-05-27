##Shader Camera
Originally Created By [LittleCheeseCake](http://littlecheesecake.me)
Modified My YuZhang (yuzhang7676@163.com) on May,27,2016

Occasionally I receives emails asking how to render Camera Frame using OpenGL ES on Android. I lazily used some codes from an open source project [InstaCam](https://github.com/harism/android_instacam) without fully understand until recently I reviewed some fundamentals of OpenGL ES and re-impelemented a demo app for camera frame rendering using shaders. This post is to discuss some key aspects in the implementation and share the demo codes.

###CameraRenderer
Drawing using OpenGL is implemented by rendering on GLSurfaceView. The common approach is subclass the GLSurfaceView and implements the GLSurfaceView.Renderer. The rendering tasks are performed by implementing the interface.

```java
public class CameraRenderer extends GLSurfaceView implements 
								GLSurfaceView.Renderer, 
								SurfaceTexture.OnFrameAvailableListener{
	@Override
	public synchronized void onSurfaceCreated(GL10 gl, EGLConfig config)
	{
		...
		//compile shader here
	}
	
	@Override
	public synchronized void onSurfaceChanged(GL10 gl, int width, int height) {
	   ...
	   //open camera and start preview here
	}
	
	@Override
	public synchronized void onDrawFrame(GL10 gl) {
	    ...
	    //draw frame as required
	}
	
}
```

###SurfaceTexture
SurfaceTexture Interface provided by Android SDK (API Level 11+) has made our life much easier when dealing with image streaming either from Camera or MediaPlayer. SurfaceTexture is bound with an OpenGL Texture id at its instantiate (mCameraTexture is discussed later, which generates OpenGL texture handle):

```java
@Override
public synchronized void onSurfaceChanged(GL10 gl, int width, int height) {
    ...
    SurfaceTexture mSurfaceTexture = new SurfaceTexture(mCameraTexture.getTextureId());
    ...
}

```
Listeners can be registered to SurfaceTexture.setOnFrameAvailable to make updates whenever a new frame is streamed in. Here the camera renderer is registered to listening for the updates, whenever a new frame is streamed in, the renderer is required to draw a new frame on the surface. Use mSurfaceTexture.updateTexImage() to query the most recent frame on the stream.

```java
@Override
public synchronized void onSurfaceChanged(GL10 gl, int width, int height) {
    ...
    mSurfaceTexture.setOnFrameAvailableListener(this);
    ...
}

...

@Override
public synchronized void onFrameAvailable(SurfaceTexture surfaceTexture)
{
    //request the renderer to draw frame when new frame available
	updateTexture = true;
	requestRender();
}

...

@Override
public synchronized void onDrawFrame(GL10 gl) {
    ...
	//render the texture to FBO if new frame is available, double check
	if(updateTexture){
		mSurfaceTexture.updateTexImage();
		...
	}
}
```
The texture updated by SurfaceTexture can only be bound to GL_TEXTURE_EXTERNAL_OES target rather than the GL_TEXTURE_2D target. Therefore a texture handle generated by the mCameraTexture object as mentioned above using the following implementation (bind with GLES11Ext.GL_TEXTURE_EXTERNAL_OES):

```java
public class OESTexture {
	...

	public void init(){
		int[] mTextureHandles = new int[1];
		GLES20.glGenTextures(1, mTextureHandles, 0);
		mTextureHandle = mTextureHandles[0];

		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureHandles[0]);
		...

	}
}
```
In the fragment shader when the texture is binded, the first line has to be inserted:

```c
#extension GL_OES_EGL_image_external : require

precision mediump float;

uniform samplerExternalOES sTexture;
varying vec2 vTextureCoord;

void main(){
	gl_FragColor = texture2D(sTexture, vTextureCoord);
}
```

###Draw Screen Quad
Using shaders, a simple screen quad can be easily drawn.

```java
private void init(){
	//Create full scene quad buffer
	final byte FULL_QUAD_COORDS[] = {-1, 1, -1, -1, 1, 1, 1, -1};
	mFullQuadVertices = ByteBuffer.allocateDirect(4 * 2);
	mFullQuadVertices.put(FULL_QUAD_COORDS).position(0);

	...
}

...
private void renderQuad(int aPosition){
	GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_BYTE, false, 0, mFullQuadVertices);
	GLES20.glEnableVertexAttribArray(aPosition);
	GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
}
```

Three issues are considered here 

1. A transformation matrix is queried using mSurfaceTexture.getTransformMatrix(float[]). This matrix transforms traditional 2D OpenGL ES texture coordinate column vectors of the form (s, t, 0, 1) where s and t are on the inclusive interval [0, 1] to the proper sampling location in the streamed texture. 

2. The orientation change of the phone has effect on the Surface dimension change (Height and Width swapped) but has no effect on the camera size (Width and Height remains as Width > Height all the time). This should be considered using an orientation matrix passed to the shader to adjust the orientation of the frame whenever the phone's orientation changes.

3. The screen dimension (SurfaceView dimension and the camera frame dimension might not be the same, to maintain a proper w/h ratio, a scaling factor should be passed to the shader to resize the screen quad.

The codes below are the passing of the three parameters to the shader. Noted that uTransformM updated every frame as required, uOrientationM updated whenever the orientation of the phone changes, and ratios updated also when the orientation of the phone changes since the w/h ratio changes when their actual values change. The later two are updated in the onSurfaceChanged(GL10, int width, int height) method. 

```java
@Override
public synchronized void onDrawFrame(GL10 gl) {
    ...
    int uTransformM = mOffscreenShader.getHandle("uTransformM");
	int uOrientationM = mOffscreenShader.getHandle("uOrientationM");
	int uRatioV = mOffscreenShader.getHandle("ratios");

	GLES20.glUniformMatrix4fv(uTransformM, 1, false, mTransformM, 0);
	GLES20.glUniformMatrix4fv(uOrientationM, 1, false, mOrientationM, 0);
	GLES20.glUniform2fv(uRatioV, 1, mRatio, 0);
	...
}
```

In the vertex shader, uTransformM, uOrientationM and ratios together do some work to make sure the frame texture coordinate fit into the phone window:

```c
uniform mat4 uTransformM;
uniform mat4 uOrientationM;
uniform vec2 ratios;
attribute vec2 aPosition;

varying vec2 vTextureCoord;

void main(){
	gl_Position = vec4(aPosition, 0.0, 1.0);
	vTextureCoord = (uTransformM * ((uOrientationM * gl_Position + 1.0)*0.5)).xy;
	gl_Position.xy *= ratios;
}
```

###Closure
Some details are not covered in the post. It might be confusing by looking at the code fragments above. Here what I want to do is to take note of the critical steps in my implementation for my own record. I think it will be much helpful to go through the complete implementation of the [demo app](https://github.com/yulu/ShaderCam) (which is quite concise, only a few hundered lines of codes). What will be more interesting? Try to replace the fragment shader with some funny shaders in [Instagram_Filter](https://github.com/yulu/Instagram_Filter) and [ShaderFilter](https://github.com/yulu/ShaderFilter), to see the interesting filter applied real-time on camera view.

![manga](https://dl.dropboxusercontent.com/spa/pv9m61pztxstay5/manga.png)


