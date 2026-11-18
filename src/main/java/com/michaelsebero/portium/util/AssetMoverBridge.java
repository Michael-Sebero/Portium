package com.michaelsebero.portium.util;

import com.cleanroommc.assetmover.api.AssetMoverAPI;
import com.cleanroommc.assetmover.api.MoveAssetEvent;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;

/**
 * The one place Portium asks AssetMover 3 to fetch files before the loading screen's resource stage, which is when
 * AssetMover runs the requests mods make through {@code MoveAssetEvent}. Portium's mod layers need their jars during
 * mod construction, long before that, so the requests are run now, on a mover of their own: the same downloads, the same
 * checksums, the same parallelism and loading-screen bar as any other request, and none of Portium's code touches the
 * network.
 * <p>
 * AssetMover has no public way to run requests at once ({@code AssetMover.move} is package-private), so it is called
 * reflectively; if AssetMover changes it, {@link #fetch} fails with a message saying so. The event posted is Portium's
 * own, so no other mod's {@code MoveAssetEvent} listener or AssetMover's own bookkeeping sees it. Kept apart from
 * {@link RuntimeFiles} so that class loads without AssetMover when nothing is missing.
 */
final class AssetMoverBridge {

    /** The event the run posts: Portium's own (AssetMover's {@code Client} and {@code Server} are for the resource stage). */
    private static final class Fetch extends MoveAssetEvent {

        Fetch(AssetMoverAPI assetMover) {
            super(assetMover);
        }
    }

    private AssetMoverBridge() {}

    /**
     * Fetches files and waits for them. Each request is {@code {url, destination below AssetMover's folder, SHA-1}}.
     * A download that fails or doesn't match its checksum is logged by AssetMover and leaves no file, which the caller sees.
     */
    static void fetch(List<String[]> requests) throws IOException {
        Function<AssetMoverAPI, MoveAssetEvent> queue = mover -> {
            for (String[] request : requests) mover.fromUrlFile(request[0], request[1]).overwrite().verify("SHA-1", request[2]);
            return new Fetch(mover);
        };
        try {
            Class<?> assetMover = Class.forName("com.cleanroommc.assetmover.AssetMover");
            Method move = assetMover.getDeclaredMethod("move", Function.class);
            move.setAccessible(true);
            move.invoke(null, queue);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            throw new IOException("AssetMover 3 isn't installed, so Portium's runtime files can't be fetched. Install AssetMover 3.0.1 or newer", e);
        } catch (NoSuchMethodException | IllegalAccessException | RuntimeException e) {
            throw new IOException("This AssetMover isn't one Portium knows how to ask for files (expected 3.0.1)", e);
        } catch (InvocationTargetException e) {
            throw new IOException("AssetMover failed while fetching Portium's runtime files", e.getCause());
        }
    }
}
