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
package io.github.opencubicchunks.cubicchunks.core.util;

import io.github.opencubicchunks.cubicchunks.api.core.CubePrimer;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import java.util.function.Predicate;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class StructureGenUtil {

    public static boolean scanWallsForBlock(CubePrimer cube,
            StructureBoundingBox boundingBox,
            Predicate<IBlockState> predicate) {
        int minX = boundingBox.minX;
        int minY = boundingBox.minY;
        int minZ = boundingBox.minZ;
        int maxX = boundingBox.maxX;
        int maxY = boundingBox.maxY;
        int maxZ = boundingBox.maxZ;
        // xy planes
        for (int x = minX; x < maxX; ++x) {
            for (int y = minY; y < maxY; ++y) {
                if (predicate.test(cube.getBlockState(x, y, minZ)) ||
                        predicate.test(cube.getBlockState(x, y, maxZ - 1))) {
                    return true;
                }
            }
        }

        // xz planes
        for (int x = minX; x < maxX; ++x) {
            for (int z = minZ; z < maxZ; ++z) {
                if (predicate.test(cube.getBlockState(x, minY, z)) ||
                        predicate.test(cube.getBlockState(x, maxY - 1, z))) {
                    return true;
                }
            }
        }

        // yz planes
        for (int y = minY; y < maxY; ++y) {
            for (int z = minZ; z < maxZ; ++z) {
                if (predicate.test(cube.getBlockState(minX, y, z)) ||
                        predicate.test(cube.getBlockState(maxX - 1, y, z))) {
                    return true;
                }
            }
        }

        return false;
    }

    //Note: it can return negative value. it's not a real distance
    public static double normalizedDistance(int cubeOriginCoord, int localCoord, double structureCoord, double scale) {
        return (Coords.localToBlock(cubeOriginCoord, localCoord) + 0.5D - structureCoord) / scale;
    }

    /**
     * Modifies boundingBox so that max coordinates are less than or equal to {@link Cube#SIZE}
     * and min coords are greater than or equal to 0
     */
    public static void clampBoundingBoxToLocalCube(StructureBoundingBox boundingBox) {
        if (boundingBox.minX < 0) {
            boundingBox.minX = 0;
        }
        if (boundingBox.maxX > Cube.SIZE) {
            boundingBox.maxX = Cube.SIZE;
        }
        if (boundingBox.minY < 0) {
            boundingBox.minY = 0;
        }
        if (boundingBox.maxY > Cube.SIZE) {
            boundingBox.maxY = Cube.SIZE;
        }
        if (boundingBox.minZ < 0) {
            boundingBox.minZ = 0;
        }
        if (boundingBox.maxZ > Cube.SIZE) {
            boundingBox.maxZ = Cube.SIZE;
        }
    }
}