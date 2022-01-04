package gollorum.signpost;

import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.storage.BlockRestrictionsStorage;
import gollorum.signpost.minecraft.utils.ClientFrameworkAdapter;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.networking.ReflectionEvent;
import gollorum.signpost.networking.SerializedWith;
import gollorum.signpost.security.WithOwner;
import gollorum.signpost.utils.Tuple;
import gollorum.signpost.utils.serialization.BooleanSerializer;
import gollorum.signpost.utils.serialization.IntSerializer;
import gollorum.signpost.utils.serialization.StringSerializer;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
import net.minecraftforge.fmllegacy.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// Server only.
public class BlockRestrictions {

	public enum Type {
		Waystone(
			() -> Config.Server.permissions.defaultMaxWaystonesPerPlayer,
			(e, i) -> e.waystonesLeft = i,
			e -> e.waystonesLeft,
			LangKeys.noMoreWaystones, LangKeys.waystonesLeft, LangKeys.unlimitedWaystones,
			LangKeys.waystonesLeftOther, LangKeys.unlimitedWaystonesOther,
			o -> o instanceof WithOwner.OfWaystone ? ((WithOwner.OfWaystone)o).getWaystoneOwner() : Optional.empty()
		),
		Signpost(
			() -> Config.Server.permissions.defaultMaxSignpostsPerPlayer,
			(e, i) -> e.signpostsLeft = i,
			e -> e.signpostsLeft,
			LangKeys.noMoreSignposts, LangKeys.signpostsLeft, LangKeys.unlimitedSignposts,
			LangKeys.signpostsLeftOther, LangKeys.unlimitedSignpostsOther,
			o -> o instanceof WithOwner.OfSignpost ? ((WithOwner.OfSignpost)o).getSignpostOwner() : Optional.empty()
		);

		final Supplier<ForgeConfigSpec.ConfigValue<Integer>> getOverridePermissionLevel;
		final BiConsumer<Entry, Integer> setCount;
		final Function<Entry, Integer> getCount;
		public final String errorLangKey;
		public final String remainingLangKey;
		public final String unlimitedRemainingLangKey;
		public final String remainingLangKeyOther;
		public final String unlimitedRemainingLangKeyOther;
		public final Function<Object, Optional<PlayerHandle>> tryGetOwner;

		public TranslatableComponent getRemainingTextComponent(int count, Optional<Component> subject) {
			return subject.map(s -> new TranslatableComponent(remainingLangKeyOther, count, s))
				.orElseGet(() -> new TranslatableComponent(remainingLangKey, count));
		}

		public TranslatableComponent getUnlimitedRemainingTextComponent(Optional<Component> subject) {
			return subject.map(s -> new TranslatableComponent(unlimitedRemainingLangKeyOther, s))
				.orElseGet(() -> new TranslatableComponent(unlimitedRemainingLangKey));
		}

		Type(
			Supplier<ForgeConfigSpec.ConfigValue<Integer>> overridePermissionLevelSupplier,
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
			this.setCount = (e, i) -> {
				setCount.accept(e, i);
				BlockRestrictions.getInstance().markDirty();
			};
			this.getCount = getCount;
			this.errorLangKey = errorLangKey;
			this.remainingLangKey = remainingLangKey;
			this.unlimitedRemainingLangKey = unlimitedRemainingLangKey;
			this.remainingLangKeyOther = remainingLangKeyOther;
			this.unlimitedRemainingLangKeyOther = unlimitedRemainingLangKeyOther;
			this.tryGetOwner = tryGetOwner;
		}
	}

	private static BlockRestrictions instance;
	public static BlockRestrictions getInstance() { return instance; }

	private SavedData savedData = null;
	public boolean hasStorageBeenSetup() { return savedData != null; }

	public static void initialize() {
		instance = new BlockRestrictions();
	}

	private BlockRestrictions() {}

