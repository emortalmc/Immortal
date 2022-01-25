package dev.emortal.immortal.config

import kotlinx.serialization.Serializable

@Serializable
class GameListingConfig(
    var gameListings: HashMap<String, GameListing> = hashMapOf()
)