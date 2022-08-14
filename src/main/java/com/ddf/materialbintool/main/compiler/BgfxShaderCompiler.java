package com.ddf.materialbintool.main.compiler;

import com.ddf.materialbintool.main.util.FileUtil;
import com.ddf.materialbintool.materials.ShaderCodePlatform;
import com.ddf.materialbintool.materials.definition.ShaderStage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BgfxShaderCompiler {
    private final Random random = new Random();
    private final String compilerPath;
    private final List<String> includePaths = new ArrayList<>();
    private final File tempDir;

    public BgfxShaderCompiler(String compilerPath) {
        this.compilerPath = compilerPath;
        this.tempDir = createTempDir();
    }

    public void addIncludePath(String includePath) {
        includePaths.add(includePath);
    }

    public byte[] compile(File input, File varyingDef, Defines defines, ShaderCodePlatform platform, ShaderStage type) {
        File tempOutputFile = new File(tempDir, System.nanoTime() + Integer.toHexString(random.nextInt()));
        int code = compile(input, varyingDef, tempOutputFile, defines, platform, type);
        if (code == 0) {
            byte[] output = tempOutputFile.exists() ? FileUtil.readAllBytes(tempOutputFile) : null;
            FileUtil.delete(tempOutputFile);
            return output;
        } else {
            return null;
        }
    }

    public int compile(File input, File varyingDef, File output, Defines defines, ShaderCodePlatform platform, ShaderStage stage) {
        List<String> command = new ArrayList<>();
        command.add(compilerPath);

        command.add("-f");
        command.add(input.getAbsolutePath());

        command.add("-o");
        command.add(output.getPath());

        command.add("--varyingdef");
        command.add(varyingDef.getAbsolutePath());

        command.add("--define");
        command.add(defines.toString());

        command.add("--platform");
        command.add(toPlatformString(platform));

        command.add("--type");
        command.add(toTypeString(stage));

        command.add("--profile");
        command.add(toProfileString(platform, stage));

        for (String includePath : includePaths) {
            command.add("-i");
            command.add(includePath);
        }

        command.add("-O 3");

        try {
            Process process = new ProcessBuilder()
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .command(command)
                    .start();
            return process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private static File createTempDir() {
        try {
            File dir = Files.createTempDirectory("materialbintool").toFile();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> FileUtil.delete(dir)));
            return dir;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String toPlatformString(ShaderCodePlatform shaderCodePlatform) {
        switch (shaderCodePlatform) {
            case ESSL_100:
            case ESSL_300:
            case ESSL_310:
                return "android";
            case Direct3D_SM20_Level_9_1:
            case Direct3D_SM20_Level_9_2:
            case Direct3D_SM20_Level_9_3:
            case Direct3D_SM30:
            case Direct3D_SM40:
            case Direct3D_SM50:
            case Direct3D_SM60:
                return "windows";
            case Metal:
                return "osx";
            case Vulkan:
                return "linux";
            default:
                return "";
        }
    }

    private static String toTypeString(ShaderStage shaderStage) {
        switch (shaderStage) {
            case Vertex:
                return "vertex";
            case Fragment:
                return "fragment";
            case Compute:
                return "compute";
            case Unknown:
                return "fragment";
            default:
                return "";
        }
    }

    private static String toProfileString(ShaderCodePlatform platform, ShaderStage stage) {
        String prefix = "";
        switch (stage) {
            case Vertex:
                prefix = "v";
                break;
            case Fragment:
                prefix = "p";
                break;
            case Compute:
                prefix = "c";
                break;
            case Unknown:
                prefix = "p";
                break;
        }

        switch (platform) {
            case Direct3D_SM20_Level_9_1:
                return prefix + "s_4_0_level_9_1";
            case Direct3D_SM20_Level_9_2:
                return prefix + "s_4_0_level_9_2";
            case Direct3D_SM20_Level_9_3:
                return prefix + "s_4_0_level_9_3";
            case Direct3D_SM30:
                return prefix + "s_3_0";
            case Direct3D_SM40:
                return prefix + "s_4_0";
            case Direct3D_SM50:
            case Direct3D_SM60:
                return prefix + "s_5_0";
            case GLSL_120:
                return "120";
            case GLSL_430:
                return "430";
            case ESSL_100:
            case ESSL_300:
            case ESSL_310:
                return "";
            case Metal:
                return "metal";
            case Vulkan:
                return "spirv";
            default:
                return "";
        }
    }
}