	private static class Entry {
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
					Config.Server.permissions.defaultMaxWaystonesPerPlayer.get(),
					Config.Server.permissions.defaultMaxSignpostsPerPlayer.get()
				);
		}
	}

	public void setupStorage(ServerLevel world){
		DimensionDataStorage storage = world.getDataStorage();
		savedData = storage.computeIfAbsent(
			compound -> new BlockRestrictionsStorage().load(compound),
			BlockRestrictionsStorage::new,
			BlockRestrictionsStorage.NAME
		);
	}

	private final Map<PlayerHandle, Entry> values = new HashMap<>();

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
			markDirty();
			return true;
		} else return false;
	}

	public void incrementRemaining(Type type, PlayerHandle player) {
		if(player.equals(PlayerHandle.Invalid)) return;
		Entry entry = getEntry(player);
		int prevCount = type.getCount.apply(entry);
		if(prevCount >= 0) {
			type.setCount.accept(entry, prevCount + 1);
			PacketHandler.send(
				PacketDistributor.PLAYER.with(() -> (ServerPlayer) player.asEntity()),
				new NotifyCountChanged(type.remainingLangKey, prevCount + 1, type == Type.Waystone)
			);
		}
	}

	public boolean tryDecrementRemaining(Type type, PlayerHandle player) {
		if(player.equals(PlayerHandle.Invalid)) return true;
		Entry entry = getEntry(player);
		int prevCount = type.getCount.apply(entry);
		if(prevCount >= 1) {
			type.setCount.accept(entry, prevCount - 1);
			PacketHandler.send(
				PacketDistributor.PLAYER.with(() -> (ServerPlayer) player.asEntity()),
				new NotifyCountChanged(type.remainingLangKey, prevCount - 1, type == Type.Waystone)
			);
			return true;
		} else {
			if(prevCount == 0)
				player.asEntity().sendMessage(new TranslatableComponent(type.errorLangKey), Util.NIL_UUID);
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

	private void markDirty() {
		// savedData is null on dedicated clients
		if(savedData != null) savedData.setDirty();
	}

	public CompoundTag saveTo(CompoundTag compound) {
		ListTag list = new ListTag();
		list.addAll(values.entrySet().stream().map(e -> {
			CompoundTag elementComp = new CompoundTag();
			PlayerHandle.Serializer.write(e.getKey(), elementComp);
			elementComp.putInt("remaining_waystones", e.getValue().waystonesLeft);
			elementComp.putInt("remaining_signposts", e.getValue().signpostsLeft);
			return elementComp;
		}).collect(Collectors.toSet()));
		compound.put("blockRestrictions", list);
		return compound;
	}

	public void readFrom(CompoundTag compound) {
		Tag nbt = compound.get("blockRestrictions");
		if(nbt instanceof ListTag) {
			ListTag list = (ListTag) nbt;
			values.clear();
			values.putAll(list.stream().map(i -> {
				CompoundTag elementCompound = (CompoundTag) i;
				return Tuple.of(PlayerHandle.Serializer.read(elementCompound),
					new Entry(elementCompound.getInt("remaining_waystones"), elementCompound.getInt("remaining_signposts")));
			}).collect(Tuple.mapCollector()));
		}
	}

	public static class NotifyCountChanged extends ReflectionEvent<NotifyCountChanged> {

		public NotifyCountChanged() {
			super();
		}

		public NotifyCountChanged(String langKey, Integer count, boolean isWaystoneNotification) {
			super(null);
			this.langKey = langKey;
			this.count = count;
			IsWaystoneNotification = isWaystoneNotification;
		}

		@SerializedWith(serializer = StringSerializer.class)
		private String langKey;

		@SerializedWith(serializer = IntSerializer.class)
		private Integer count;

		@SerializedWith(serializer = BooleanSerializer.class)
		private Boolean IsWaystoneNotification;

		@Override
		public Class<NotifyCountChanged> getMessageClass() {
			return NotifyCountChanged.class;
		}

		@Override
		public void handle(NotifyCountChanged message, NetworkEvent.Context context) {
			if((message.IsWaystoneNotification
				? Config.Client.enableWaystoneLimitNotifications
				: Config.Client.enableSignpostLimitNotifications
			).get())
				ClientFrameworkAdapter.showStatusMessage(new TranslatableComponent(message.langKey, message.count), true);
		}
	}

}
