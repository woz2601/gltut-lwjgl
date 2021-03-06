package rosick.mckesson.IV.tut17;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import rosick.LWJGLWindow;
import rosick.jglsdk.BufferableData;
import rosick.jglsdk.glimg.DdsLoader;
import rosick.jglsdk.glimg.ImageSet;
import rosick.jglsdk.glimg.TextureGenerator;
import rosick.jglsdk.glimg.ImageSet.Dimensions;
import rosick.jglsdk.glimg.ImageSet.SingleImage;
import rosick.jglsdk.glm.Glm;
import rosick.jglsdk.glm.Mat4;
import rosick.jglsdk.glm.Quaternion;
import rosick.jglsdk.glm.Vec3;
import rosick.jglsdk.glm.Vec4;
import rosick.jglsdk.glutil.MatrixStack;
import rosick.jglsdk.glutil.MousePoles.*;
import rosick.mckesson.framework.Framework;
import rosick.mckesson.framework.Mesh;
import rosick.mckesson.framework.MousePole;
import rosick.mckesson.framework.Scene;
import rosick.mckesson.framework.Scene.SceneNode;
import rosick.mckesson.framework.SceneBinders;
import rosick.mckesson.framework.Timer;
import rosick.mckesson.framework.SceneBinders.UniformIntBinder;
import rosick.mckesson.framework.SceneBinders.UniformMat4Binder;
import rosick.mckesson.framework.SceneBinders.UniformVec3Binder;


/**
 * Visit https://github.com/rosickteam/OpenGL for project info, updates and license terms.
 * 
 * IV. Texturing
 * 17. Spotlight on Textures
 * http://www.arcsynthesis.org/gltut/Texturing/Tutorial%2017.html
 * @author integeruser
 *
 * W,A,S,D	- move the camera forward/backwards and left/right, relative to the camera's current orientation.
 * 				Holding SHIFT with these keys will move in smaller increments.
 * SPACE	- reset the projected flashlight direction.
 * T		- toggle viewing of the current target point.
 * G		- toggle all of the regular lighting on and off.
 * P		- toggle pausing.
 * 1,2		- toggle between different light textures.
 * 
 * LEFT	  CLICKING and DRAGGING			- rotate the camera around the target point, both horizontally and vertically.
 * LEFT	  CLICKING and DRAGGING + CTRL	- rotate the camera around the target point, either horizontally or vertically.
 * LEFT	  CLICKING and DRAGGING + ALT	- change the camera's up direction.
 * RIGHT  CLICKING and DRAGGING			- rotate the orientation of the light horizontally and vertically, relative to the current camera view.
 * RIGHT  CLICKING and DRAGGING + CTRL	- rotate the orientation of the light horizontally or vertically only, relative to the current camera view.
 * RIGHT  CLICKING and DRAGGING + ALT	- spin the orientation of the light.
 * WHEEL  SCROLLING						- move the camera closer to it's target point or farther away. 
 */
public class CubePointLight03 extends LWJGLWindow {
	
	public static void main(String[] args) {
		Framework.CURRENT_TUTORIAL_DATAPATH = "/rosick/mckesson/IV/tut17/data/";
		
		new CubePointLight03().start(displayWidth, displayHeight);
	}

	
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
	 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
	 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	
	@Override
	protected void init() {
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		glFrontFace(GL_CW);
		
		final float depthZNear = 0.0f;
		final float depthZFar = 1.0f;

		glEnable(GL_DEPTH_TEST);
		glDepthMask(true);
		glDepthFunc(GL_LEQUAL);
		glDepthRange(depthZNear, depthZFar);
		glEnable(GL_DEPTH_CLAMP);
		glEnable(GL_FRAMEBUFFER_SRGB);

		// Setup our Uniform Buffers
		projectionUniformBuffer = glGenBuffers();
		glBindBuffer(GL_UNIFORM_BUFFER, projectionUniformBuffer);
		glBufferData(GL_UNIFORM_BUFFER, ProjectionBlock.SIZE, GL_STREAM_DRAW);

		glBindBufferRange(GL_UNIFORM_BUFFER, projectionBlockIndex, projectionUniformBuffer,
				0, ProjectionBlock.SIZE);

		createSamplers();
		loadTextures();
		
		try {
			loadAndSetupScene();
		} catch (Exception exception) {
			exception.printStackTrace();
			System.exit(-1);
		}

		lightUniformBuffer = glGenBuffers();
		glBindBuffer(GL_UNIFORM_BUFFER, lightUniformBuffer);
		glBufferData(GL_UNIFORM_BUFFER, LightBlock.SIZE, GL_STREAM_DRAW);

		glBindBufferRange(GL_UNIFORM_BUFFER, lightBlockIndex, lightUniformBuffer,
				0, LightBlock.SIZE);

		glBindBuffer(GL_UNIFORM_BUFFER, 0);
	}
	

