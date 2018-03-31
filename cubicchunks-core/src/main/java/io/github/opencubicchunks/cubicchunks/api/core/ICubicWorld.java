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
package io.github.opencubicchunks.cubicchunks.api.core;

import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;
import io.github.opencubicchunks.cubicchunks.core.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.world.IMinMaxHeight;
import io.github.opencubicchunks.cubicchunks.core.world.NotCubicChunksWorldException;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;

import java.util.function.Predicate;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface ICubicWorld extends IMinMaxHeight {

    boolean isCubicWorld();

    /**
     * Returns the {@link ICubeProvider} for this world, or throws {@link NotCubicChunksWorldException}
     * if this is not a CubicChunks world.
     */
    ICubeProvider getCubeCache();

    /**
     * Returns the {@link LightingManager} for this world, or throws {@link NotCubicChunksWorldException}
     * if this is not a CubicChunks world.
     */
    LightingManager getLightingManager();

    /**
     * Returns true iff the given Predicate evaluates to true for all cubes for block positions within blockRadius from
     * centerPos. Only cubes that exist are tested. If some cubes within that range aren't loaded - returns false.
     */
    default boolean testForCubes(BlockPos centerPos, int blockRadius, Predicate<Cube> test) {
        return testForCubes(
                centerPos.getX() - blockRadius, centerPos.getY() - blockRadius, centerPos.getZ() - blockRadius,
                centerPos.getX() + blockRadius, centerPos.getY() + blockRadius, centerPos.getZ() + blockRadius,
                test
        );
    }

    /**
     * Returns true iff the given Predicate evaluates to true for all cubes for block positions between
     * BlockPos(minBlockX, minBlockY, minBlockZ) and BlockPos(maxBlockX, maxBlockY, maxBlockZ) (including the specified
     * positions). Only cubes that exist are tested. If some cubes within that range aren't loaded - returns false.
     */
    default boolean testForCubes(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ, Predicate<Cube> test) {
        return testForCubes(
                CubePos.fromBlockCoords(minBlockX, minBlockY, minBlockZ),
                CubePos.fromBlockCoords(maxBlockX, maxBlockY, maxBlockZ),
                test
        );
    }

    /**
     * Returns true iff the given Predicate evaluates to true for given cube and neighbors.
     * Only cubes that exist are tested. If some cubes within that range aren't loaded - returns false.
     */
    boolean testForCubes(CubePos start, CubePos end, Predicate<Cube> test);

    /**
     * Return the actual world height for this world. Typically this is 256 for worlds with a sky, and 128 for worlds
     * without.
     *
     * @return The actual world height
     */
    int getActualHeight();

    Cube getCubeFromCubeCoords(int cubeX, int cubeY, int cubeZ);

    Cube getCubeFromBlockCoords(BlockPos pos);

    int getEffectiveHeight(int blockX, int blockZ);

    boolean isBlockColumnLoaded(BlockPos pos);

    boolean isBlockColumnLoaded(BlockPos pos, boolean allowEmpty);

    int getMinGenerationHeight();

    int getMaxGenerationHeight();
}
