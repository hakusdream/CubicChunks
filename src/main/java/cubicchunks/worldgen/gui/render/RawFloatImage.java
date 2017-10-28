/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks.worldgen.gui.render;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LUMINANCE;
import static org.lwjgl.opengl.GL11.GL_RED;
import static org.lwjgl.opengl.GL11.GL_RGB;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LOD;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_MIN_LOD;
import static org.lwjgl.opengl.GL14.GL_TEXTURE_LOD_BIAS;
import static org.lwjgl.opengl.GL30.GL_R16F;
import static org.lwjgl.opengl.GL30.GL_RG;
import static org.lwjgl.opengl.GL30.GL_RG16F;
import static org.lwjgl.opengl.GL30.GL_RGB16F;
import static org.lwjgl.opengl.GL30.GL_RGBA16F;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class RawFloatImage implements ITextureObject {

    private final int width, height;
    private int texture;
    private final FloatBuffer data;
    private int channels;

    public RawFloatImage(float[][] imageData, int channels) {
        if (channels > 4 || channels < 1) {
            throw new IllegalArgumentException("Channel count must be between 1 and 4 but was " + channels);
        }
        this.channels = channels;
        FloatBuffer buffer = BufferUtils.createFloatBuffer(imageData.length * imageData[0].length);
        for (float[] subArr : imageData) {
            buffer.put(subArr);
        }
        buffer.flip();

        this.data = buffer;
        this.width = imageData[0].length / channels;
        this.height = imageData.length;
    }

    public void delete() {
        if (texture > 0) {
            TextureUtil.deleteTexture(texture);
        }
    }

    @Override public void setBlurMipmap(boolean blurIn, boolean mipmapIn) {
        // no-op
    }

    @Override public void restoreLastBlurMipmap() {
        // no-op
    }

    @Override public void loadTexture(IResourceManager resourceManager) {
        delete();
        int texture = TextureUtil.glGenTextures();
        GlStateManager.bindTexture(texture);
        GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);
        GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_LOD, 0);
        GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LOD, 0);
        GlStateManager.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, 0.0F);

        int internalFmt;
        int fmt;
        switch (channels) {
            case 1:
                internalFmt = GL_R16F;
                fmt = GL_RED;
                break;
            case 2:
                internalFmt = GL_RG16F;
                fmt = GL_RG;
                break;
            case 3:
                internalFmt = GL_RGB16F;
                fmt = GL_RGB;
                break;
            case 4:
                internalFmt = GL_RGBA16F;
                fmt = GL_RGBA;
                break;
            default:
                throw new Error();
        }
        glTexImage2D(GL_TEXTURE_2D, 0, internalFmt, width, height, 0, fmt, GL_FLOAT, this.data);
        this.texture = texture;
    }

    @Override public int getGlTextureId() {
        return texture;
    }
}
