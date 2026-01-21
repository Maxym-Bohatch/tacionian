package com.maxim.tacionian.client.hud;

import com.maxim.tacionian.energy.ClientPlayerEnergy;
import net.minecraft.client.Minecraft;

public class EnergyColorHelper {
    public static int getColor() {
        long time = Minecraft.getInstance().level.getGameTime();
        float ratio = ClientPlayerEnergy.getRatio();

        // 1. Смертельна небезпека (>150%)
        if (ratio > 1.5f) {
            return (time % 4 < 2) ? 0xFFFF0000 : 0xFFFFA500;
        }

        // 2. Глушилка
        if (ClientPlayerEnergy.isJammed()) {
            return (time % 10 < 5) ? 0xFFFFFF00 : 0xFF333300;
        }

        // 3. Перевантаження та Критичний рівень
        if (ClientPlayerEnergy.isOverloaded()) return 0xFFFFA500;
        if (ClientPlayerEnergy.isCriticalLow()) return 0xFFFF0000;

        // 4. Активна передача енергії
        if (ClientPlayerEnergy.isRemoteNoDrain()) {
            return (time % 6 < 3) ? 0xFFFFFFFF : 0xFF00E5FF;
        }

        // 5. Стабілізація (Зелений)
        if (ClientPlayerEnergy.isStabilized()) return 0xFF00FF44;

        // 6. Зона інтерфейсу (Фіолетовий)
        if (ClientPlayerEnergy.isRemoteStabilized()) return 0xFFA020F0;

        // 7. Стандарт (Блакитний)
        return 0xFF00FFFF;
    }
}