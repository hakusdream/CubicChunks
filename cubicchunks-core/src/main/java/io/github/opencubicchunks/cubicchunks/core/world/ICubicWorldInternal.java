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
package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.api.core.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.core.ICubicWorldClient;
import io.github.opencubicchunks.cubicchunks.api.core.ICubicWorldServer;
import io.github.opencubicchunks.cubicchunks.core.util.IntRange;
import mcp.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ICubicWorldInternal extends ICubicWorld {
    /**
     * Updates the world
     */
    void tickCubicWorld();

    public interface Server extends ICubicWorldInternal, ICubicWorldServer {

        /**
         * Initializes the world to be a CubicChunks world. Must be done before any players are online and before any chunks
         * are loaded. Cannot be used more than once.
         * @param heightRange
         * @param generationRange
         */
        void initCubicWorldServer(IntRange heightRange, IntRange generationRange);

    }

    public interface Client extends ICubicWorldInternal, ICubicWorldClient {

        /**
         * Initializes the world to be a CubicChunks world. Must be done before any players are online and before any chunks
         * are loaded. Cannot be used more than once.
         * @param heightRange
         * @param generationRange
         */
        void initCubicWorldClient(IntRange heightRange, IntRange generationRange);

    }
}
