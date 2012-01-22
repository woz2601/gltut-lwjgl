package rosick.mckesson.tut08;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;

import rosick.GLWindow;
import rosick.framework.Framework;
import rosick.framework.Mesh;
import rosick.glm.Glm;
import rosick.glm.Mat4;
import rosick.glm.Quaternion;
import rosick.glm.Vec3;
import rosick.glm.Vec4;
import rosick.glutil.MatrixStack;


/**
 * Visit https://github.com/rosickteam/OpenGL for project info, updates and license terms.
 * 
 * II. Positioning
 * 8. Getting Oriented 
 * http://www.arcsynthesis.org/gltut/Positioning/Tutorial%2008.html
 * @author integeruser
 */
public class CameraRelative03 extends GLWindow {
	
	public static void main(String[] args) {		
		new CameraRelative03().start(800, 800);
	}
	
	
	private static final String BASEPATH = "/rosick/mckesson/tut08/data/";
	
	
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
	 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	
	private int theProgram;
	private int modelToCameraMatrixUnif, cameraToClipMatrixUnif, baseColorUnif;
	private Mat4 cameraToClipMatrix = new Mat4();
	private FloatBuffer tempSharedBuffer = BufferUtils.createFloatBuffer(16);

	private MatrixStack currMatrix = new MatrixStack(); 

	
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
	 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

	@Override
	protected void init() {
		initializeProgram();
		
		try {		
			g_pShip = new Mesh(BASEPATH + "Ship.xml");
			g_pPlane = new Mesh(BASEPATH + "UnitPlane.xml");
		} catch (Exception exception) {
			exception.printStackTrace();
			System.exit(0);
		}		
		
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		glFrontFace(GL_CW);

		glEnable(GL_DEPTH_TEST);
		glDepthMask(true);
		glDepthFunc(GL_LEQUAL);
		glDepthRange(0.0f, 1.0f);
	}
	
