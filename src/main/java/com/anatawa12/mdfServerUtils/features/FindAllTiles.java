package com.anatawa12.mdfServerUtils.features;

import com.anatawa12.mdfServerUtils.util.LruCacheMap;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.DimensionManager;

import java.lang.ref.SoftReference;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class FindAllTiles {
    private FindAllTiles() {
    }

    public static void processCommand(ICommandSender sender, String[] args) {
        // parse regex first
        MatchContext match = compileAndGetContext(args[1]);

        Stream<Chunk> chunks;
        int sx;
        int sy;
        int sz;
        int ex;
        int ey;
        int ez;
        if (args.length == 1 + 1) {
            chunks = Arrays.stream(DimensionManager.getWorlds())
                    .flatMap(worldServer -> getChunksFromWorldServer(worldServer, sender));
            sx = Integer.MIN_VALUE;
            sy = Integer.MIN_VALUE;
            sz = Integer.MIN_VALUE;
            ex = Integer.MAX_VALUE;
            ey = Integer.MAX_VALUE;
            ez = Integer.MAX_VALUE;
        } else if (args.length == 1 + 1 + 6) {
            World world = sender.getEntityWorld();
            sx = CommandBase.parseInt(sender, args[2]);
            sy = CommandBase.parseInt(sender, args[3]);
            sz = CommandBase.parseInt(sender, args[4]);
            ex = CommandBase.parseInt(sender, args[5]);
            ey = CommandBase.parseInt(sender, args[6]);
            ez = CommandBase.parseInt(sender, args[7]);
            int scx = sx >> 4;
            int scz = sz >> 4;
            int ecx = ex >> 4 + 1;
            int ecz = ez >> 4 + 1;
            chunks = IntStream.rangeClosed(scx, ecx)
                    .mapToObj(x -> IntStream.rangeClosed(scz, ecz).mapToObj(z -> new ChunkCoordIntPair(x, z)))
                    .flatMap(Function.identity())
                    .map(coord -> world.getChunkFromChunkCoords(coord.chunkXPos, coord.chunkZPos));
        } else {
            throw new WrongUsageException("");
        }

        try (Stream<Chunk> chunks_ = chunks) {
            int printed = 0;
            chunks:
            for (Chunk chunk : (Iterable<? extends Chunk>) chunks::iterator) {
                @SuppressWarnings("unchecked")
                Map<ChunkPosition, TileEntity> tileEntityMap = chunk.chunkTileEntityMap;
                for (TileEntity tile : tileEntityMap.values()) {
                    if (tile == null) continue;
                    if (sx > tile.xCoord || tile.xCoord > ex) continue;
                    if (sx > tile.yCoord || tile.yCoord > ey) continue;
                    if (sx > tile.zCoord || tile.zCoord > ez) continue;

                    if (match.match(tile.getClass())) {
                        printed++;
                        String name = tile.getClass().getSimpleName();
                        sender.addChatMessage(new ChatComponentText("found " + name
                                + " at " + tile.xCoord + " " + tile.yCoord + " " + tile.zCoord));

                        if (printed >= MAX_PRINT_COUNT) {
                            sender.addChatMessage(new ChatComponentText("and more"));
                            break chunks;
                        }
                    }
                }
            }
            if (printed == 0) {
                sender.addChatMessage(new ChatComponentText("nothing found"));
            }
        }
    }

    private static final int MAX_PRINT_COUNT = 16;

    private static Stream<Chunk> getChunksFromWorldServer(WorldServer server, ICommandSender logTo) {
        IChunkProvider provider = server.getChunkProvider();
        if (!(provider instanceof ChunkProviderServer)) {
            if (logTo != null)
                logTo.addChatMessage(new ChatComponentText("the dimension #" + server.provider.dimensionId
                        + "cannot be traced")
                        .setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
            return Stream.empty();
        }

        @SuppressWarnings("unchecked")
        List<Chunk> loadedChunks = ((ChunkProviderServer) provider).loadedChunks;
        return loadedChunks.stream();
    }

    private static final int CACHE_SIZE = 32;
    private static final Supplier<LruCacheMap<String, MatchContext>> creator = () -> new LruCacheMap<>(CACHE_SIZE);
    private static SoftReference<LruCacheMap<String, MatchContext>> cache = new SoftReference<>(creator.get());

    private static MatchContext compileAndGetContext(String regex) {
        LruCacheMap<String, MatchContext> cacheReal = cache.get();
        MatchContext context = null;
        if (cacheReal != null) context = cacheReal.get(regex);
        if (context != null) return context;
        // parse regex first
        Pattern pattern;
        try {
            pattern = Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            throw new WrongUsageException("invalid regex: " + e.getMessage());
        }
        context = new MatchContext(pattern);
        if (cacheReal == null) {
            cacheReal = creator.get();
            cache = new SoftReference<>(cacheReal);
        }
        cacheReal.put(regex, context);

        return context;
    }

    private static class MatchContext {
        private final Pattern pattern;
        private final Set<Class<?>> matchCahce;
        private final Set<Class<?>> noMatchCahce;

        private MatchContext(Pattern pattern) {
            this.pattern = pattern;
            matchCahce = new HashSet<>();
            noMatchCahce = new HashSet<>();
        }

        public boolean match(Class<?> tileClass) {
            if (matchCahce.contains(tileClass)) return true;
            if (noMatchCahce.contains(tileClass)) return false;

            if (pattern.matcher(tileClass.getName()).matches()) {
                matchCahce.add(tileClass);
                return true;
            } else {
                Class<?> superClass = tileClass.getSuperclass();
                if (superClass != null && TileEntity.class.isAssignableFrom(superClass)) {
                    if (match(superClass)) {
                        matchCahce.add(tileClass);
                        return true;
                    }
                }
                for (Class<?> superInterface : tileClass.getInterfaces()) {
                    if (match(superInterface)) {
                        matchCahce.add(tileClass);
                        return true;
                    }
                }
                noMatchCahce.add(tileClass);
                return false;
            }
        }
    }
}
