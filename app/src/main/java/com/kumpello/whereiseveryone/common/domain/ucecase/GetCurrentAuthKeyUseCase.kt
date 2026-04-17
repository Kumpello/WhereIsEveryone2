package com.kumpello.whereiseveryone.common.domain.ucecase

import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication

class GetCurrentAuthKeyUseCase(
    private val getKeyUseCase: GetKeyUseCase,
    private val saveKeyUseCase: SaveKeyUseCase
) {
   fun execute() : String? {
       //TODO: Add check is Auth key is current, if not use refresh token and save new key
       return getKeyUseCase.getValue(WhereIsEveryoneApplication.AUTH_TOKEN_KEY)
   }
}