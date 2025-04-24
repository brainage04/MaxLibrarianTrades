package io.github.brainage04;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.api.ModInitializer;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.VillagerProfession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MaxLibrarianTrades implements ModInitializer {
	public static final String MOD_ID = "maxlibrariantrades";
	public static final String MOD_NAME = "MaxLibrarianTrades";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static TradeOffers.Factory createMaxEnchWrapper(TradeOffers.Factory original) {
		return (entity, random) -> {
			TradeOffer offer = original.create(entity, random);
			if (offer == null) return null;

			ItemStack result = offer.getSellItem().copy();
			if (result.getItem() == Items.ENCHANTED_BOOK) {
				// get stored enchantments
				ItemEnchantmentsComponent before = result.getComponents().get(DataComponentTypes.STORED_ENCHANTMENTS);
				if (before == null) return null;

				// create hashmap for enchantment registry entries and levels
				Object2IntOpenHashMap<RegistryEntry<Enchantment>> enchantments = new Object2IntOpenHashMap<>();
				for (RegistryEntry<Enchantment> enchantmentRegistryEntry : before.getEnchantments()) {
					enchantments.put(enchantmentRegistryEntry, enchantmentRegistryEntry.value().getMaxLevel());
				}

				// create new item enchantments component with populated hashmap and override previous one
				ItemEnchantmentsComponent after = new ItemEnchantmentsComponent(enchantments);
				result.components.set(DataComponentTypes.STORED_ENCHANTMENTS, after);

				offer = new TradeOffer(
						offer.getFirstBuyItem(),
						offer.getSecondBuyItem(),
						result,
						offer.getUses(),
						offer.getMaxUses(),
						offer.getMerchantExperience(),
						offer.getPriceMultiplier()
				);
			}

			return offer;
		};
	}

	private static void overrideVillagerTradeOffers() {
		Map<RegistryKey<VillagerProfession>, Int2ObjectMap<TradeOffers.Factory[]>> tradesMap =
				TradeOffers.PROFESSION_TO_LEVELED_TRADE;

		Int2ObjectMap<TradeOffers.Factory[]> librarianLevels = tradesMap.get(VillagerProfession.LIBRARIAN);
		if (librarianLevels == null) return;

		for (int level : librarianLevels.keySet()) {
			TradeOffers.Factory[] originalFactories = librarianLevels.get(level);
			TradeOffers.Factory[] wrappedFactories = new TradeOffers.Factory[originalFactories.length];

			for (int i = 0; i < originalFactories.length; i++) {
				TradeOffers.Factory original = originalFactories[i];
				wrappedFactories[i] = createMaxEnchWrapper(original);
			}

			librarianLevels.put(level, wrappedFactories);
		}
	}
	
	@Override
	public void onInitialize() {
		LOGGER.info("{} initializing...", MOD_NAME);

		overrideVillagerTradeOffers();

		LOGGER.info("{} initialized.", MOD_NAME);
	}
}