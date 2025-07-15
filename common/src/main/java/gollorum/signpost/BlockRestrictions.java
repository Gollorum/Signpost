package gollorum.signpost;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.minecraft.config.IConfig;
import gollorum.signpost.minecraft.utils.ClientFrameworkAdapter;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.security.WithOwner;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

// Server only.
public class BlockRestrictions extends SavedData {

	public static final Codec<BlockRestrictions> CODEC = Codec.unboundedMap(
		PlayerHandle.CODEC,
		Entry.CODEC
	).xmap(BlockRestrictions::new, br -> br.values);

	public static final SavedDataType<BlockRestrictions> TYPE = new SavedDataType<>(
		Signpost.MOD_ID + "_BlockRestrictions",
		BlockRestrictions::new,
		CODEC,
		DataFixTypes.SAVED_DATA_MAP_DATA
	);

	public enum Type {
		Waystone(
			() -> IConfig.IServer.getInstance().permissions().defaultMaxWaystonesPerPlayer(),
			(e, i) -> e.waystonesLeft = i,
			e -> e.waystonesLeft,
			LangKeys.noMoreWaystones, LangKeys.waystonesLeft, LangKeys.unlimitedWaystones,
			LangKeys.waystonesLeftOther, LangKeys.unlimitedWaystonesOther,
			o -> o instanceof WithOwner.OfWaystone ? ((WithOwner.OfWaystone)o).getWaystoneOwner() : Optional.empty()
		),
		Signpost(
			() -> IConfig.IServer.getInstance().permissions().defaultMaxSignpostsPerPlayer(),
			(e, i) -> e.signpostsLeft = i,
			e -> e.signpostsLeft,
			LangKeys.noMoreSignposts, LangKeys.signpostsLeft, LangKeys.unlimitedSignposts,
			LangKeys.signpostsLeftOther, LangKeys.unlimitedSignpostsOther,
			o -> o instanceof WithOwner.OfSignpost ? ((WithOwner.OfSignpost)o).getSignpostOwner() : Optional.empty()
		);

		final Supplier<Integer> getOverridePermissionLevel;
		final BiConsumer<Entry, Integer> setCount;
		final Function<Entry, Integer> getCount;
		public final String errorLangKey;
		public final String remainingLangKey;
		public final String unlimitedRemainingLangKey;
		public final String remainingLangKeyOther;
		public final String unlimitedRemainingLangKeyOther;
		public final Function<Object, Optional<PlayerHandle>> tryGetOwner;

		public Component getRemainingTextComponent(int count, Optional<Component> subject) {
			return subject.map(s -> Component.translatable(remainingLangKeyOther, count, s))
				.orElseGet(() -> Component.translatable(remainingLangKey, count));
		}

		public Component getUnlimitedRemainingTextComponent(Optional<Component> subject) {
			return subject.map(s -> Component.translatable(unlimitedRemainingLangKeyOther, s))
				.orElseGet(() -> Component.translatable(unlimitedRemainingLangKey));
		}

		Type(
			Supplier<Integer> overridePermissionLevelSupplier,
			BiConsumer<Entry, Integer> setCount,
			Function<Entry, Integer> getCount,
			String errorLangKey,
			String remainingLangKey,
			String unlimitedRemainingLangKey,
			String remainingLangKeyOther,
			String unlimitedRemainingLangKeyOther,
			Function<Object, Optional<PlayerHandle>> tryGetOwner
		) {
			this.getOverridePermissionLevel = overridePermissionLevelSupplier;
			this.setCount = setCount;
			this.getCount = getCount;
			this.errorLangKey = errorLangKey;
			this.remainingLangKey = remainingLangKey;
			this.unlimitedRemainingLangKey = unlimitedRemainingLangKey;
			this.remainingLangKeyOther = remainingLangKeyOther;
			this.unlimitedRemainingLangKeyOther = unlimitedRemainingLangKeyOther;
			this.tryGetOwner = tryGetOwner;
		}
	}

	public static BlockRestrictions getInstance() {
		assert Signpost.getServerType().isServer;
		return Signpost.getServerInstance().overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	private static class Entry {
		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("remaining_waystones").forGetter(e -> e.waystonesLeft),
			Codec.INT.fieldOf("remaining_signposts").forGetter(e -> e.signpostsLeft)
		).apply(i, Entry::new));

