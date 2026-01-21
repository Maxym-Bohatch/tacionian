package com.maxim.tacionian.blocks.charger;

public class TachyonSafeChargerBlock extends TachyonChargerBlock {
    public TachyonSafeChargerBlock(Properties props) {
        super(props, true); // Передаємо true в конструктор батька
    }
}