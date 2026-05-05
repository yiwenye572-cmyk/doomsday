package com.doomsday.game.media;

public record GalleryImageItem(
        String imageUrl,
        String provider,
        String author,
        String license
) {}
