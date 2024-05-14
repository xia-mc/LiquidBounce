/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.container

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.client.asNbt
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.world
import net.minecraft.inventory.SimpleInventory
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.registry.DynamicRegistryManager
import net.minecraft.text.Text
import net.minecraft.util.Formatting

/**
 * Generates a list of [NbtCompound]s from a [SimpleInventory]
 *
 * These [NbtCompound]s can be used to recreate the inventory
 * using Minecraft Chests
 * by placing them together
 *
 * For example by typing this command:
 * give @p chest{display:{Name:"\"Hello!\""},BlockEntityTag:{Items:[{Slot:0,id:acacia_boat,Count:1}]}} 1
 *
 * This will give you a chest with the name "Hello!" and an acacia boat in the first slot
 * However, this will only work for the first 27 slots and not for the rest. This is why we split the
 *  inventory into multiple [NbtCompound]s
 *
 * There should also be [CommandChest] .chest <nbt> which will give you the chest with the given nbt
 *
 * @return A list of [NbtCompound]s representing the inventory
 */
fun SimpleInventory.inventoryAsCompound(title: Text): List<NbtCompound> {
    val compoundList = mutableListOf<NbtCompound>()

    val stacks = getHeldStacks()

    // If the inventory is empty, we don't need to do anything
    if (stacks.isEmpty()) {
        return compoundList
    }

    stacks.chunked(27).forEachIndexed { containerIndex, stacks ->
        val chestCompound = NbtCompound()
        val displayCompound = NbtCompound()

        displayCompound.putString("Name", Text.Serialization.toJsonString(title, DynamicRegistryManager.EMPTY))

        val loreList = NbtList()
        loreList.add("Container #$containerIndex".asText().styled { it.withColor(Formatting.GOLD) }.asNbt(world))
        mc.currentServerEntry?.let {
            loreList.add("Server: ${it.address}".asText().styled { it.withColor(Formatting.GOLD) }.asNbt(world))
        }
        loreList.add("".asText().asNbt(world))

        loreList.add("Generated by ${LiquidBounce.CLIENT_NAME} ${LiquidBounce.clientVersion}".asText().styled {
            it.withColor(Formatting.DARK_AQUA)
        }.asNbt(world))
        loreList.add("Made with <3 by ${LiquidBounce.CLIENT_AUTHOR}".asText().styled {
            it.withColor(Formatting.YELLOW)
        }.asNbt(world))
        displayCompound.put("Lore", loreList)

        chestCompound.put("display", displayCompound)

        val blockEntityTag = NbtCompound()

        val itemList = NbtList()

        stacks.forEachIndexed { index, itemStack ->
            val itemCompound = itemStack.encode(DynamicRegistryManager.EMPTY) as NbtCompound

            itemCompound.putByte("Slot", index.toByte())
            itemList.add(itemCompound)
        }

        blockEntityTag.put("Items", itemList)
        chestCompound.put("BlockEntityTag", blockEntityTag)


        compoundList.add(chestCompound)
    }

    return compoundList
}
