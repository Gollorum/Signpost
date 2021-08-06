package gollorum.signpost;

import com.ibm.icu.impl.Pair;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.storage.BlockRestrictionsStorage;
import gollorum.signpost.minecraft.utils.ClientFrameworkAdapter;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.networking.ReflectionEvent;
import gollorum.signpost.networking.SerializedWith;
import gollorum.signpost.security.WithOwner;
import gollorum.signpost.utils.serialization.BooleanSerializer;
import gollorum.signpost.utils.serialization.IntSerializer;
import gollorum.signpost.utils.serialization.StringSerializer;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

		public TranslationTextComponent getRemainingTextComponent(int count, Optional<ITextComponent> subject) {
			return subject.map(s -> new TranslationTextComponent(remainingLangKeyOther, count, s))
				.orElseGet(() -> new TranslationTextComponent(remainingLangKey, count));
		}

		public TranslationTextComponent getUnlimitedRemainingTextComponent(Optional<ITextComponent> subject) {
			return subject.map(s -> new TranslationTextComponent(unlimitedRemainingLangKeyOther, s))
				.orElseGet(() -> new TranslationTextComponent(unlimitedRemainingLangKey));
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

	private WorldSavedData savedData = null;
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

	public void setupStorage(ServerWorld world){
		DimensionSavedDataManager storage = world.getSavedData();
		savedData = storage.getOrCreate(BlockRestrictionsStorage::new, BlockRestrictionsStorage.NAME);
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
				PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player.asEntity()),
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
				PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player.asEntity()),
				new NotifyCountChanged(type.remainingLangKey, prevCount - 1, type == Type.Waystone)
			);
			return true;
		} else {
			if(prevCount == 0)
				player.asEntity().sendMessage(new TranslationTextComponent(type.errorLangKey), Util.DUMMY_UUID);
			return prevCount < 0;
		}
	}

	public boolean hasRemaining(Type type, PlayerHandle player) {
		if(player.equals(PlayerHandle.Invalid)) return true;
		Entry entry = getEntry(player);
		int remaining;
		switch (type) {
			case Waystone: remaining = entry.waystonesLeft;
				break;
			case Signpost: remaining = entry.signpostsLeft;
				break;
			default: throw new IllegalArgumentException();
		}
		return remaining != 0;
	}

	public int getRemaining(Type type, PlayerHandle player) {
		if(player.equals(PlayerHandle.Invalid)) return -1;
		Entry entry = getEntry(player);
		int remaining;
		switch (type) {
			case Waystone: remaining = entry.waystonesLeft;
				break;
			case Signpost: remaining = entry.signpostsLeft;
				break;
			default: throw new IllegalArgumentException();
		}
		return remaining;
	}

	private void markDirty() { savedData.markDirty(); }

	public CompoundNBT saveTo(CompoundNBT compound) {
		ListNBT list = new ListNBT();
		list.addAll(values.entrySet().stream().map(e -> {
			CompoundNBT elementComp = new CompoundNBT();
			PlayerHandle.Serializer.write(e.getKey(), elementComp);
			elementComp.putInt("remaining_waystones", e.getValue().waystonesLeft);
			elementComp.putInt("remaining_signposts", e.getValue().signpostsLeft);
			return elementComp;
		}).collect(Collectors.toSet()));
		compound.put("blockRestrictions", list);
		return compound;
	}

	public void readFrom(CompoundNBT compound) {
		INBT nbt = compound.get("blockRestrictions");
		if(nbt instanceof ListNBT) {
			ListNBT list = (ListNBT) nbt;
			values.clear();
			values.putAll(list.stream().map(i -> {
				CompoundNBT elementCompound = (CompoundNBT) i;
				return Pair.of(PlayerHandle.Serializer.read(elementCompound),
					new Entry(elementCompound.getInt("remaining_waystones"), elementCompound.getInt("remaining_signposts")));
			}).collect(Collectors.toMap(p -> p.first, p -> p.second)));
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
		protected void handle(NotifyCountChanged message, NetworkEvent.Context context) {
			if((message.IsWaystoneNotification
				? Config.Client.enableWaystoneLimitNotifications
				: Config.Client.enableSignpostLimitNotifications
			).get())
				ClientFrameworkAdapter.showStatusMessage(new TranslationTextComponent(message.langKey, message.count), true);
		}
	}

}
