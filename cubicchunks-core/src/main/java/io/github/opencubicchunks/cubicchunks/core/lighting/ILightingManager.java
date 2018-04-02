package io.github.opencubicchunks.cubicchunks.core.lighting;

import io.github.opencubicchunks.cubicchunks.api.ICube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
// TODO: make this a real API
public interface ILightingManager {

    void doOnBlockSetLightUpdates(Chunk column, int localX, int oldHeight, int changeY, int localZ);

    //TODO: make it private
    void markCubeBlockColumnForUpdate(ICube cube, int blockX, int blockZ);

    void onHeightMapUpdate(Chunk column, int localX, int localZ, int oldHeight, int newHeight);
}