	@Override
	protected void update() {
		while (Mouse.next()) {
			int eventButton = Mouse.getEventButton();
									
			if (eventButton != -1) {
				boolean pressed = Mouse.getEventButtonState();
				MousePole.forwardMouseButton(viewPole, eventButton, pressed, Mouse.getX(), Mouse.getY());			
				MousePole.forwardMouseButton(lightPole, eventButton, pressed, Mouse.getX(), Mouse.getY());			
			} else {
				// Mouse moving or mouse scrolling
				int dWheel = Mouse.getDWheel();
				
				if (dWheel != 0) {
					MousePole.forwardMouseWheel(viewPole, dWheel, dWheel, Mouse.getX(), Mouse.getY());
					MousePole.forwardMouseWheel(lightPole, dWheel, dWheel, Mouse.getX(), Mouse.getY());
				}
				
				if (Mouse.isButtonDown(0) || Mouse.isButtonDown(1) || Mouse.isButtonDown(2)) {
					MousePole.forwardMouseMotion(viewPole, Mouse.getX(), Mouse.getY());			
					MousePole.forwardMouseMotion(lightPole, Mouse.getX(), Mouse.getY());			
				}
			}
		}
		
		
		float lastFrameDuration = getLastFrameDuration() * 10 / 1000.0f;
		
		if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
			viewPole.charPress(Keyboard.KEY_W, Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT), lastFrameDuration);
		} else if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
			viewPole.charPress(Keyboard.KEY_S, Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT), lastFrameDuration);
		}
		
		if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
			viewPole.charPress(Keyboard.KEY_D, Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT), lastFrameDuration);
		} else if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
			viewPole.charPress(Keyboard.KEY_A, Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT), lastFrameDuration);
		}

		if (Keyboard.isKeyDown(Keyboard.KEY_E)) {
			viewPole.charPress(Keyboard.KEY_E, Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT), lastFrameDuration);
		} else if (Keyboard.isKeyDown(Keyboard.KEY_Q)) {
			viewPole.charPress(Keyboard.KEY_Q, Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT), lastFrameDuration);
		}
		
		
		while (Keyboard.next()) {
			if (Keyboard.getEventKeyState()) {
				switch (Keyboard.getEventKey()) {
				case Keyboard.KEY_SPACE:
					lightPole.reset();
					break;
					
				case Keyboard.KEY_T:
					drawCameraPos = !drawCameraPos;
					break;

				case Keyboard.KEY_G:
					showOtherLights = !showOtherLights;
					break;
						
				case Keyboard.KEY_P:
					timer.togglePause();
					break;
					
				case Keyboard.KEY_1:
				case Keyboard.KEY_2:
					int number = Keyboard.getEventKey() - Keyboard.KEY_1;
					currTextureIndex = number;
					System.out.printf("%s\n", texDefs[currTextureIndex].name);
					break;
					
				case Keyboard.KEY_ESCAPE:
					leaveMainLoop();
					break;
				}
			}
		}
	}
	

	@Override
	protected void display() {
		timer.update(getElapsedTime());

		glClearColor(0.8f, 0.8f, 0.8f, 1.0f);
		glClearDepth(1.0f);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		final Mat4 cameraMatrix = viewPole.calcMatrix();
		final Mat4 lightView = lightPole.calcMatrix();
		
		MatrixStack modelMatrix = new MatrixStack();
		modelMatrix.applyMatrix(cameraMatrix);

		buildLights(cameraMatrix);
		
		nodes.get(0).nodeSetOrient(Glm.rotate(new Quaternion(1.0f),
				360.0f * timer.getAlpha(), new Vec3(0.0f, 1.0f, 0.0f)));

		nodes.get(3).nodeSetOrient(Quaternion.mul(spinBarOrient, Glm.rotate(new Quaternion(1.0f),
				360.0f * timer.getAlpha(), new Vec3(0.0f, 0.0f, 1.0f))));

		{		
			MatrixStack persMatrix = new MatrixStack();
			persMatrix.perspective(60.0f, displayWidth / (float) displayHeight, zNear, zFar);
			
			ProjectionBlock projData = new ProjectionBlock();
			projData.cameraToClipMatrix = persMatrix.top();

			glBindBuffer(GL_UNIFORM_BUFFER, projectionUniformBuffer);
			glBufferData(GL_UNIFORM_BUFFER, projData.fillAndFlipBuffer(mat4Buffer), GL_STREAM_DRAW);
			glBindBuffer(GL_UNIFORM_BUFFER, 0);
		}

		glActiveTexture(GL_TEXTURE0 + lightProjTexUnit);
		glBindTexture(GL_TEXTURE_CUBE_MAP, lightTextures[currTextureIndex]);
		glBindSampler(lightProjTexUnit, samplers[currSampler]);	
		
		{
			MatrixStack lightProjStack = new MatrixStack();		
			lightProjStack.applyMatrix(Glm.inverse(lightView));
			lightProjStack.applyMatrix(Glm.inverse(cameraMatrix));

			lightProjMatBinder.setValue(lightProjStack.top());

			Vec4 worldLightPos = lightView.getColumn(3);
			Vec3 lightPos = new Vec3(Mat4.mul(cameraMatrix, worldLightPos));

			camLightPosBinder.setValue(lightPos);
		}
				
		glViewport(0, 0, displayWidth, displayHeight);
		scene.render(modelMatrix.top());

		{
			// Draw axes
			modelMatrix.push();
			
			modelMatrix.applyMatrix(lightView);
			modelMatrix.scale(15.0f);

			glUseProgram(coloredProg);
			glUniformMatrix4(coloredModelToCameraMatrixUnif, false,
				modelMatrix.top().fillAndFlipBuffer(mat4Buffer));
			axesMesh.render();
	
			modelMatrix.pop();
		}
		
		if (drawCameraPos) {
			modelMatrix.push();

			// Draw lookat point.
			modelMatrix.setIdentity();
			modelMatrix.translate(0.0f, 0.0f, -viewPole.getView().radius);
			modelMatrix.scale(0.5f);

			glDisable(GL_DEPTH_TEST);
			glDepthMask(false);
			glUseProgram(unlitProg);
			glUniformMatrix4(unlitModelToCameraMatrixUnif, false,
					modelMatrix.top().fillAndFlipBuffer(mat4Buffer));
			glUniform4f(unlitObjectColorUnif, 0.25f, 0.25f, 0.25f, 1.0f);
			sphereMesh.render("flat");
			glDepthMask(true);
			glEnable(GL_DEPTH_TEST);
			glUniform4f(unlitObjectColorUnif, 1.0f, 1.0f, 1.0f, 1.0f);
			sphereMesh.render("flat");
			
			modelMatrix.pop();
		}
		
		glActiveTexture(GL_TEXTURE0 + lightProjTexUnit);
		glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
		glBindSampler(lightProjTexUnit, 0);
	}
	
	
	@Override
	protected void reshape(int width, int height) {
		displayWidth = width;
		displayHeight = height;
	}
	
	
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
	 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
	 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
		
	private static int displayWidth	= 500;
	private static int displayHeight = 500;

	private final float zNear = 1.0f;
	private final float zFar = 1000.0f;

	private final FloatBuffer mat4Buffer		= BufferUtils.createFloatBuffer(Mat4.SIZE);
	private final FloatBuffer lightBlockBuffer	= BufferUtils.createFloatBuffer(LightBlock.SIZE);

	
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
	 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

	private int coloredModelToCameraMatrixUnif;
	private int coloredProg;
	
	private int unlitProg;
	private int unlitModelToCameraMatrixUnif;
	private int unlitObjectColorUnif;
		
	
	private void loadAndSetupScene() {
		scene = new Scene("projCube_scene.xml");

		nodes = new ArrayList<>();
		nodes.add(scene.findNode("cube"));
		nodes.add(scene.findNode("rightBar"));
		nodes.add(scene.findNode("leaningBar"));
		nodes.add(scene.findNode("spinBar"));
		nodes.add(scene.findNode("diorama"));
		nodes.add(scene.findNode("floor"));
		
		lightNumBinder = new UniformIntBinder();
		SceneBinders.associateUniformWithNodes(nodes, lightNumBinder, "numberOfLights");
		SceneBinders.setStateBinderWithNodes(nodes, lightNumBinder);

		lightProjMatBinder = new UniformMat4Binder();
		SceneBinders.associateUniformWithNodes(nodes, lightProjMatBinder, "cameraToLightProjMatrix");
		SceneBinders.setStateBinderWithNodes(nodes, lightProjMatBinder);
		
		camLightPosBinder = new UniformVec3Binder();
		SceneBinders.associateUniformWithNodes(nodes, camLightPosBinder, "cameraSpaceProjLightPos");
		SceneBinders.setStateBinderWithNodes(nodes, camLightPosBinder);	
		
		int unlit = scene.findProgram("p_unlit");
		sphereMesh = scene.findMesh("m_sphere");

		int colored = scene.findProgram("p_colored");
		axesMesh = scene.findMesh("m_axes");
		
		// No more things that can throw.
		spinBarOrient = nodes.get(3).nodeGetOrient();
		unlitProg = unlit;
		unlitModelToCameraMatrixUnif = glGetUniformLocation(unlit, "modelToCameraMatrix");
		unlitObjectColorUnif = glGetUniformLocation(unlit, "objectColor");

		coloredProg = colored;
		coloredModelToCameraMatrixUnif = glGetUniformLocation(colored, "modelToCameraMatrix");	
	}
	
	
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
	 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	
	private Scene scene;
	private ArrayList<SceneNode> nodes;
	private Timer timer = new Timer(Timer.Type.LOOP, 10.0f);

	private UniformMat4Binder lightProjMatBinder;
	private UniformVec3Binder camLightPosBinder;
	
	private Quaternion spinBarOrient;
	
	private boolean showOtherLights = true;

	private Mesh sphereMesh, axesMesh;
	private boolean drawCameraPos;
		
	
	////////////////////////////////
	// View setup.
	private ViewData initialView = new ViewData(
			new Vec3(0.0f, 0.0f, 10.0f),
			new Quaternion(0.909845f, 0.16043f, -0.376867f, -0.0664516f),
			25.0f, 
			0.0f);
	
	private ViewScale initialViewScale = new ViewScale(
			5.0f, 70.0f,
			2.0f, 0.5f,
			2.0f, 0.5f,
			90.0f / 250.0f);
	
	
	private ObjectData initLightData = new ObjectData(
			new Vec3(0.0f, 0.0f, 10.0f),
			new Quaternion(1.0f, 0.0f, 0.0f, 0.0f)
	);
	
	
	private ViewPole viewPole 		= new ViewPole(initialView, initialViewScale, MouseButtons.MB_LEFT_BTN);
	private ObjectPole lightPole 	= new ObjectPole(initLightData, 90.0f/250.0f, MouseButtons.MB_RIGHT_BTN, viewPole);

	
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
	 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

	private final TexDef[] texDefs = { 
			new TexDef("IrregularPoint.dds", 	"Irregular Point Light"),
			new TexDef("Planetarium.dds", 		"Planetarium")};
	private final int NUM_LIGHT_TEXTURES = texDefs.length;

	private int[] lightTextures = new int[texDefs.length];
	private int currTextureIndex = 0;
	
	
	private class TexDef {
		String filename;
		String name;
		
		TexDef(String filename, String name) {
			this.filename = filename;
			this.name = name;
		}
	}
	
	
	private void loadTextures() {
		try {
			for (int textureIndex = 0; textureIndex < NUM_LIGHT_TEXTURES; textureIndex++) {
				lightTextures[textureIndex] = glGenTextures();
				
				String filepath = Framework.findFileOrThrow(texDefs[textureIndex].filename);
				ImageSet imageSet = DdsLoader.loadFromFile(filepath);
				
				glBindTexture(GL_TEXTURE_CUBE_MAP, lightTextures[textureIndex]);
				glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_BASE_LEVEL, 0);
				glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAX_LEVEL, 0);

				Dimensions imageDimensions = imageSet.getDimensions();
				int imageFormat = TextureGenerator.getInternalFormat(imageSet.getFormat(), 0);
				
				for (int face = 0; face < 6; ++face) {
					SingleImage singleImage = imageSet.getImage(0, 0, face);

					glCompressedTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + face, 
							0, imageFormat, imageDimensions.width, imageDimensions.height, 
							0, singleImage.getImageData());
				}
				
				glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
		
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
	 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	
	private final int NUM_SAMPLERS = 1;

	private int[] samplers = new int[NUM_SAMPLERS];
	private int currSampler = 0;
	
	
	private void createSamplers() {
		for (int samplerIndex = 0; samplerIndex < NUM_SAMPLERS; samplerIndex++) {
			samplers[samplerIndex] = glGenSamplers();
			glSamplerParameteri(samplers[samplerIndex], GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glSamplerParameteri(samplers[samplerIndex], GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		}

		glSamplerParameteri(samplers[0], GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glSamplerParameteri(samplers[0], GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glSamplerParameteri(samplers[0], GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
	}

	
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
	 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	
	private final int projectionBlockIndex = 0;

	private int projectionUniformBuffer;

		
	private class ProjectionBlock extends BufferableData<FloatBuffer> {
		Mat4 cameraToClipMatrix;
		
		static final int SIZE = Mat4.SIZE;
		
		@Override
		public FloatBuffer fillBuffer(FloatBuffer buffer) {
			return cameraToClipMatrix.fillBuffer(buffer);
		}
	}
		
	
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
	 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	
	private static final int MAX_NUMBER_OF_LIGHTS = 4;

	private final int lightBlockIndex = 1;
	private final int lightProjTexUnit = 3;

	private int lightUniformBuffer;
	private UniformIntBinder lightNumBinder;


	private class PerLight extends BufferableData<FloatBuffer> {
		Vec4 cameraSpaceLightPos;
		Vec4 lightIntensity;
		
		static final int SIZE = Vec4.SIZE + Vec4.SIZE;

		@Override
		public FloatBuffer fillBuffer(FloatBuffer buffer) {
			cameraSpaceLightPos.fillBuffer(buffer);
			lightIntensity.fillBuffer(buffer);

			return buffer;
		}
	}
	
	
	private class LightBlock extends BufferableData<FloatBuffer> {
		Vec4 ambientIntensity;
		float lightAttenuation;
		float maxIntensity;
		float padding[] = new float[2];
		PerLight lights[] = new PerLight[MAX_NUMBER_OF_LIGHTS];

		static final int SIZE = Vec4.SIZE + ((1 + 1 + 2) * FLOAT_SIZE) + PerLight.SIZE * MAX_NUMBER_OF_LIGHTS;

		@Override
		public FloatBuffer fillBuffer(FloatBuffer buffer) {			
			ambientIntensity.fillBuffer(buffer);
			buffer.put(lightAttenuation);
			buffer.put(maxIntensity);
			buffer.put(padding);
			
			for (PerLight light : lights) {
				if (light == null)
					break;
				
				light.fillBuffer(buffer);
			}
			
			return buffer;
		}
	}

	
	private void buildLights(Mat4 camMatrix) {
		LightBlock lightData = new LightBlock();
		lightData.ambientIntensity = new Vec4(0.2f, 0.2f, 0.2f, 1.0f);
		lightData.lightAttenuation = 1.0f / (30.0f * 30.0f);
		lightData.maxIntensity = 2.0f;
		
		lightData.lights[0] = new PerLight();
		lightData.lights[0].lightIntensity = new Vec4(0.2f, 0.2f, 0.2f, 1.0f);
		lightData.lights[0].cameraSpaceLightPos = Mat4.mul(camMatrix, 
				Glm.normalize(new Vec4(-0.2f, 0.5f, 0.5f, 0.0f)));
		
		lightData.lights[1] = new PerLight();
		lightData.lights[1].lightIntensity = new Vec4(3.5f, 6.5f, 3.0f, 1.0f).scale(0.5f);
		lightData.lights[1].cameraSpaceLightPos = Mat4.mul(camMatrix, 
				new Vec4(5.0f, 6.0f, 0.5f, 1.0f));

		if (showOtherLights) {
			lightNumBinder.setValue(2);
		}
		else {
			lightNumBinder.setValue(0);
		}

		glBindBuffer(GL_UNIFORM_BUFFER, lightUniformBuffer);
		glBufferData(GL_UNIFORM_BUFFER, lightData.fillAndFlipBuffer(lightBlockBuffer), GL_STREAM_DRAW);
	}
}