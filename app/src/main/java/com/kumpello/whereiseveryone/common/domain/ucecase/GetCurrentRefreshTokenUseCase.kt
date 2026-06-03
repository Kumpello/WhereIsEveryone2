package com.kumpello.whereiseveryone.common.domain.ucecase

import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication

class GetCurrentRefreshTokenUseCase(
    private val getKeyUseCase: GetKeyUseCase
) {
   fun execute() : String? {
       return getKeyUseCase.getValue(WhereIsEveryoneApplication.AUTH_REFRESH_TOKEN_KEY)
   }
}