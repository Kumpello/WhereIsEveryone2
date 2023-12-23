package com.kumpello.whereiseveryone.common.domain.ucecase

class GetCurrentAuthKeyUseCase(
    private val getKeyUseCase: GetKeyUseCase,
    private val saveKeyUseCase: SaveKeyUseCase
) {
   fun execute() : String {
       //TODO: Add check is Auth key is current, if not use refresh token and save new key
       return getKeyUseCase.getAuthToken().toString()
   }
}