		public int waystonesLeft;
		public int signpostsLeft;
		public Entry(int waystonesLeft, int signpostsLeft) {
			this.waystonesLeft = waystonesLeft;
			this.signpostsLeft = signpostsLeft;
		}
		public static Entry forNewUser(PlayerHandle player) {
			return player.equals(PlayerHandle.Invalid)
				? new Entry(-1, -1)
				: new Entry(
				IConfig.IServer.getInstance().permissions().defaultMaxWaystonesPerPlayer(),
				IConfig.IServer.getInstance().permissions().defaultMaxSignpostsPerPlayer()
			);
		}
	}

	private final Map<PlayerHandle, Entry> values;

	private BlockRestrictions() {
		this.values = new HashMap<>();
	}

	private BlockRestrictions(Map<PlayerHandle, Entry> values) {
		this.values = values;
	}

	private Entry getEntry(PlayerHandle player) {
		return values.computeIfAbsent(player, Entry::forNewUser);
	}

	public boolean setRemaining(Type type, PlayerHandle player, Function<Integer, Integer> count) {
		if(player.equals(PlayerHandle.Invalid)) return true;
		Entry entry = getEntry(player);
		int oldCount;
		int newCount;
		switch (type) {
			case Signpost:
				oldCount = entry.signpostsLeft;
				newCount = entry.signpostsLeft = count.apply(oldCount);
				break;
			case Waystone:
				oldCount = entry.waystonesLeft;
				newCount = entry.waystonesLeft = count.apply(oldCount);
				break;
			default: throw new IllegalArgumentException();
		}
		if(oldCount != newCount) {
			setDirty();
			return true;
		} else return false;
	}

	public void incrementRemaining(Type type, PlayerHandle player) {
		if(player.equals(PlayerHandle.Invalid)) return;
		Entry entry = getEntry(player);
		int prevCount = type.getCount.apply(entry);
		if(prevCount >= 0) {
			type.setCount.accept(entry, prevCount + 1);
			setDirty();
			PacketHandler.getInstance().sendToPlayer(
                player.asEntity(),
				new NotifyCountChanged.Package(type.remainingLangKey, prevCount + 1, type == Type.Waystone)
			);
		}
	}

	public boolean tryDecrementRemaining(Type type, PlayerHandle player) {
		if(player.equals(PlayerHandle.Invalid)) return true;
		Entry entry = getEntry(player);
		int prevCount = type.getCount.apply(entry);
		if(prevCount >= 1) {
			type.setCount.accept(entry, prevCount - 1);
			setDirty();
			PacketHandler.getInstance().sendToPlayer(
                player.asEntity(),
				new NotifyCountChanged.Package(type.remainingLangKey, prevCount - 1, type == Type.Waystone)
			);
			return true;
		} else {
			if(prevCount == 0)
				player.asEntity().sendSystemMessage(Component.translatable(type.errorLangKey));
			return prevCount < 0;
		}
	}

	public int getRemaining(Type type, PlayerHandle player) {
		if(player.equals(PlayerHandle.Invalid)) return -1;
		Entry entry = getEntry(player);
		return switch (type) {
			case Waystone -> entry.waystonesLeft;
			case Signpost -> entry.signpostsLeft;
		};
	}

	public static final class NotifyCountChanged implements PacketHandler.Event.ForClient<NotifyCountChanged.Package> {

		public static final record Package(String langKey, Integer count, boolean isWaystoneNotification) {
			private static final StreamCodec<RegistryFriendlyByteBuf, Package> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, Package::langKey,
				ByteBufCodecs.INT, Package::count,
				ByteBufCodecs.BOOL, Package::isWaystoneNotification,
				Package::new
			);
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, Package> codec() {
			return Package.STREAM_CODEC;
		}

		@Override
		public Class<Package> getMessageClass() {
			return Package.class;
		}

        @Override
        public void handle(Package message, PacketHandler.Context.Client context) {
            if((message.isWaystoneNotification
                ? IConfig.IClient.getInstance().enableWaystoneLimitNotifications()
                : IConfig.IClient.getInstance().enableSignpostLimitNotifications()
            )) ClientFrameworkAdapter.showStatusMessage(Component.translatable(message.langKey, message.count), true);
        }
	}

}