	private void initializeProgram() {			
		int vertexShader =		Framework.loadShader(GL_VERTEX_SHADER, 		BASEPATH + "PosColorLocalTransform.vert");
		int fragmentShader = 	Framework.loadShader(GL_FRAGMENT_SHADER, 	BASEPATH + "ColorMultUniform.frag");
        
		ArrayList<Integer> shaderList = new ArrayList<>();
		shaderList.add(vertexShader);
		shaderList.add(fragmentShader);

		theProgram = Framework.createProgram(shaderList);
		

		modelToCameraMatrixUnif = glGetUniformLocation(theProgram, "modelToCameraMatrix");
		cameraToClipMatrixUnif = glGetUniformLocation(theProgram, "cameraToClipMatrix");
		baseColorUnif = glGetUniformLocation(theProgram, "baseColor");

		float fzNear = 1.0f; float fzFar = 600.0f;
		
		cameraToClipMatrix.put(0,	fFrustumScale);
		cameraToClipMatrix.put(5, 	fFrustumScale);
		cameraToClipMatrix.put(10, 	(fzFar + fzNear) / (fzNear - fzFar));
		cameraToClipMatrix.put(11, 	-1.0f);
		cameraToClipMatrix.put(14, 	(2 * fzFar * fzNear) / (fzNear - fzFar));

		glUseProgram(theProgram);
		glUniformMatrix4(cameraToClipMatrixUnif, false, cameraToClipMatrix.fillBuffer(tempSharedBuffer));
		glUseProgram(0);
	}
	
	
	@Override
	protected void update() {
		lastFrameDuration *= 5;
	
		if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
			offsetOrientation(new Vec3(0.0f, 0.0f, 1.0f), (float) (SMALL_ANGLE_INCREMENT * lastFrameDuration));
		} else if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
			offsetOrientation(new Vec3(0.0f, 0.0f, 1.0f), (float) (-SMALL_ANGLE_INCREMENT * lastFrameDuration));
		}

		if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
			offsetOrientation(new Vec3(1.0f, 0.0f, 0.0f), (float) (SMALL_ANGLE_INCREMENT * lastFrameDuration));
		} else if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
			offsetOrientation(new Vec3(1.0f, 0.0f, 0.0f), (float) (-SMALL_ANGLE_INCREMENT * lastFrameDuration));
		}

		if (Keyboard.isKeyDown(Keyboard.KEY_Q)) {
			offsetOrientation(new Vec3(0.0f, 1.0f, 0.0f), (float) (SMALL_ANGLE_INCREMENT * lastFrameDuration));
		} else if (Keyboard.isKeyDown(Keyboard.KEY_E)) {
			offsetOrientation(new Vec3(0.0f, 1.0f, 0.0f), (float) -(SMALL_ANGLE_INCREMENT * lastFrameDuration));
		}
		
		
		if (Keyboard.isKeyDown(Keyboard.KEY_J)) {
			g_sphereCamRelPos.x -= 11.25f * lastFrameDuration;
		} else if (Keyboard.isKeyDown(Keyboard.KEY_L)) {
			g_sphereCamRelPos.x += 11.25f * lastFrameDuration;
		}

		if (Keyboard.isKeyDown(Keyboard.KEY_I)) {
			g_sphereCamRelPos.y -= 11.25f * lastFrameDuration;
		} else if (Keyboard.isKeyDown(Keyboard.KEY_K)) {
			g_sphereCamRelPos.y += 11.25f * lastFrameDuration;
		}

		
		if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
			leaveMainLoop();
		}
		
		
		while (Keyboard.next()) {
			if (Keyboard.getEventKeyState()) {
				if (Keyboard.getEventKey() == Keyboard.KEY_SPACE) {
					int ordinal = g_iOffset.ordinal();
					ordinal += 1;
					ordinal = ordinal % OffsetRelative.NUM_RELATIVES.ordinal();
					
					g_iOffset = OffsetRelative.values()[ordinal];
					
					switch (g_iOffset) {
						case MODEL_RELATIVE: 	
							System.out.println("Model Relative"); 
							break;
						case WORLD_RELATIVE: 	
							System.out.println("World Relative"); 
							break;
						case CAMERA_RELATIVE: 	
							System.out.println("Camera Relative"); 
							break;
					}
				}
			}
		}


		g_sphereCamRelPos.y = Glm.clamp(g_sphereCamRelPos.y, -78.75f, 10.0f);
	}
	

	@Override
	protected void display() {			
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		glClearDepth(1.0f);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		currMatrix.clear();
		final Vec3 camPos = resolveCamPosition();
		currMatrix.setMatrix(calcLookAtMatrix(camPos, g_camTarget, new Vec3(0.0f, 1.0f, 0.0f)));

		glUseProgram(theProgram);

		{
			currMatrix.push();
			
			currMatrix.scale(100.0f, 1.0f, 100.0f);

			glUniform4f(baseColorUnif, 0.2f, 0.5f, 0.2f, 1.0f);
			glUniformMatrix4(modelToCameraMatrixUnif, false, currMatrix.top().fillBuffer(tempSharedBuffer));

			g_pPlane.render();
			
			currMatrix.pop();
		}

		{
			currMatrix.push();
			
			currMatrix.translate(g_camTarget);
			currMatrix.applyMatrix(Glm.matCast(g_orientation));
			currMatrix.rotateX(-90.0f);

			//Set the base color for this object.
			glUniform4f(baseColorUnif, 1.0f, 1.0f, 1.0f, 1.0f);
			glUniformMatrix4(modelToCameraMatrixUnif, false, currMatrix.top().fillBuffer(tempSharedBuffer));

			g_pShip.render(/*"tint"*/);
			
			currMatrix.pop();
		}

		glUseProgram(0);
	}
	
	
	@Override
	protected void reshape(int width, int height) {	
		cameraToClipMatrix.put(0, fFrustumScale / (width / (float) height));
		cameraToClipMatrix.put(5, fFrustumScale);

		glUseProgram(theProgram);
		glUniformMatrix4(cameraToClipMatrixUnif, false, cameraToClipMatrix.fillBuffer(tempSharedBuffer));
		glUseProgram(0);

		glViewport(0, 0, width, height);
	}
	
	
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
	 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

	private final float SMALL_ANGLE_INCREMENT = 9.0f;

	private static Vec3 g_camTarget = new Vec3(0.0f, 10.0f, 0.0f);
	private static Quaternion g_orientation = new Quaternion(1.0f, 0.0f, 0.0f, 0.0f);
	private static OffsetRelative g_iOffset = OffsetRelative.MODEL_RELATIVE;
	// In spherical coordinates.
	private static Vec3 g_sphereCamRelPos = new Vec3(90.0f, 0.0f, 66.0f);
	
	private Mesh g_pShip;
	private Mesh g_pPlane;
	
	
	private enum OffsetRelative {
		MODEL_RELATIVE,
		WORLD_RELATIVE,
		CAMERA_RELATIVE,

		NUM_RELATIVES;
	};
	
	
	private void offsetOrientation(Vec3 _axis, float fAngDeg) {
		float fAngRad = Framework.degToRad(fAngDeg);

		Vec3 axis = Glm.normalize(_axis);
		axis.scale((float) Math.sin(fAngRad / 2.0f));
		
		float scalar = (float) Math.cos(fAngRad / 2.0f);

		Quaternion offset = new Quaternion(scalar, axis.x, axis.y, axis.z);

		switch (g_iOffset) {
			case MODEL_RELATIVE:
				g_orientation = Quaternion.mul(g_orientation, offset);
				break;
			case WORLD_RELATIVE:
				g_orientation = Quaternion.mul(offset, g_orientation);
				break;
			case CAMERA_RELATIVE: 
				{
					final Vec3 camPos = resolveCamPosition();
					final Mat4 camMat = calcLookAtMatrix(camPos, g_camTarget, new Vec3(0.0f, 1.0f, 0.0f));
		
					Quaternion viewQuat = Glm.quatCast(camMat);
					Quaternion invViewQuat = Glm.conjugate(viewQuat);
		
					final Quaternion worldQuat = invViewQuat.mul(offset.mul(viewQuat));
					g_orientation = Quaternion.mul(worldQuat, g_orientation);
				}
				break;
		}

		g_orientation = Glm.normalize(g_orientation);
	}
	
	
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
	 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
		
	private final float fFrustumScale = calcFrustumScale(20.0f);

	
	private float calcFrustumScale(float fFovDeg) {
		final float degToRad = 3.14159f * 2.0f / 360.0f;
		float fFovRad = fFovDeg * degToRad;
		
		return (float) (1.0f / Math.tan(fFovRad / 2.0f));
	}
	
	
	private Vec3 resolveCamPosition() {
		float phi = Framework.degToRad(g_sphereCamRelPos.x);
		float theta = Framework.degToRad(g_sphereCamRelPos.y + 90.0f);

		float fSinTheta = (float) Math.sin(theta);
		float fCosTheta = (float) Math.cos(theta);
		float fCosPhi = (float) Math.cos(phi);
		float fSinPhi = (float) Math.sin(phi);

		Vec3 dirToCamera = new Vec3(fSinTheta * fCosPhi, fCosTheta, fSinTheta * fSinPhi);
		
		return (dirToCamera.scale(g_sphereCamRelPos.z)).add(g_camTarget);
	}
	
	
	private Mat4 calcLookAtMatrix(Vec3 cameraPt, Vec3 lookPt, Vec3 upPt) {
		Vec3 lookDir = Glm.normalize(Vec3.sub(lookPt, cameraPt));
		Vec3 upDir = Glm.normalize(upPt);

		Vec3 rightDir = Glm.normalize(Glm.cross(lookDir, upDir));
		Vec3 perpUpDir = Glm.cross(rightDir, lookDir);

		Mat4 rotMat = new Mat4(1.0f);
		rotMat.putColumn(0, new Vec4(rightDir, 0.0f));
		rotMat.putColumn(1, new Vec4(perpUpDir, 0.0f));
		rotMat.putColumn(2, new Vec4(Vec3.negate(lookDir), 0.0f));

		rotMat = Glm.transpose(rotMat);

		Mat4 transMat = new Mat4(1.0f);
		transMat.putColumn(3, new Vec4(Vec3.negate(cameraPt), 1.0f));

		rotMat.mul(transMat);
		
		return rotMat;
	}
}