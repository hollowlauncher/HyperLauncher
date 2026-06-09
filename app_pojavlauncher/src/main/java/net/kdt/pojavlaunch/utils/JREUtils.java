package net.kdt.pojavlaunch.utils;

import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_DUMP_SHADERS;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_VSYNC_IN_ZINK;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_ZINK_PREFER_SYSTEM_DRIVER;

import android.content.Context;
import android.system.Os;
import android.util.ArrayMap;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import net.kdt.pojavlaunch.Logger;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.lifecycle.ContextExecutor;
import net.kdt.pojavlaunch.multirt.Runtime;
import net.kdt.pojavlaunch.plugins.LibraryPlugin;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JREUtils {

    public static void setupAngleEnv(Context ctx, Map<String, String> envMap) {
        LibraryPlugin angle = LibraryPlugin.discoverPlugin(ctx, LibraryPlugin.ID_ANGLE_PLUGIN);
        if (angle == null) return;
        if (!LauncherPreferences.PREF_USE_ANGLE) return;

        envMap.put("ANGLE_PLATFORM", "vulkan");
        envMap.put("ANGLE_VULKAN_IMAGE_DEFAULT_MAX_SAMPLES", "4");
        envMap.put("POJAV_ANGLE_PATH", angle.resolveAbsolutePath("libEGL_angle.so"));
    }

    public static void setupFfmpegEnv(Context ctx, Map<String, String> envMap) {
        LibraryPlugin ffmpeg = LibraryPlugin.discoverPlugin(ctx, LibraryPlugin.ID_FFMPEG_PLUGIN);
        if (ffmpeg == null) return;
        envMap.put("POJAV_FFMPEG_PATH", ffmpeg.resolveAbsolutePath("libffmpeg.so"));
    }

    public static void setupMobileGluesEnv(Context ctx, Map<String, String> envMap) {
        LibraryPlugin mobileGlues = LibraryPlugin.discoverPlugin(ctx, LibraryPlugin.ID_MOBILEGLUES_PLUGIN);
        if (mobileGlues == null) return;

        String[] mgLibs = {"libmobileglues.so", "libspirv-cross-c-shared.so"};
        if (!mobileGlues.checkLibraries(mgLibs)) {
            Log.e("MGEnvSetup", "MobileGlues plugin exists, but the libraries are not present.");
            return;
        }
        String libPath = mobileGlues.getLibraryPath();
        envMap.put("MOBILEGLUES_LIB_PATH", libPath);

        String existingLd = System.getenv("LD_LIBRARY_PATH");
        envMap.put("LD_LIBRARY_PATH", Tools.NATIVE_LIB_DIR + ":" + libPath + (existingLd != null ? ":" + existingLd : ""));

        try {

            System.loadLibrary("shaderconv");
            System.loadLibrary("spirv-cross-c-shared");
            System.load(mobileGlues.resolveAbsolutePath("libmobileglues.so"));
            Log.i("MGEnvSetup", "Successfully preloaded MobileGlues libraries");
        } catch (Throwable e) {
            Log.e("MGEnvSetup", "Failed to preload MobileGlues dependencies: " + e.getMessage());
        }
    }

    public static void setupRendererEnv(Map<String, String> envMap, String renderer) {
        switch(renderer) {
            case "vulkan_zink":
                envMap.put("GALLIUM_DRIVER", "zink");
                envMap.put("MESA_LOADER_DRIVER_OVERRIDE", "zink");

                envMap.put("MESA_GLSL_VERSION_OVERRIDE", "460");
                break;
            case "freedreno_kgsl":
                if(GLInfoUtils.getGlInfo().isAdreno()) {
                    envMap.put("MESA_LOADER_DRIVER_OVERRIDE", "kgsl");

                    if(GLInfoUtils.getGlInfo().isAdreno500Lower()) {
                        envMap.put("MESA_GL_VERSION_OVERRIDE", "3.3");
                        envMap.put("MESA_GLSL_VERSION_OVERRIDE", "330");
                    }
                }
                break;
        }
    }
    public static void setEnviroimentForGame(Context context, String renderer) throws Throwable {
        Map<String, String> envMap = new ArrayMap<>();
        envMap.put("LIBGL_MIPMAP", "3");

        envMap.put("LIBGL_NOERROR", "1");

        envMap.put("LIBGL_NOINTOVLHACK", "1");

        envMap.put("LIBGL_NORMALIZE", "1");

        if(PREF_DUMP_SHADERS)
            envMap.put("LIBGL_VGPU_DUMP", "1");
        if(PREF_VSYNC_IN_ZINK)
            envMap.put("POJAV_VSYNC_IN_ZINK", "1");

        envMap.put("LIBGL_ES", (String) ExtraCore.getValue(ExtraConstants.OPEN_GL_VERSION));

        envMap.put("FORCE_VSYNC", String.valueOf(LauncherPreferences.PREF_FORCE_VSYNC));

        envMap.put("MESA_GLSL_CACHE_DIR", Tools.DIR_CACHE.getAbsolutePath());
        envMap.put("force_glsl_extensions_warn", "true");
        envMap.put("allow_higher_compat_version", "true");
        envMap.put("allow_glsl_extension_directive_midshader", "true");

		File modRuntimeDir = new File(Tools.DIR_CACHE, "app_runtime_mod");
		if (!modRuntimeDir.exists()) {
    		modRuntimeDir.mkdirs();
		}
		envMap.put("MOD_ANDROID_RUNTIME", modRuntimeDir.getAbsolutePath());

        if(!renderer.equals("opengles2")) {
            setupAngleEnv(context, envMap);
        }
        setupFfmpegEnv(context, envMap);
        setupMobileGluesEnv(context, envMap);
        setupRendererEnv(envMap, renderer);

        String nativeDir = Tools.NATIVE_LIB_DIR;
        String mgLibPath = envMap.get("MOBILEGLUES_LIB_PATH");
        if (mgLibPath != null) {

            nativeDir = nativeDir + ":" + mgLibPath;
        }
        envMap.put("POJAV_NATIVEDIR", nativeDir);
        envMap.put("EGL_PLATFORM", "android");

        if(LauncherPreferences.PREF_BIG_CORE_AFFINITY) envMap.put("POJAV_BIG_CORE_AFFINITY", "1");

        if(GLInfoUtils.getGlInfo().isAdreno() && !PREF_ZINK_PREFER_SYSTEM_DRIVER) {
            setUseTurnip(true);
        }

        if(LauncherPreferences.PREF_FREEDRENO_SYSMEM) {

            Logger.appendToLog("Will use sysmem rendering for Turnip/Freedreno");
            envMap.put("FD_MESA_DEBUG", "sysmem");
            envMap.put("TU_DEBUG", "sysmem");
        }

        overrideEnvVars(envMap);

        for (Map.Entry<String, String> env : envMap.entrySet()) {
            Logger.appendToLog("Added custom env: " + env.getKey() + "=" + env.getValue());
            try {
                Os.setenv(env.getKey(), env.getValue(), true);
            }catch (NullPointerException exception){
                Log.e("JREUtils", exception.toString());
            }
        }
    }

    public static void launchJavaVM(final AppCompatActivity activity, final Runtime runtime, File gameDirectory, final List<String> JVMArgs, final String userArgsString) throws Throwable {
        Tools.fullyExit();
    }

    /**
     * Parse and separate java arguments in a user friendly fashion
     * It supports multi line and absence of spaces between arguments
     * The function also supports auto-removal of improper arguments, although it may miss some.
     *
     * @param args The un-parsed argument list.
     * @return Parsed args as an ArrayList
     */
    public static ArrayList<String> parseJavaArguments(String args){
        ArrayList<String> parsedArguments = new ArrayList<>(0);
        args = args.trim().replace(" ", "");

        String[] separators = new String[]{"-XX:-","-XX:+", "-XX:","--", "-D", "-X", "-javaagent:", "-verbose"};
        for(String prefix : separators){
            while (true){
                int start = args.indexOf(prefix);
                if(start == -1) break;

                int end = -1;
                for(String separator: separators){
                    int tempEnd = args.indexOf(separator, start + prefix.length());
                    if(tempEnd == -1) continue;
                    if(end == -1){
                        end = tempEnd;
                        continue;
                    }
                    end = Math.min(end, tempEnd);
                }

                if(end == -1) end = args.length();

                String parsedSubString = args.substring(start, end);
                args = args.replace(parsedSubString, "");

                if(parsedSubString.indexOf('=') == parsedSubString.lastIndexOf('=')) {
                    int arraySize = parsedArguments.size();
                    if(arraySize > 0){
                        String lastString = parsedArguments.get(arraySize - 1);

                        if(lastString.charAt(lastString.length() - 1) == ',' ||
                                parsedSubString.contains(",")){
                            parsedArguments.set(arraySize - 1, lastString + parsedSubString);
                            continue;
                        }
                    }
                    parsedArguments.add(parsedSubString);
                }
                else Log.w("JAVA ARGS PARSER", "Removed improper arguments: " + parsedSubString);
            }
        }
        return parsedArguments;
    }

    private static void overrideEnvVars(Map<String, String> envMap) {
        String customEnv = LauncherPreferences.PREF_CUSTOM_ENV_VARS;
        if (customEnv == null || customEnv.isEmpty()) return;

        for (String line : customEnv.split("\n")) {
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                envMap.put(parts[0].trim(), parts[1].trim());
            }
        }
    }

    /**
     * Open the render library in accordance to the settings.
     * It will fallback if it fails to load the library.
     * @return The name of the loaded library
     */
    public static String loadGraphicsLibrary(String renderer){
        String renderLibrary;
        boolean useGles;
        boolean bypassNamespace = false;
        boolean preloadVk = true;
        int glesVersion;
        switch (renderer){
            case "mobileglues":
                LibraryPlugin mobileGlues = LibraryPlugin.discoverPlugin(ContextExecutor.getContext(), LibraryPlugin.ID_MOBILEGLUES_PLUGIN);
                if (mobileGlues != null) {
                    renderLibrary = mobileGlues.resolveAbsolutePath("libmobileglues.so");
                    useGles = true;
                    glesVersion = 3;

                    bypassNamespace = true;

                    setLdLibraryPath(Tools.NATIVE_LIB_DIR + ":" + mobileGlues.getLibraryPath());

                    try {
                        System.loadLibrary("shaderconv");
                        System.loadLibrary("spirv-cross-c-shared");
                        System.load(renderLibrary);
                    } catch (Throwable ignored) {}

                    break;
                }

            case "freedreno_kgsl":
                preloadVk = false;
            case "vulkan_zink":
                renderLibrary = "libEGL_mesa.so";
                useGles = false;
                bypassNamespace = true;
                glesVersion = 3;
                if(preloadVk) preloadVulkan();
                break;
            case "opengles3_ltw" :
                renderLibrary = "libltw.so";
                useGles = true;
                glesVersion = 3;
                break;
            case "opengles2":
            case "opengles2_5":
            case "opengles3":
            default:
                renderLibrary = "libgl4es_114.so";
                useGles = true;
                glesVersion = Integer.parseInt((String) ExtraCore.getValue(ExtraConstants.OPEN_GL_VERSION));
                break;
        }

        if (!configureRenderspec(renderLibrary, bypassNamespace, useGles, glesVersion)) {
            Log.e("RENDER_LIBRARY","Failed to load renderer " + renderLibrary );
            return null;
        }
        return renderLibrary;
    }

    public static int getDetectedVersion() {
        return GLInfoUtils.getGlInfo().glesMajorVersion;
    }

    public static void redirectAndPrintJRELog() {

        Log.i("JREUtils", "Redirection called but handled internally");
    }

    public static native int chdir(String path);

    public static native void setLdLibraryPath(String ldLibraryPath);
    public static native boolean configureRenderspec(String eglPath, boolean useLoaderBypass, boolean useGles, int glesVersion);
    public static native void preloadVulkan();
    public static native void setUseTurnip(boolean enable);

    public static native boolean renderAWTScreenFrame(ByteBuffer tempBuffer);
    static {
        System.loadLibrary("pojavexec");
        System.loadLibrary("pojavexec_awt");
    }
}
