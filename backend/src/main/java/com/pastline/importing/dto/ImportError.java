package com.pastline.importing.dto;

/** index: позиція запису у файлі (з нуля), щоб користувач знайшов і виправив саме його. */
public record ImportError(int index, String message) {}
